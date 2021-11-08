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

import modulecheck.parsing.*

data class ClasspathDependencies(
  internal val delegate: Map<SourceSetName, Set<ConfiguredProjectDependency>>
) : Map<SourceSetName, Set<ConfiguredProjectDependency>> by delegate,
  ProjectContext.Element {

  override val key: ProjectContext.Key<ClasspathDependencies>
    get() = Key

  companion object Key : ProjectContext.Key<ClasspathDependencies> {
    override operator fun invoke(project: McProject): ClasspathDependencies {
      val map = project.sourceSets.keys
        .associateWith { project.fullTree(it).toSet() }

      return ClasspathDependencies(map)
    }

    private fun McProject.fullTree(
      sourceSetName: SourceSetName
    ): Sequence<ConfiguredProjectDependency> {

      val seed = sequenceOf(projectDependencies[sourceSetName]).flatten()

      val sourceApis = setOf(sourceSetName.apiConfig(), ConfigurationName.api)
      val sourceApiWithTestFixtures = setOf(
        sourceSetName.apiConfig(),
        ConfigurationName.api,
        SourceSetName.TEST_FIXTURES.apiConfig()
      )

      return generateSequence(seed) { cpds ->

        cpds.flatMap { (_, proj, isTestFixture) ->

          if (isTestFixture) {
            sourceApiWithTestFixtures
          } else {
            sourceApis
          }.flatMap { apiConfig ->

            proj.projectDependencies[apiConfig].orEmpty()
              .map { originalCpd ->
                val sourceCpd = requireSourceOf(
                  dependencyProject = originalCpd.project,
                  sourceSetName = sourceSetName,
                  isTestFixture = originalCpd.isTestFixture,
                  apiOnly = false
                )

                originalCpd.copy(configurationName = sourceCpd.configurationName)
              }
          }
        }
          .takeIf { it.firstOrNull() != null }
      }
        .flatten()
    }
  }
}

val ProjectContext.classpathDependencies: ClasspathDependencies get() = get(ClasspathDependencies)
