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
package modulecheck.builds.ktlint

import modulecheck.builds.ModuleCheckBuildTask
import modulecheck.builds.allProjects
import modulecheck.builds.applyOnce
import modulecheck.builds.capitalize
import modulecheck.builds.dependsOn
import modulecheck.builds.isRealRootProject
import modulecheck.builds.matchingName
import modulecheck.builds.register
import modulecheck.builds.rootProject
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.tasks.TaskContainer
import org.jmailen.gradle.kotlinter.KotlinterExtension
import org.jmailen.gradle.kotlinter.tasks.ConfigurableKtLintTask
import org.jmailen.gradle.kotlinter.tasks.FormatTask
import org.jmailen.gradle.kotlinter.tasks.LintTask

abstract class KtLintConventionPlugin : Plugin<Project> {
  override fun apply(target: Project) {

    target.plugins.applyOnce("org.jmailen.kotlinter")

    target.extensions.configure(KotlinterExtension::class.java) { extension ->
      extension.ignoreFailures = false
      extension.reporters = arrayOf("checkstyle", "plain")
      extension.experimentalRules = true
    }

    // dummy ktlint-gradle plugin task names which just delegate to the Kotlinter ones
    target.tasks.register("ktlintCheck") { it.dependsOn("lintKotlin") }
    target.tasks.register("ktlintFormat") {
      it.dependsOn("formatKotlin")
    }

    if (target.isRealRootProject()) {
      addRootProjectDelegateTasks(target)
    }

    target.tasks.named("lintKotlin") {
      it.mustRunAfter(target.tasks.named("formatKotlin"))
    }

    if (target == target.rootProject) {
      target.addGradleScriptTasks(target.tasks, taskNameQualifier = "")

      target.tasks.named("lintKotlin") { rootLint ->
        target.subprojects { sub ->
          rootLint.dependsOn(sub.tasks.matchingName("lintKotlin"))
        }
      }
      target.tasks.named("formatKotlin") { rootLint ->
        target.subprojects { sub ->
          rootLint.dependsOn(sub.tasks.matchingName("formatKotlin"))
        }
      }
    }

    target.afterEvaluate {

      target.tasks.withType(ConfigurableKtLintTask::class.java) { task ->
        excludeGenerated(task, target)
      }
    }
  }

  // Add check/format tasks to each root project (including child root projects) which target every
  // `build.gradle.kts` and `settings.gradle.kts` file within that project group.
  private fun Project.addGradleScriptTasks(
    tasks: TaskContainer,
    dependencies: List<Any> = listOf(),
    taskNameQualifier: String = ""
  ) {
    val includedProjectScriptFiles = allprojects
      .flatMap { included ->
        listOfNotNull(
          included.buildFile,
          included.file("settings.gradle.kts").takeIf { it.exists() }
        )
      }

    val lintKotlinBuildLogic = tasks
      .register<LintTask>("lintKotlin${taskNameQualifier}BuildScripts") { task ->
        task.group = "Formatting"
        task.description = "Runs lint on the build and settings files"
        task.source(includedProjectScriptFiles)
        task.dependsOn(dependencies)
      }
    tasks.named("lintKotlin").dependsOn(lintKotlinBuildLogic)

    val formatKotlinBuildLogic = tasks
      .register<FormatTask>("formatKotlin${taskNameQualifier}BuildScripts") { task ->
        task.group = "Formatting"
        task.description = "Formats the build and settings files"
        task.source(includedProjectScriptFiles)
        task.dependsOn(dependencies)
      }
    tasks.named("formatKotlin").dependsOn(formatKotlinBuildLogic)
  }

  private fun addRootProjectDelegateTasks(rootProject: Project) {

    val writeEditorConfig = rootProject.tasks
      .register<ModuleCheckBuildTask>("writeBuildLogicEditorConfig") { task ->
        val rootConfig = rootProject.file(".editorconfig")
        task.inputs.file(rootConfig)
        val buildLogicConfig = rootProject.file("build-logic/.editorconfig")
        task.outputs.file(buildLogicConfig)

        task.doLast {

          val originalRules = rootConfig.readLines()
            .single { it.startsWith("ktlint_disabled_rules =") }
            .substringAfter('=')
            .trim()
            .split(" ?, ?".toRegex())

          val additionalRules = listOf("no-since-in-kdoc")

          val newText = buildString {
            appendLine("### THIS FILE IS GENERATED.  DO NOT MODIFY.")
            appendLine("# These disabled rules are copied from the root project's .editorconfig.")
            appendLine("# Then additional rules are appended.")
            appendLine("# This is done by the 'writeBuildLogicEditorConfig' task.")
            appendLine("[{*.kt,*.kts}]")

            appendLine("# original rules:")
            originalRules.forEach { appendLine("#    $it") }
            appendLine("# additional disabled rules:")
            additionalRules.forEach { appendLine("#    $it") }

            appendLine("# noinspection EditorConfigKeyCorrectness")

            // creates `rule1,rule2,experimental:rule3,my-rule4`
            // Whitespaces around the '=' are fine, but there can't be any spaces before or after
            // the commas or Ktlint won't parse them and will just silently apply all rules.
            val allDisabledString = originalRules.plus(additionalRules).joinToString(",")
            appendLine("ktlint_disabled_rules = $allDisabledString")
            appendLine()
          }

          if (!buildLogicConfig.exists() || newText != buildLogicConfig.readText()) {
            println("writing a new version of file://$buildLogicConfig")
            buildLogicConfig.writeText(newText)
          }
        }
      }

    // Add KtLint tasks to the root project to handle build-logic project sources as well.
    // The convention plugin can't be applied to build-logic in the conventional way since
    // that's where its source is.
    rootProject.gradle
      .includedBuild("build-logic")
      .allProjects()
      .asSequence()
      .forEach { proj ->

        val cleanProjectName = proj.name.split("[^a-zA-Z]+".toRegex())
          .joinToString("") { it.capitalize() }

        proj.file("src")
          .listFiles()
          .orEmpty()
          .filter { it.isDirectory && it.exists() }
          .forEach { buildLogicSrc ->

            val taskSuffix = cleanProjectName + buildLogicSrc.nameWithoutExtension.capitalize()

            val lintKotlinBuildLogic = rootProject
              .tasks
              .register<LintTask>("lintKotlinBuildLogic$taskSuffix") { task ->
                task.group = "Formatting"
                task.description = "Runs lint on the source files in build-logic"
                task.source(rootProject.files(buildLogicSrc))
                excludeGenerated(task, proj)
                task.dependsOn(writeEditorConfig)
              }
            rootProject.tasks.named("lintKotlin").dependsOn(lintKotlinBuildLogic)

            val formatKotlinBuildLogic = rootProject
              .tasks
              .register<FormatTask>("formatKotlinBuildLogic$taskSuffix") { task ->
                task.group = "Formatting"
                task.description = "Formats the source files in build-logic"
                task.source(rootProject.files(buildLogicSrc))
                excludeGenerated(task, proj)
                task.dependsOn(writeEditorConfig)
              }
            rootProject.tasks.named("formatKotlin").dependsOn(formatKotlinBuildLogic)
          }
      }

    rootProject.gradle.includedBuild("build-logic")
      .rootProject()
      .addGradleScriptTasks(
        rootProject.tasks,
        dependencies = listOf(writeEditorConfig),
        taskNameQualifier = "BuildLogic"
      )
  }

  // These exclude the generated code from Kotlinter's checks.
  // These globs are relative to the source set's kotlin root.
  private fun excludeGenerated(task: ConfigurableKtLintTask, project: Project) {
    // task.exclude("*Plugin.kt")
    // task.exclude("gradle/kotlin/dsl/**")
    // task.exclude("**/*_Proto.kt")
    // task.exclude("**/*JsonAdapter.kt")

    task.setSource(task.source - project.fileTree("${project.buildDir}/generated"))
  }
}
