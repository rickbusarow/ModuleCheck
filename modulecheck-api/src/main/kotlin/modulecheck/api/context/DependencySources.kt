/*
 * Copyright (C) 2021-2022 Rick Busarow
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
import modulecheck.parsing.gradle.ProjectPath.StringProjectPath
import modulecheck.parsing.gradle.SourceSetName
import modulecheck.project.ConfiguredProjectDependency
import modulecheck.project.McProject
import modulecheck.project.ProjectContext
import modulecheck.utils.SafeCache

data class DependencySources(
  private val delegate: SafeCache<SourceKey, SourceResult>,
  private val project: McProject
) : ProjectContext.Element {

  data class SourceKey(
    val sourceSetName: SourceSetName,
    val dependencyProjectPath: StringProjectPath,
    val isTestFixture: Boolean
  )

  sealed interface SourceResult {
    data class Found(val sourceDependency: ConfiguredProjectDependency) : SourceResult
    object NOT_PRESENT : SourceResult
  }

  override val key: ProjectContext.Key<DependencySources>
    get() = DependencySources

  suspend fun sourceOfOrNull(
    dependencyProjectPath: StringProjectPath,
    sourceSetName: SourceSetName,
    isTestFixture: Boolean
  ): ConfiguredProjectDependency? {

    val key = SourceKey(
      sourceSetName = sourceSetName,
      dependencyProjectPath = dependencyProjectPath,
      isTestFixture = isTestFixture
    )

    val fromCacheOrNull = delegate.getOrPut(key) {

      val sourceOrNull = project.classpathDependencies()
        .get(sourceSetName)
        .firstOrNull { transitive ->
          transitive.contributed.project.path == dependencyProjectPath &&
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
      return DependencySources(SafeCache(), project)
    }
  }
}

suspend fun ProjectContext.dependencySources(): DependencySources = get(DependencySources)

suspend fun McProject.requireSourceOf(
  dependencyProject: McProject,
  sourceSetName: SourceSetName,
  isTestFixture: Boolean
): ConfiguredProjectDependency {
  return dependencySources().sourceOfOrNull(
    dependencyProjectPath = dependencyProject.path,
    sourceSetName = sourceSetName,
    isTestFixture = isTestFixture
  )
    ?: throw IllegalArgumentException(
      "Unable to find source of the dependency project '${dependencyProject.path}' for SourceSet " +
        "`${sourceSetName.value}` in the dependent project '$path', " +
        "including transitive dependencies."
    )
}
