/*
 * Copyright (C) 2021-2023 Rick Busarow
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

package modulecheck.builds

import com.google.devtools.ksp.gradle.KspTaskJvm
import com.rickbusarow.kgx.EagerGradleApi
import com.rickbusarow.kgx.maybeNamed
import org.gradle.api.Project
import org.gradle.api.Task
import org.gradle.api.tasks.TaskProvider

fun Project.registerSimpleGenerationTaskAsDependency(
  sourceSetName: String,
  taskProvider: TaskProvider<out Task>
) {
  val kotlinTaskSourceSetName = when (sourceSetName) {
    "main" -> ""
    else -> sourceSetName.capitalize()
  }

  val ktlintSourceSetName = sourceSetName.capitalize()

  setOf(
    "compile${kotlinTaskSourceSetName}Kotlin",
    "sourcesJar",
    "javaSourcesJar",
    "lintKotlin$ktlintSourceSetName",
    "formatKotlin$ktlintSourceSetName",
    "runKtlintCheckOver${ktlintSourceSetName}SourceSet",
    "runKtlintFormatOver${ktlintSourceSetName}SourceSet"
  ).forEach { taskName ->

    @OptIn(EagerGradleApi::class)
    tasks.maybeNamed(taskName) { it.dependsOn(taskProvider) }
  }

  tasks.withType(KspTaskJvm::class.java).configureEach { it.dependsOn(taskProvider) }

  // generate the build properties file during an IDE sync, so no more red squigglies
  rootProject.tasks.named("prepareKotlinBuildScriptModel") {
    it.dependsOn(taskProvider)
  }
}
