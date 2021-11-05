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

package modulecheck.core.internal

import modulecheck.api.context.*
import modulecheck.core.android.androidDataBindingDeclarationsForSourceSetName
import modulecheck.core.android.androidResourceDeclarationsForSourceSetName
import modulecheck.parsing.*

fun McProject.uses(dependency: ConfiguredProjectDependency): Boolean {
  val mergedScopeNames = anvilScopeMerges
    .values
    .flatMap { it.keys }

  val config = configurations[dependency.configurationName] ?: return false

  val all = config.inherited + config

  return all.any { usesInConfig(mergedScopeNames, dependency.copy(configurationName = it.name)) }
}

fun McProject.usesInConfig(
  mergedScopeNames: List<AnvilScopeName>,
  dependency: ConfiguredProjectDependency
): Boolean {
  val contributions = dependency.project
    .anvilScopeContributionsForSourceSetName(SourceSetName.MAIN)

  val dependencyDeclarations = dependency.allDependencyDeclarations()

  val javaIsUsed = mergedScopeNames.any { contributions.containsKey(it) } ||
    dependencyDeclarations
      .map { it.fqName }
      .any { declaration ->
        declaration in importsForSourceSetName(dependency.configurationName.toSourceSetName()) ||
          declaration in possibleReferencesForSourceSetName(dependency.configurationName.toSourceSetName())
      }

  if (javaIsUsed) return true

  if (this !is AndroidMcProject) return false

  val rReferences =
    possibleReferencesForSourceSetName(dependency.configurationName.toSourceSetName())
      .filter { it.startsWith("R.") }

  val dependencyAsAndroid = dependency.project as? AndroidMcProject ?: return false

  val dataBindingIsUsed = dependencyAsAndroid
    .androidDataBindingDeclarationsForSourceSetName(dependency.configurationName.toSourceSetName())
    .map { it.fqName }
    .any { declaration ->
      declaration in importsForSourceSetName(dependency.configurationName.toSourceSetName()) ||
        declaration in possibleReferencesForSourceSetName(dependency.configurationName.toSourceSetName())
    }

  if (dataBindingIsUsed) return true

  return dependencyAsAndroid
    .androidResourceDeclarationsForSourceSetName(dependency.configurationName.toSourceSetName())
    .map { it.fqName }
    .any { rDeclaration ->
      rDeclaration in rReferences
    }
}

fun ConfiguredProjectDependency.allDependencyDeclarations(): Set<DeclarationName> {
  val root = project[Declarations][configurationName.toSourceSetName()]
    .orEmpty()

  val main = project[Declarations][SourceSetName.MAIN]
    .orEmpty()

  val fixtures = if (isTestFixture) {
    project[Declarations][SourceSetName.TEST_FIXTURES].orEmpty()
  } else {
    emptySet()
  }

  val inherited = project.configurations[configurationName]
    ?.inherited
    ?.flatMap { inherited ->
      project[Declarations][inherited.name.toSourceSetName()]
        .orEmpty()
    }
    .orEmpty()

  return root + main + fixtures + inherited.toSet()
}
