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

import io.gitlab.arturbosch.detekt.Detekt
import io.gitlab.arturbosch.detekt.DetektCreateBaselineTask
import io.gitlab.arturbosch.detekt.DetektGenerateConfigTask
import io.gitlab.arturbosch.detekt.report.ReportMergeTask
import modulecheck.builds.ModuleCheckBuildCodeGeneratorTask
import modulecheck.builds.dependency
import modulecheck.builds.libsCatalog

plugins {
  id("io.gitlab.arturbosch.detekt")
}

val reportMerge by tasks.registering(ReportMergeTask::class) {
  output.set(rootProject.buildDir.resolve("reports/detekt/merged.sarif"))
}

val detektExcludes = listOf(
  "**/resources/**",
  "**/build/**"
)

dependencies {
  detektPlugins(libsCatalog.dependency("detekt-rules-libraries"))
}

detekt {

  autoCorrect = false
  baseline = file("$rootDir/detekt/detekt-baseline.xml")
  config = files("$rootDir/detekt/detekt-config.yml")
  buildUponDefaultConfig = true

  source = files(
    listOf("src/main/java", "src/test/java", "src/main/kotlin", "src/test/kotlin")
  )
  parallel = true
}

tasks.withType<Detekt> {

  autoCorrect = false
  parallel = true
  baseline.set(file("$rootDir/detekt/detekt-baseline.xml"))
  config.from(files("$rootDir/detekt/detekt-config.yml"))
  buildUponDefaultConfig = true

  // If in CI, merge sarif reports.  Skip this locally because it's not worth looking at
  // and the task is unnecessarily chatty.
  if (!System.getenv("CI").isNullOrBlank()) {
    finalizedBy(reportMerge)
    reportMerge.configure {
      input.from(sarifReportFile)
    }
  }

  reports {
    xml.required.set(true)
    html.required.set(true)
    txt.required.set(false)
    sarif.required.set(true)
  }

  exclude(detektExcludes)
  subprojects.forEach { sub ->
    exclude("**/${sub.projectDir.relativeTo(rootDir)}/**")
  }

  dependsOn(tasks.withType(ModuleCheckBuildCodeGeneratorTask::class.java))
}

fun Task.otherDetektTasks(withAutoCorrect: Boolean): TaskCollection<Detekt> {
  return tasks.withType(Detekt::class.java)
    .matching { it.autoCorrect == withAutoCorrect && it != this@otherDetektTasks }
}

tasks.register("detektAll", Detekt::class) {
  description = "runs the standard PSI Detekt as well as all type resolution tasks"
  dependsOn(otherDetektTasks(withAutoCorrect = false))
}

// Make all tasks from Detekt part of the 'detekt' task group.  Default is 'verification'.
sequenceOf(
  Detekt::class.java,
  DetektCreateBaselineTask::class.java,
  DetektGenerateConfigTask::class.java
).forEach { type ->
  tasks.withType(type).configureEach { group = "detekt" }
}

// By default, `check` only handles the PSI Detekt task.  This adds the type resolution tasks.
tasks.named(LifecycleBasePlugin.CHECK_TASK_NAME) {
  dependsOn(otherDetektTasks(withAutoCorrect = false))
}

if (project == rootProject) {

  tasks.register("detektProjectBaseline", DetektCreateBaselineTask::class) {
    description = "Overrides current baseline."
    buildUponDefaultConfig.set(true)
    ignoreFailures.set(true)
    parallel.set(true)
    setSource(files(rootDir))
    config.setFrom(files("$rootDir/detekt/detekt-config.yml"))

    val baselineFile = file("$rootDir/detekt/detekt-baseline.xml")
    baseline.set(baselineFile)

    include("**/*.kt", "**/*.kts")
    exclude(detektExcludes)

    doLast {
      // Detekt completely re-writes this file's contents any time it has to update.
      // After updating the baseline file, insert the comment to exclude it from auto-format.
      val oldText = baselineFile.readText()
      val newText = oldText.replaceFirst(
        "<?xml version='1.0' encoding='UTF-8'?>",
        "<?xml version='1.0' encoding='UTF-8'?>\n" +
          "<!--@formatter:off   this file (or Detekt's parsing?) is broken if this gets auto-formatted-->"
      )
      baselineFile.writeText(newText)
    }
  }
}
