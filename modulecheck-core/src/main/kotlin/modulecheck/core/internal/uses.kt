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

package modulecheck.core.internal

import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.filterIsInstance
import kotlinx.coroutines.flow.toSet
import modulecheck.api.context.anvilGraph
import modulecheck.api.context.anvilScopeContributionsForSourceSetName
import modulecheck.api.context.declarations
import modulecheck.api.context.dependents
import modulecheck.api.context.referencesForSourceSetName
import modulecheck.parsing.gradle.model.ConfiguredProjectDependency
import modulecheck.parsing.gradle.model.SourceSetName
import modulecheck.parsing.source.Generated
import modulecheck.project.McProject
import modulecheck.utils.any
import modulecheck.utils.containsAny
import modulecheck.utils.dataSource
import modulecheck.utils.lazySet

suspend fun McProject.uses(dependency: ConfiguredProjectDependency): Boolean {

  val dependencyDeclarations = dependency.declarations(projectCache)

  val referencesSourceSetName = dependency.configurationName.toSourceSetName()

  val refs = referencesForSourceSetName(referencesSourceSetName)

  // Check whether human-written code references the dependency first.
  val usedInStaticSource = refs
    .any { reference -> dependencyDeclarations.contains(reference) }

  if (usedInStaticSource) return true

  // Any generated code from the receiver project which requires a declaration from the dependency
  val generatedFromThisDependency = lazySet(
    dataSource {

      // TODO - probably make "all generated declarations" its own ProjectContext.Element,
      //  specifically targeting generated declarations.  It shouldn't be needed in this specific
      //  case, since `dependencyDeclarations` should already be fully cached by the time we get
      //  here, and we have to iterate over the flow anyway in order to filter again.
      declarations()
        .get(dependency.declaringSourceSetName(), includeUpstream = true)
        .filterIsInstance<Generated>()
        .filter { dependencyDeclarations.containsAny(it.sources) }
        .toSet()
    }
  )

  val usedUpstream = generatedFromThisDependency.isNotEmpty() && dependents()
    .any { downstreamDependency ->
      val downstreamSourceSet = downstreamDependency.configuredProjectDependency
        .declaringSourceSetName()

      projectCache.getValue(downstreamDependency.dependentProjectPath)
        .referencesForSourceSetName(downstreamSourceSet)
        .containsAny(generatedFromThisDependency)
    }

  if (usedUpstream) return true

  // If there are no references is manually/human written static code, then parse the Anvil graph.
  val anvilContributions = dependency.project()
    .anvilScopeContributionsForSourceSetName(SourceSetName.MAIN)

  return anvilGraph()
    .mergedScopeNames()
    .any { anvilContributions.containsKey(it) }
}
