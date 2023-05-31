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

import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.tasks.TaskContainer
import org.jmailen.gradle.kotlinter.KotlinterExtension
import org.jmailen.gradle.kotlinter.tasks.ConfigurableKtLintTask
import org.jmailen.gradle.kotlinter.tasks.FormatTask
import org.jmailen.gradle.kotlinter.tasks.LintTask
import kotlin.text.RegexOption.MULTILINE

abstract class KtLintConventionPlugin : Plugin<Project> {
  override fun apply(target: Project) {

    target.plugins.applyOnce("org.jmailen.kotlinter")

    target.extensions.configure(KotlinterExtension::class.java) { extension ->
      extension.ignoreFailures = false
      extension.reporters = arrayOf("checkstyle", "plain")
    }

    // dummy ktlint-gradle plugin task names which just delegate to the Kotlinter ones
    target.tasks.register("ktlintCheck") { it.dependsOn("lintKotlin") }
    target.tasks.register("ktlintFormat") {
      it.dependsOn("formatKotlin")
    }

    if (target.isRealRootProject()) {
      target.addRootProjectDelegateTasks()

      target.tasks.register("updateEditorConfigVersion") { task ->

        val file = target.file(".editorconfig")

        task.doLast {
          val oldText = file.readText()

          val reg = """^(ktlint_kt-rules_project_version *?= *?)\S*$""".toRegex(MULTILINE)

          val newText = oldText.replace(reg, "$1$VERSION_NAME")

          if (newText != oldText) {
            file.writeText(newText)
          }
        }
      }
    }

    target.tasks.withType(ConfigurableKtLintTask::class.java).configureEach { task ->
      task.source(target.buildFile)
      task.dependsOn(":updateEditorConfigVersion")
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

      target.tasks.withType(ConfigurableKtLintTask::class.java).configureEach { task ->
        excludeGenerated(task, target)

        if (task.name.contains("GeneratedByKsp")) {
          task.enabled = false
          // Just disabling the task is not enough to remove its dependencies from the task graph.
          // The output of the KSP tasks is part of this task's source, so its source must be
          // cleared.
          task.setSource(emptyList<String>())
          task.dependsOn.clear()
        }
      }
    }
  }

  // Add check/format tasks to each root project (including child root projects) which target every
  // `build.gradle.kts` and `settings.gradle.kts` file within that project group.
  private fun Project.addGradleScriptTasks(
    tasks: TaskContainer,
    dependencies: List<Any> = emptyList(),
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

  private fun Project.addRootProjectDelegateTasks() {

    // Add KtLint tasks to the root project to handle build-logic project sources as well.
    // The convention plugin can't be applied to build-logic in the conventional way since
    // that's where its source is.
    gradle
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

            val lintKotlinBuildLogic = tasks
              .register<LintTask>("lintKotlinBuildLogic$taskSuffix") { task ->
                task.group = "Formatting"
                task.description = "Runs lint on the source files in build-logic"
                task.source(files(buildLogicSrc))
                excludeGenerated(task, proj)
              }
            tasks.named("lintKotlin").dependsOn(lintKotlinBuildLogic)

            val formatKotlinBuildLogic = tasks
              .register<FormatTask>("formatKotlinBuildLogic$taskSuffix") { task ->
                task.group = "Formatting"
                task.description = "Formats the source files in build-logic"
                task.source(files(buildLogicSrc))
                excludeGenerated(task, proj)
              }
            tasks.named("formatKotlin").dependsOn(formatKotlinBuildLogic)
          }
      }

    gradle.includedBuild("build-logic")
      .rootProject()
      .addGradleScriptTasks(
        tasks,
        dependencies = emptyList(),
        taskNameQualifier = "BuildLogic"
      )
  }

  /**
   * These exclude anything in `$projectDir/build/generated/` from Kotlinter's
   * checks. Globs are relative to the **source set's** kotlin root.
   */
  private fun excludeGenerated(task: ConfigurableKtLintTask, project: Project) {
    // task.exclude("*Plugin.kt")
    // task.exclude("gradle/kotlin/dsl/**")
    // task.exclude("**/*_Proto.kt")
    // task.exclude("**/*JsonAdapter.kt")

    task.setSource(task.source - project.fileTree("${project.buildDir}/generated"))
  }
}
