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

import modulecheck.api.context.Declarations
import modulecheck.api.context.androidDataBindingDeclarationsForSourceSetName
import modulecheck.api.context.androidResourceDeclarationsForSourceSetName
import modulecheck.api.context.anvilGraph
import modulecheck.api.context.anvilScopeContributionsForSourceSetName
import modulecheck.api.context.importsForSourceSetName
import modulecheck.api.context.referencesForSourceSetName
import modulecheck.parsing.gradle.SourceSetName
import modulecheck.parsing.source.AnvilScopeName
import modulecheck.parsing.source.DeclarationName
import modulecheck.project.AndroidMcProject
import modulecheck.project.ConfiguredProjectDependency
import modulecheck.project.McProject
import modulecheck.project.TransitiveProjectDependency

suspend fun McProject.uses(dependency: TransitiveProjectDependency): Boolean {

  val syntheticCpd = dependency.contributed
    .copy(
      configurationName = dependency.source.configurationName
    )

  return uses(syntheticCpd)
}

suspend fun McProject.uses(dependency: ConfiguredProjectDependency): Boolean {
  val mergedScopeNames = anvilGraph()
    .mergedScopeNames()

  val config = configurations[dependency.configurationName] ?: return false

  val all = config.inherited + config

  return all.any { usesInConfig(mergedScopeNames, dependency.copy(configurationName = it.name)) }
}

suspend fun McProject.usesInConfig(
  mergedScopeNames: List<AnvilScopeName>,
  dependency: ConfiguredProjectDependency
): Boolean {
  val contributions = dependency.project
    .anvilScopeContributionsForSourceSetName(SourceSetName.MAIN)

  val dependencyDeclarations = dependency.allDependencyDeclarations()

  val usedForAnvil = mergedScopeNames.any { contributions.containsKey(it) }

  val javaIsUsed = usedForAnvil || dependencyDeclarations
    .map { it.fqName }
    .any { declaration ->

      val imports = importsForSourceSetName(dependency.configurationName.toSourceSetName())
      val refs = referencesForSourceSetName(dependency.configurationName.toSourceSetName())

      val imported = declaration in imports
      val referenced = declaration in refs

      imported || referenced
    }

  if (javaIsUsed) return true

  if (this !is AndroidMcProject) return false

  val dependencyAsAndroid = dependency.project as? AndroidMcProject ?: return false

  val rReferences =
    referencesForSourceSetName(dependency.configurationName.toSourceSetName())
      .filter { it.startsWith("R.") }

  val dataBindingIsUsed = dependencyAsAndroid
    .androidDataBindingDeclarationsForSourceSetName(dependency.configurationName.toSourceSetName())
    .map { it.fqName }
    .any { declaration ->
      declaration in importsForSourceSetName(dependency.configurationName.toSourceSetName()) ||
        declaration in referencesForSourceSetName(dependency.configurationName.toSourceSetName())
    }

  if (dataBindingIsUsed) return true

  return dependencyAsAndroid
    .androidResourceDeclarationsForSourceSetName(dependency.configurationName.toSourceSetName())
    .map { it.fqName }
    .any { rDeclaration ->
      rDeclaration in rReferences
    }
}

suspend fun ConfiguredProjectDependency.allDependencyDeclarations(): Set<DeclarationName> {
  val root = project.get(Declarations).get(configurationName.toSourceSetName())

  val main = project.get(Declarations).get(SourceSetName.MAIN)

  val fixtures = if (isTestFixture) {
    project.get(Declarations).get(SourceSetName.TEST_FIXTURES)
  } else {
    emptySet()
  }

  val inherited = project.configurations[configurationName]
    ?.inherited
    ?.flatMap { inherited ->
      project.get(Declarations).get(inherited.name.toSourceSetName())
    }
    .orEmpty()

  return root + main + fixtures + inherited.toSet()
}
