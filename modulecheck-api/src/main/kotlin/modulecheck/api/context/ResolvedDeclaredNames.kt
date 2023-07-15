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

import modulecheck.api.context.ResolvedDeclaredNames.SourceResult.Found
import modulecheck.api.context.ResolvedDeclaredNames.SourceResult.NOT_PRESENT
import modulecheck.model.sourceset.SourceSetName
import modulecheck.parsing.source.QualifiedDeclaredName
import modulecheck.project.McProject
import modulecheck.project.ProjectContext
import modulecheck.project.project
import modulecheck.utils.cache.SafeCache

/**
 * Represents a collection of resolved declared names in a project.
 *
 * @property delegate A cache that stores the source
 *   results for different declarations in source sets.
 * @property project The project for which the resolved declared names are being fetched.
 */
data class ResolvedDeclaredNames internal constructor(
  private val delegate: SafeCache<DeclarationInSourceSet, SourceResult>,
  private val project: McProject
) : ProjectContext.Element {

  override val key: ProjectContext.Key<ResolvedDeclaredNames>
    get() = Key

  /**
   * Represents a declaration in a source set.
   *
   * @property declaredName The qualified declared name.
   * @property sourceSetName The name of the source set in which the declaration is present.
   */
  internal data class DeclarationInSourceSet(
    val declaredName: QualifiedDeclaredName,
    val sourceSetName: SourceSetName
  )

  /** Represents the result of searching for a source of a declaration. */
  internal sealed interface SourceResult {
    data class Found(val sourceProject: McProjectWithSourceSetName) : SourceResult
    object NOT_PRESENT : SourceResult
  }

  /**
   * Represents a project with a source set name.
   *
   * @property project The project.
   * @property sourceSetName The name of the source set.
   */
  data class McProjectWithSourceSetName(
    val project: McProject,
    val sourceSetName: SourceSetName
  )

  /**
   * Fetches the source of a given declared name in a given source set.
   *
   * @param declaredName The qualified declared name.
   * @param sourceSetName The name of the source set.
   * @return The project with the source set name where
   *   the declared name is found, or `null` if not found.
   */
  suspend fun getSource(
    declaredName: QualifiedDeclaredName,
    sourceSetName: SourceSetName
  ): McProjectWithSourceSetName? {
    val declarationInSourceSet = DeclarationInSourceSet(declaredName, sourceSetName)

    val existing = delegate
      .getOrPut(declarationInSourceSet) { fetchNewSource(declaredName, sourceSetName) }

    return (existing as? Found)?.sourceProject
  }

  /**
   * Fetches the source of a given declared name in a given source set.
   *
   * @param declaredName The qualified declared name.
   * @param sourceSetName The name of the source set.
   * @return The result of searching for the source of the declared name.
   */
  private suspend fun fetchNewSource(
    declaredName: QualifiedDeclaredName,
    sourceSetName: SourceSetName
  ): SourceResult {
    return project.takeIf {
      project.declarations()
        .get(sourceSetName, includeUpstream = false)
        .contains(declaredName)
    }
      ?.let { Found(McProjectWithSourceSetName(it, sourceSetName)) }
      ?: project.classpathDependencies()
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
                .getSource(declaredName, dependencySourceSetName)
            }
        }
        ?.let { Found(it) }
      ?: NOT_PRESENT
  }

  companion object Key : ProjectContext.Key<ResolvedDeclaredNames> {
    override suspend operator fun invoke(project: McProject): ResolvedDeclaredNames {
      return ResolvedDeclaredNames(
        SafeCache(listOf(project.projectPath, ResolvedDeclaredNames::class)),
        project
      )
    }
  }
}

/**
 * Fetches the resolved declared names for a project.
 *
 * @return The `ResolvedDeclaredNames` for the project.
 */
suspend fun ProjectContext.resolvedDeclaredNames(): ResolvedDeclaredNames =
  get(ResolvedDeclaredNames)
