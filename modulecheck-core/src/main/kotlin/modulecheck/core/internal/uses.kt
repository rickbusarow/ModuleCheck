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

import modulecheck.api.*
import modulecheck.api.context.DeclarationName
import modulecheck.api.context.Declarations
import modulecheck.api.context.importsForSourceSetName
import modulecheck.api.context.possibleReferencesForSourceSetName
import modulecheck.core.parser.android.androidResourceDeclarationsForSourceSetName

fun Project2.uses(dependency: ConfiguredProjectDependency): Boolean {
  val config = configurations[dependency.configurationName] ?: return false

  val all = config.inherited + config

  val depProject = dependency.project

  return all.any { usesInConfig(it, depProject) }
}

fun Project2.usesInConfig(config: Config, depProject: Project2): Boolean {
  val dependencyDeclarations = depProject.allDependencyDeclarationsForConfig(config)

  val javaIsUsed = dependencyDeclarations
    .any { declaration ->

      declaration in importsForSourceSetName(config.name.toSourceSetName()) ||
        declaration in possibleReferencesForSourceSetName(config.name.toSourceSetName())
    }

  if (javaIsUsed) return true

  if (this !is AndroidProject2) return false

  val rReferences =
    possibleReferencesForSourceSetName(config.name.toSourceSetName())
      .filter { it.startsWith("R.") }

  val dependencyAsAndroid = depProject as? AndroidProject2 ?: return false

  val resourcesAreUsed = dependencyAsAndroid
    .androidResourceDeclarationsForSourceSetName(config.name.toSourceSetName())
    .any { rDeclaration ->
      rDeclaration in rReferences
    }

  return resourcesAreUsed
}

fun Project2.allDependencyDeclarationsForConfig(config: Config): Set<DeclarationName> {
  val root = get(Declarations)[config.name.toSourceSetName()]
    .orEmpty()

  val main = get(Declarations)[SourceSetName.MAIN]
    .orEmpty()

  val inherited = config.inherited.flatMap { inherited ->
    get(Declarations)[inherited.name.toSourceSetName()]
      .orEmpty()
  }

  return root + main + inherited.toSet()
}
