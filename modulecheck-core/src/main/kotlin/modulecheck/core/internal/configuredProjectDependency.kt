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

import modulecheck.api.AndroidProject2
import modulecheck.api.ConfiguredProjectDependency
import modulecheck.api.Project2
import modulecheck.api.context.Declarations
import modulecheck.api.context.extraPossibleReferencesForSourceSetName
import modulecheck.api.context.importsForSourceSetName
import kotlin.LazyThreadSafetyMode.NONE

fun Project2.uses(dependency: ConfiguredProjectDependency): Boolean {
  return when (dependency.configurationName) {
    "androidTest" -> usesInAndroidTest(dependency)
    "api" -> usesInMain(dependency)
    "compileOnly" -> usesInMain(dependency)
    "implementation" -> usesInMain(dependency)
    "runtimeOnly" -> usesInMain(dependency)
    "testApi" -> usesInTest(dependency)
    "testImplementation" -> usesInTest(dependency)
    else -> false
  }
}

private fun Project2.usesInMain(dependency: ConfiguredProjectDependency): Boolean {
  val dependencyDeclarations = dependency
    .project [Declarations]["main"]
    .orEmpty()

  val javaIsUsed = dependencyDeclarations
    .any { declaration ->

      declaration in importsForSourceSetName("main") ||
        declaration in extraPossibleReferencesForSourceSetName("main")
    }

  if (javaIsUsed) return true

  if (this !is AndroidProject2) return false

  val rReferences = extraPossibleReferencesForSourceSetName("main")
    .filter { it.startsWith("R.") }

  val dependencyAsAndroid = dependency.project as? AndroidProject2 ?: return false

  val resourcesAreUsed = dependencyAsAndroid
    .androidResourceDeclarationsForSourceSetName("main")
    .any { rDeclaration ->
      rDeclaration in rReferences
    }

  return resourcesAreUsed
}

private fun Project2.usesInAndroidTest(dependency: ConfiguredProjectDependency): Boolean {
  val dependencyDeclarations = dependency
    .project [Declarations]["main"]
    .orEmpty()

  val rReferences by lazy(NONE) {
    extraPossibleReferencesForSourceSetName("androidTest")
      .filter { it.startsWith("R.") }
  }

  return dependencyDeclarations
    .any { declaration ->
      declaration in importsForSourceSetName("androidTest") ||
        declaration in extraPossibleReferencesForSourceSetName("androidTest")
    } || (dependency.project as? AndroidProject2)
    ?.androidResourceDeclarationsForSourceSetName("main")
    .orEmpty()
    .any { rDeclaration ->
      rDeclaration in rReferences
    }
}

private fun Project2.usesInTest(dependency: ConfiguredProjectDependency): Boolean {
  val dependencyDeclarations = dependency
    .project [Declarations]["main"]
    .orEmpty()

  val rReferences by lazy(NONE) {
    extraPossibleReferencesForSourceSetName("test")
      .filter { it.startsWith("R.") }
  }

  return dependencyDeclarations
    .any { declaration ->
      declaration in importsForSourceSetName("test") ||
        declaration in extraPossibleReferencesForSourceSetName("test")
    } || (dependency.project as? AndroidProject2)
    ?.androidResourceDeclarationsForSourceSetName("main")
    .orEmpty()
    .any { rDeclaration ->
      rDeclaration in rReferences
    }
}
