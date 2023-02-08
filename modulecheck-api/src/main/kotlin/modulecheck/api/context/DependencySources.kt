/*
 * Copyright (C) 2021-2023 Rick Busarow
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package modulecheck.api.context

import modulecheck.api.context.DependencySources.SourceResult.Found
import modulecheck.api.context.DependencySources.SourceResult.NOT_PRESENT
import modulecheck.model.dependency.ProjectDependency
import modulecheck.model.dependency.ProjectPath
import modulecheck.model.sourceset.SourceSetName
import modulecheck.project.McProject
import modulecheck.project.ProjectContext
import modulecheck.utils.cache.SafeCache

data class DependencySources(
  private val delegate: SafeCache<SourceKey, SourceResult>,
  private val project: McProject
) : ProjectContext.Element {

  data class SourceKey(
    val sourceSetName: SourceSetName,
    val dependencyProjectPath: ProjectPath,
    val isTestFixture: Boolean
  )

  sealed interface SourceResult {
    data class Found(val sourceDependency: ProjectDependency) : SourceResult
    object NOT_PRESENT : SourceResult
  }

  override val key: ProjectContext.Key<DependencySources>
    get() = DependencySources

  suspend fun sourceOfOrNull(
    dependencyProjectPath: ProjectPath,
    sourceSetName: SourceSetName,
    isTestFixture: Boolean
  ): ProjectDependency? {

    val key = SourceKey(
      sourceSetName = sourceSetName,
      dependencyProjectPath = dependencyProjectPath,
      isTestFixture = isTestFixture
    )

    val fromCacheOrNull = delegate.getOrPut(key) {

      val sourceOrNull = project.classpathDependencies()
        .get(sourceSetName)
        .firstOrNull { transitive ->
          transitive.contributed.projectPath == dependencyProjectPath &&
            transitive.contributed.isTestFixture == isTestFixture
        }
        ?.source

      if (sourceOrNull != null) {
        Found(sourceOrNull)
      } else {
        NOT_PRESENT
      }
    }

    return when (fromCacheOrNull) {
      is Found -> fromCacheOrNull.sourceDependency
      NOT_PRESENT -> null
    }
  }

  companion object Key : ProjectContext.Key<DependencySources> {
    override suspend fun invoke(project: McProject): DependencySources {
      return DependencySources(
        SafeCache(listOf(project.projectPath, DependencySources::class)),
        project
      )
    }
  }
}

suspend fun ProjectContext.dependencySources(): DependencySources = get(DependencySources)

suspend fun McProject.requireSourceOf(
  dependencyProject: McProject,
  sourceSetName: SourceSetName,
  isTestFixture: Boolean
): ProjectDependency {
  return dependencySources().sourceOfOrNull(
    dependencyProjectPath = dependencyProject.projectPath,
    sourceSetName = sourceSetName,
    isTestFixture = isTestFixture
  )
    ?: throw IllegalArgumentException(
      "Unable to find source of the dependency project '${dependencyProject.projectPath}' " +
        "for SourceSet `${sourceSetName.value}` in the dependent project '$projectPath', " +
        "including transitive dependencies."
    )
}
