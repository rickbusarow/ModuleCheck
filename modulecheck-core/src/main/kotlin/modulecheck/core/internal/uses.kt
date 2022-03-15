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

import modulecheck.api.context.anvilGraph
import modulecheck.api.context.anvilScopeContributionsForSourceSetName
import modulecheck.api.context.declarations
import modulecheck.api.context.referencesForSourceSetName
import modulecheck.parsing.gradle.SourceSetName
import modulecheck.parsing.source.contains
import modulecheck.project.ConfiguredProjectDependency
import modulecheck.project.McProject
import modulecheck.utils.any

suspend fun McProject.uses(dependency: ConfiguredProjectDependency): Boolean {

  val dependencyDeclarations = dependency.declarations()

  val referencesSourceSetName = dependency.configurationName.toSourceSetName()

  val refs = referencesForSourceSetName(referencesSourceSetName)

  // Check whether human-written code references the dependency first.
  val usedInStaticSource = refs
    .any { reference -> dependencyDeclarations.contains(reference) }

  if (usedInStaticSource) return true

  // If there are no references is manually/human written static code, then parse the Anvil graph.
  val anvilContributions = dependency.project
    .anvilScopeContributionsForSourceSetName(SourceSetName.MAIN)

  return anvilGraph()
    .mergedScopeNames()
    .any { anvilContributions.containsKey(it) }
}
