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

import kotlinx.coroutines.flow.firstOrNull
import modulecheck.api.context.ResolvedDeclaredNames.SourceResult.Found
import modulecheck.api.context.ResolvedDeclaredNames.SourceResult.NOT_PRESENT
import modulecheck.parsing.gradle.model.SourceSetName
import modulecheck.parsing.source.QualifiedDeclaredName
import modulecheck.parsing.source.ResolvableMcName
import modulecheck.project.McProject
import modulecheck.project.ProjectContext
import modulecheck.project.project
import modulecheck.utils.cache.SafeCache

class ResolvedDeclaredNames private constructor(
  private val delegate: SafeCache<NameInSourceSet, SourceResult>,
  private val project: McProject
) : ProjectContext.Element {

  override val key: ProjectContext.Key<ResolvedDeclaredNames>
    get() = Key

  internal sealed interface SourceResult {
    data class Found(val sourceProject: McProjectWithSourceSetName) : SourceResult
    object NOT_PRESENT : SourceResult
  }

  data class McProjectWithSourceSetName(
    val project: McProject,
    val sourceSetName: SourceSetName,
    val declaration: QualifiedDeclaredName
  )

  suspend fun getSource(
    name: ResolvableMcName,
    sourceSetName: SourceSetName
  ): McProjectWithSourceSetName? {
    val nameInSourceSet = NameInSourceSet(name, sourceSetName)

    val existing = delegate
      .getOrPut(nameInSourceSet) { fetchNewSource(name, sourceSetName) }

    return (existing as? Found)?.sourceProject
  }

  private suspend fun fetchNewSource(
    name: ResolvableMcName,
    sourceSetName: SourceSetName
  ): SourceResult {

    return sourceSetName.withUpstream(project)
      .firstNotNullOfOrNull { sourceSetOrUpstream ->

        val s: Set<Int> = setOf(1, 2, 3)

        project.declarations()
          .get(sourceSetOrUpstream, includeUpstream = false)
          .firstOrNull { it is QualifiedDeclaredName && it == name }
          ?.let {
            Found(
              McProjectWithSourceSetName(
                project = project,
                sourceSetName = sourceSetOrUpstream,
                declaration = it as QualifiedDeclaredName
              )
            )
          }
      } ?: project.classpathDependencies()
      .get(sourceSetName)
      .asSequence()
      .map { it.contributed }
      .distinctBy { it.project(project) to it.isTestFixture }
      .firstNotNullOfOrNull { sourceCpd ->

        listOfNotNull(
          SourceSetName.MAIN,
          SourceSetName.TEST_FIXTURES.takeIf { sourceCpd.isTestFixture }
        )
          .firstNotNullOfOrNull { dependencySourceSetName ->
            sourceCpd.project(project)
              .resolvedDeclaredNames()
              .getSource(name, dependencySourceSetName)
          }
      }
      ?.let { Found(it) }
      ?: NOT_PRESENT
  }

  companion object Key : ProjectContext.Key<ResolvedDeclaredNames> {
    override suspend operator fun invoke(project: McProject): ResolvedDeclaredNames {
      return ResolvedDeclaredNames(
        SafeCache(listOf(project.path, ResolvedDeclaredNames::class)),
        project
      )
    }
  }

  data class NameInSourceSet(
    val name: ResolvableMcName,
    val sourceSetName: SourceSetName
  )
}

suspend fun ProjectContext.resolvedDeclaredNames(): ResolvedDeclaredNames =
  get(ResolvedDeclaredNames)
