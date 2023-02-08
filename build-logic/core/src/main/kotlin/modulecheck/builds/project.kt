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
import org.gradle.api.Project
import org.gradle.api.Task
import org.gradle.api.plugins.PluginContainer
import org.gradle.api.tasks.TaskProvider

/**
 * Determines if this project is the root project **and** root of a composite build, if it's part of
 * a composite build.
 *
 * A composite build is a build using 'includeBuild(...)' in settings.gradle[.kts]. In composite
 * builds, the root of an included build is also a `rootProject` inside that included build. So
 * within that composite build, there are multiple projects for which `project == rootProject` would
 * return true.
 *
 * The Project property [gradle][org.gradle.api.Project.getGradle] refers to the specific
 * [gradle][org.gradle.api.invocation.Gradle] instance in that invocation of `./gradlew`, and the
 * only time [gradle.parent][org.gradle.api.invocation.Gradle.getParent] is null is when it's at the
 * true root of that tree.
 *
 * @return true if this project is the root of the entire build, else false
 */
fun Project.isRootOfComposite(): Boolean {
  return this == rootProject && gradle.parent == null
}

fun PluginContainer.applyOnce(id: String) {
  if (!hasPlugin(id)) {
    apply(id)
  }
}

fun Project.checkProjectIsRoot(
  message: () -> String = { "Only apply this plugin to the project root." }
) {
  check(this == rootProject, message)
}

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
    "javaSourcesJar",
    "lintKotlin$ktlintSourceSetName",
    "formatKotlin$ktlintSourceSetName",
    "runKtlintCheckOver${ktlintSourceSetName}SourceSet",
    "runKtlintFormatOver${ktlintSourceSetName}SourceSet"
  ).forEach { taskName ->
    tasks.maybeNamed(taskName) { dependsOn(taskProvider) }
  }

  tasks.withType(KspTaskJvm::class.java) { it.dependsOn(taskProvider) }

  // generate the build properties file during an IDE sync, so no more red squigglies
  rootProject.tasks.named("prepareKotlinBuildScriptModel") {
    it.dependsOn(taskProvider)
  }
}
