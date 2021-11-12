/*
 * Copyright (C) 2021 Rick Busarow
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

import modulecheck.api.context.ApiDependencySources.SourceKey
import modulecheck.api.context.ApiDependencySources.SourceResult
import modulecheck.api.context.ApiDependencySources.SourceResult.Found
import modulecheck.api.context.ApiDependencySources.SourceResult.NOT_PRESENT
import modulecheck.parsing.ConfiguredProjectDependency
import modulecheck.parsing.McProject
import modulecheck.parsing.ProjectContext
import modulecheck.parsing.SourceSetName
import java.util.concurrent.ConcurrentHashMap

data class ApiDependencySources(
  internal val delegate: ConcurrentHashMap<SourceKey, SourceResult>,
  private val project: McProject
) : Map<SourceKey, SourceResult> by delegate,
  ProjectContext.Element {

  data class SourceKey(
    val sourceSetName: SourceSetName,
    val dependencyProjectPath: String,
    val isTestFixture: Boolean
  )

  sealed interface SourceResult {
    data class Found(val sourceDependency: ConfiguredProjectDependency) : SourceResult
    object NOT_PRESENT : SourceResult
  }

  override val key: ProjectContext.Key<ApiDependencySources>
    get() = ApiDependencySources

  suspend fun sourceOfOrNull(
    dependencyProjectPath: String,
    sourceSetName: SourceSetName,
    isTestFixture: Boolean
  ): ConfiguredProjectDependency? {

    val fromCacheOrNull = delegate[
      SourceKey(
        sourceSetName = sourceSetName,
        dependencyProjectPath = dependencyProjectPath,
        isTestFixture = isTestFixture
      )
    ]
    if (fromCacheOrNull != null) {
      return when (fromCacheOrNull) {
        is Found -> fromCacheOrNull.sourceDependency
        NOT_PRESENT -> null
      }
    }

    val sourceOrNull = project.classpathDependencies()[sourceSetName]
      .firstOrNull { transitive ->
        transitive.contributed.project.path == dependencyProjectPath &&
          transitive.contributed.isTestFixture == isTestFixture
      }
      ?.source

    // cache the result for next time
    delegate.getOrPut(
      SourceKey(
        sourceSetName = sourceSetName,
        dependencyProjectPath = dependencyProjectPath,
        isTestFixture = isTestFixture
      )
    ) {
      if (sourceOrNull != null) {
        Found(sourceOrNull)
      } else {
        NOT_PRESENT
      }
    }

    return sourceOrNull
  }

  companion object Key : ProjectContext.Key<ApiDependencySources> {
    override suspend fun invoke(project: McProject): ApiDependencySources {
      return ApiDependencySources(ConcurrentHashMap(), project)
    }
  }
}

suspend fun ProjectContext.apiDependencySources(): ApiDependencySources = get(ApiDependencySources)

suspend fun McProject.requireSourceOf(
  dependencyProject: McProject,
  sourceSetName: SourceSetName,
  isTestFixture: Boolean
): ConfiguredProjectDependency {
  return apiDependencySources().sourceOfOrNull(
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
