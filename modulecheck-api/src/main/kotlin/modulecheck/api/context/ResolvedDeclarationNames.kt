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

import modulecheck.api.context.ResolvedDeclarationNames.SourceResult.Found
import modulecheck.api.context.ResolvedDeclarationNames.SourceResult.NOT_PRESENT
import modulecheck.parsing.gradle.SourceSetName
import modulecheck.parsing.source.DeclarationName
import modulecheck.project.McProject
import modulecheck.project.ProjectContext
import modulecheck.utils.SafeCache

data class ResolvedDeclarationNames internal constructor(
  private val delegate: SafeCache<DeclarationInSourceSet, SourceResult>,
  private val project: McProject
) : ProjectContext.Element {

  override val key: ProjectContext.Key<ResolvedDeclarationNames>
    get() = Key

  internal data class DeclarationInSourceSet(
    val declarationName: DeclarationName,
    val sourceSetName: SourceSetName
  )

  internal sealed interface SourceResult {
    data class Found(val sourceProject: McProjectWithSourceSetName) : SourceResult
    object NOT_PRESENT : SourceResult
  }

  data class McProjectWithSourceSetName(
    val project: McProject,
    val sourceSetName: SourceSetName
  )

  suspend fun getSource(
    declarationName: DeclarationName,
    sourceSetName: SourceSetName
  ): McProjectWithSourceSetName? {

    val declarationInSourceSet = DeclarationInSourceSet(declarationName, sourceSetName)

    val existing = delegate
      .getOrPut(declarationInSourceSet) { fetchNewSource(declarationName, sourceSetName) }

    return (existing as? Found)?.sourceProject
  }

  private suspend fun fetchNewSource(
    declarationName: DeclarationName,
    sourceSetName: SourceSetName
  ): SourceResult {
    return project.takeIf {

      project.declarations()
        .get(sourceSetName, includeUpstream = false)
        .contains(declarationName)
    }
      ?.let { Found(McProjectWithSourceSetName(it, sourceSetName)) }
      ?: project.classpathDependencies()
        .get(sourceSetName)
        .asSequence()
        .map { it.contributed }
        .distinctBy { it.project to it.isTestFixture }
        .firstNotNullOfOrNull { sourceCpd ->

          listOfNotNull(
            SourceSetName.MAIN,
            SourceSetName.TEST_FIXTURES.takeIf { sourceCpd.isTestFixture }
          )
            .firstNotNullOfOrNull { dependencySourceSetName ->
              sourceCpd.project.resolvedDeclarationNames()
                .getSource(declarationName, dependencySourceSetName)
            }
        }
        ?.let { Found(it) }
      ?: NOT_PRESENT
  }

  companion object Key : ProjectContext.Key<ResolvedDeclarationNames> {
    override suspend operator fun invoke(project: McProject): ResolvedDeclarationNames {

      return ResolvedDeclarationNames(SafeCache(), project)
    }
  }
}

suspend fun ProjectContext.resolvedDeclarationNames(): ResolvedDeclarationNames =
  get(ResolvedDeclarationNames)
