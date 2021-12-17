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

import modulecheck.parsing.gradle.SourceSetName
import modulecheck.project.McProject
import modulecheck.project.ProjectContext
import modulecheck.project.TransitiveProjectDependency
import modulecheck.utils.SafeCache

data class SourceSetDependencies(
  private val delegate: SafeCache<SourceSetName, List<TransitiveProjectDependency>>,
  private val project: McProject
) : ProjectContext.Element {

  override val key: ProjectContext.Key<SourceSetDependencies>
    get() = Key

  suspend fun get(sourceSetName: SourceSetName): List<TransitiveProjectDependency> {
    return delegate.getOrPut(sourceSetName) { project.fullTree(sourceSetName) }
  }

  private suspend fun McProject.fullTree(
    sourceSetName: SourceSetName
  ): List<TransitiveProjectDependency> {

    fun sourceConfigs(
      isTestFixtures: Boolean
    ): Set<SourceSetName> = setOfNotNull(
      SourceSetName.MAIN,
      SourceSetName.TEST_FIXTURES.takeIf { isTestFixtures }
    )

    val directDependencies = projectDependencies[sourceSetName]
      .filterNot { it.project == project }
      .toSet()

    val directDependencyPaths = directDependencies.map { it.project.path }.toSet()

    val inherited = directDependencies.flatMap { sourceCpd ->
      sourceConfigs(sourceCpd.isTestFixture)
        .flatMap { dependencySourceSetName ->

          sourceCpd.project
            .sourceSetDependencies()
            .get(dependencySourceSetName)
            .filterNot { it.contributed.project.path in directDependencyPaths }
            .map { transitiveCpd ->
              TransitiveProjectDependency(sourceCpd, transitiveCpd.contributed)
            }
        }
    }
      .toSet()

    val directTransitive = directDependencies.map { TransitiveProjectDependency(it, it) }

    val mainFromTestFixtures = directDependencies.filter { it.isTestFixture }
      .map { TransitiveProjectDependency(it, it.copy(isTestFixture = false)) }

    return directTransitive + inherited + mainFromTestFixtures
  }

  companion object Key : ProjectContext.Key<SourceSetDependencies> {
    override suspend operator fun invoke(project: McProject): SourceSetDependencies {
      return SourceSetDependencies(SafeCache(), project)
    }
  }
}

suspend fun ProjectContext.sourceSetDependencies(): SourceSetDependencies =
  get(SourceSetDependencies)
