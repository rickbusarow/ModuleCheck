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

import modulecheck.parsing.gradle.ConfigurationName
import modulecheck.parsing.gradle.SourceSetName
import modulecheck.project.McProject
import modulecheck.project.ProjectContext
import modulecheck.project.TransitiveProjectDependency
import modulecheck.utils.SafeCache

data class ClasspathDependencies(
  private val delegate: SafeCache<SourceSetName, List<TransitiveProjectDependency>>,
  private val project: McProject
) : ProjectContext.Element {

  override val key: ProjectContext.Key<ClasspathDependencies>
    get() = Key

  suspend fun all(): List<TransitiveProjectDependency> {
    return project.sourceSets.keys.flatMap { get(it) }
  }

  suspend fun get(key: SourceSetName): List<TransitiveProjectDependency> {
    return delegate.getOrPut(key) { project.fullTree(key) }
  }

  private suspend fun McProject.fullTree(
    sourceSetName: SourceSetName
  ): List<TransitiveProjectDependency> {

    fun sourceApiConfigs(
      sourceSetName: SourceSetName,
      isTestFixtures: Boolean
    ): Set<ConfigurationName> = setOfNotNull(
      sourceSetName.apiConfig(),
      ConfigurationName.api,
      SourceSetName.TEST_FIXTURES.apiConfig().takeIf { isTestFixtures }
    )

    val directDependencies = projectDependencies[sourceSetName]
      .filterNot { it.project == project }
      .toSet()

    val directDependencyPaths = directDependencies.map { it.project.path }.toSet()

    val inherited = directDependencies.flatMap { sourceCpd ->
      sourceApiConfigs(sourceSetName, sourceCpd.isTestFixture)
        .flatMap { apiConfig ->

          sourceCpd.project
            .classpathDependencies()
            .get(apiConfig.toSourceSetName())
            .asSequence()
            .filter { it.contributed.configurationName.isApi() }
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

  companion object Key : ProjectContext.Key<ClasspathDependencies> {
    override suspend operator fun invoke(project: McProject): ClasspathDependencies {
      return ClasspathDependencies(SafeCache(), project)
    }
  }
}

suspend fun ProjectContext.classpathDependencies(): ClasspathDependencies =
  get(ClasspathDependencies)
