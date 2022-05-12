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

package modulecheck.finding

import modulecheck.parsing.gradle.dsl.HasBuildFile
import modulecheck.project.ConfiguredProjectDependency
import modulecheck.project.Dependency
import modulecheck.project.HasProjectCache
import modulecheck.project.PluginDependency

class Suppressions(
  private val delegate: Map<Dependency, Set<FindingName>>
) {

  private val reverseMap by lazy {
    buildMap<FindingName, MutableSet<Dependency>> {
      delegate.forEach { (cpd, names) ->

        names.forEach { name ->
          val setWithNewCpd = getOrPut(name) { mutableSetOf() }
            .apply { add(cpd) }
          put(name, setWithNewCpd)
        }
      }
    }
  }

  fun get(dependency: Dependency): Set<FindingName> {
    return delegate[dependency].orEmpty()
  }

  fun get(findingName: FindingName): Set<Dependency> {
    return reverseMap[findingName].orEmpty()
  }
}

suspend fun <T> T.getSuppressions(): Suppressions
  where T : HasBuildFile,
        T : HasProjectCache {
  val fromDependencies = buildFileParser.dependenciesBlocks()
    .map { it.allSuppressions }
    .fold(mutableMapOf<Dependency, MutableSet<FindingName>>()) { acc, block ->

      block.forEach { (configuredModule, newNames) ->

        val dependencyProject = projectCache.getValue(configuredModule.projectPath)
        val cpd = ConfiguredProjectDependency(
          configuredModule.configName,
          dependencyProject,
          configuredModule.testFixtures
        )

        val cachedNames = acc.getOrPut(cpd) { mutableSetOf() }

        cachedNames.addAll(newNames)
      }
      acc
    }

  buildFileParser.pluginsBlock()
    ?.let { pluginsBlock ->

      pluginsBlock.allSuppressions.forEach { (pluginDeclaration, newNames) ->
        val dependency = PluginDependency(pluginDeclaration.declarationText)

        val cachedNames = fromDependencies.getOrPut(dependency) { mutableSetOf() }

        cachedNames.addAll(newNames)
      }
    }

  return Suppressions(fromDependencies)
}
