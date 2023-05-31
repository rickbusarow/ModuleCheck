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
import org.gradle.api.initialization.IncludedBuild
import org.gradle.api.internal.GradleInternal
import org.gradle.api.internal.project.ProjectInternal
import org.gradle.api.internal.project.ProjectRegistry
import org.gradle.api.invocation.Gradle
import org.gradle.api.plugins.JavaPluginExtension
import org.gradle.api.plugins.PluginContainer
import org.gradle.api.tasks.TaskCollection
import org.gradle.api.tasks.TaskProvider
import org.gradle.composite.internal.DefaultIncludedBuild
import org.gradle.composite.internal.DefaultIncludedBuild.IncludedBuildImpl

val Project.javaExtension: JavaPluginExtension
  get() = extensions.getByType(JavaPluginExtension::class.java)

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
    "sourcesJar",
    "javaSourcesJar",
    "lintKotlin$ktlintSourceSetName",
    "formatKotlin$ktlintSourceSetName",
    "runKtlintCheckOver${ktlintSourceSetName}SourceSet",
    "runKtlintFormatOver${ktlintSourceSetName}SourceSet"
  ).forEach { taskName ->
    tasks.maybeNamed(taskName) { dependsOn(taskProvider) }
  }

  tasks.withType(KspTaskJvm::class.java).configureEach { it.dependsOn(taskProvider) }

  // generate the build properties file during an IDE sync, so no more red squigglies
  rootProject.tasks.named("prepareKotlinBuildScriptModel") {
    it.dependsOn(taskProvider)
  }
}

/** @return the root project of this included build */
fun IncludedBuild.rootProject(): ProjectInternal {
  return requireProjectRegistry().rootProject!!
}

/** @return all projects in this included build */
fun IncludedBuild.allProjects(): Set<ProjectInternal> {
  return requireProjectRegistry().allProjects
}

/**
 * @return the projects in this included build, or throws
 *   if the [IncludedBuild] is of an unexpected type
 */
fun IncludedBuild.requireProjectRegistry(): ProjectRegistry<ProjectInternal> {
  require(this is IncludedBuildImpl) {
    "The receiver ${IncludedBuild::class.qualifiedName} is expected to be an " +
      "${IncludedBuildImpl::class.qualifiedName}, but instead it was ${this::class.qualifiedName}"
  }
  val delegate = this@requireProjectRegistry.target
  require(delegate is DefaultIncludedBuild) {
    "The 'target' property is expected to be a ${DefaultIncludedBuild::class.qualifiedName}, " +
      "but instead it was ${delegate::class.qualifiedName}"
  }

  delegate.ensureProjectsConfigured()

  return delegate.mutableModel.projectRegistry
}

/** @return all projects from all included builds */
fun Gradle.allIncludedProjects(): List<ProjectInternal> {
  return includedBuilds.flatMap { it.allProjects() }
}

/**
 * Look at the internal modules of an included build, find
 * any tasks with a matching name, and return them all.
 *
 * Note that this forces the included build to configure.
 */
fun Gradle.includedAllProjectsTasks(taskName: String): List<TaskCollection<Task>> {
  return allIncludedProjects().map { it.tasks.matchingName(taskName) }
}

/**
 * Look at the root project of an included build, find any task with a
 * matching name, and return it or null. This is an alternative to the standard
 * [IncludedBuild.task][org.gradle.api.initialization.IncludedBuild.task] function in
 * that the standard `task` version will throw an exception if the task is not registered.
 *
 * Note that this forces the included build to configure.
 */
fun Gradle.includedRootProjectsTasks(taskName: String): List<TaskCollection<Task>> {
  return includedBuilds.mapNotNull { included ->

    val includedImpl = included as IncludedBuildImpl

    val implState = includedImpl.target as DefaultIncludedBuild

    implState.ensureProjectsConfigured()

    implState.mutableModel.rootProject.tasks.matchingName(taskName)
  }
}

/**
 * Determines whether the receiver project is the "real" root of this
 * composite build, as opposed to the root projects of included builds.
 */
fun Project.isRealRootProject(): Boolean {
  return (gradle as GradleInternal).isRootBuild && this == rootProject
}

/** `rootProject == this` */
fun Project.isRootProject(): Boolean {
  return rootProject == this
}
