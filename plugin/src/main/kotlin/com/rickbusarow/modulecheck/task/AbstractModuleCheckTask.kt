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

package com.rickbusarow.modulecheck.task

import com.rickbusarow.modulecheck.*
import com.rickbusarow.modulecheck.internal.Output
import com.rickbusarow.modulecheck.kapt.UnusedKaptRule
import com.rickbusarow.modulecheck.kapt.kaptMatchers
import com.rickbusarow.modulecheck.overshot.OvershotRule
import com.rickbusarow.modulecheck.parser.DslBlockParser
import com.rickbusarow.modulecheck.parser.PsiElementWithSurroundingText
import com.rickbusarow.modulecheck.rule.RedundantRule
import com.rickbusarow.modulecheck.rule.UnusedRule
import com.rickbusarow.modulecheck.rule.android.DisableAndroidResourcesRule
import com.rickbusarow.modulecheck.rule.android.DisableViewBindingRule
import com.rickbusarow.modulecheck.sort.SortDependenciesRule
import com.rickbusarow.modulecheck.sort.SortPluginsRule
import kotlinx.coroutines.runBlocking
import org.gradle.api.DefaultTask
import org.gradle.api.Project
import org.gradle.api.provider.Property
import org.gradle.api.provider.SetProperty
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.TaskAction
import org.gradle.kotlin.dsl.getByType
import java.util.*
import kotlin.system.measureTimeMillis

abstract class AbstractModuleCheckTask : DefaultTask() {

  init {
    group = "moduleCheck"
  }

  @get:Input
  val extension: ModuleCheckExtension = project.extensions.getByType()

  @get:Input
  val autoCorrect: Property<Boolean> = extension.autoCorrect

  @get:Input
  val alwaysIgnore: SetProperty<String> = extension.alwaysIgnore

  @get:Input
  val ignoreAll: SetProperty<String> = extension.ignoreAll

  val comparables: Array<(PsiElementWithSurroundingText) -> Comparable<*>> =
    SortPluginsRule.patterns
      .map { it.toRegex() }
      .map { regex ->
        { str: String -> !str.matches(regex) }
      }
      .map { booleanLambda ->
        { psi: PsiElementWithSurroundingText ->

          booleanLambda.invoke(psi.psiElement.text)
        }
      }.toTypedArray()

  @Suppress("SpreadOperator")
  val pluginComparator: Comparator<PsiElementWithSurroundingText> = compareBy(*comparables)

  val dependencyComparator: Comparator<PsiElementWithSurroundingText> =
    compareBy { psiElementWithSurroundings ->
      psiElementWithSurroundings
        .psiElement
        .text
        .toLowerCase(Locale.US)
    }

  @TaskAction
  fun evaluate() = runBlocking {
    val alwaysIgnore = alwaysIgnore.get()
    val ignoreAll = ignoreAll.get()

    val checks = extension.checks.get()

    val findings = buildList<Finding> {
      measured {
        project
          .allprojects
          .filter { it.buildFile.exists() }
          .sortedByDescending { it.mcp().getMainDepth() }
          .forEach { proj ->

            if (checks.overshot.get()) {
              addAll(
                OvershotRule(proj, alwaysIgnore, ignoreAll).check()
                  .distinctBy { it.dependencyProject.path }
              )
            }

            if (checks.redundant.get()) {
              addAll(
                RedundantRule(proj, alwaysIgnore, ignoreAll).check()
                  .distinctBy { it.dependencyProject.path }
              )
            }

            if (checks.unused.get()) {
              addAll(
                UnusedRule(proj, alwaysIgnore, ignoreAll).check()
                  .distinctBy { it.dependencyProject.path }
              )
            }

            if (checks.used.get()) {
            }

            if (checks.sortDependencies.get()) {
              val parser = DslBlockParser("dependencies")

              addAll(
                SortDependenciesRule(
                  project = proj,
                  alwaysIgnore = alwaysIgnore,
                  ignoreAll = ignoreAll,
                  parser = parser,
                  comparator = dependencyComparator
                )
                  .check()
              )
            }

            if (checks.sortPlugins.get()) {
              val parser = DslBlockParser("plugins")

              addAll(
                SortPluginsRule(
                  project = proj,
                  alwaysIgnore = alwaysIgnore,
                  ignoreAll = ignoreAll,
                  parser = parser,
                  comparator = pluginComparator
                )
                  .check()
              )
            }

            if (checks.kapt.get()) {
              val additionalKaptMatchers = project.extensions
                .getByType<ModuleCheckExtension>()
                .additionalKaptMatchers

              addAll(
                UnusedKaptRule(
                  project = proj,
                  alwaysIgnore = alwaysIgnore,
                  ignoreAll = ignoreAll,
                  kaptMatchers = kaptMatchers + additionalKaptMatchers.get()
                ).check()
              )
            }

            if (checks.disableAndroidResources.get()) {
              addAll(DisableAndroidResourcesRule(proj, alwaysIgnore, ignoreAll).check())
            }

            if (checks.disableViewBinding.get()) {
              addAll(DisableViewBindingRule(proj, alwaysIgnore, ignoreAll).check())
            }
          }
      }
    }

    findings.finish()
  }

  protected fun List<Finding>.finish() {
    val grouped = this.groupBy { it.dependentProject }

    Output.printMagenta("ModuleCheck found ${this.size} issues:\n")

    grouped.forEach { (project, list) ->
      Output.printMagenta("\t${project.path}")

      list.forEach { finding ->

        Output.printYellow("\t\t${finding.logString()}")

        if (finding is Fixable && autoCorrect.get()) {
          finding.fix()
        }
      }
    }
  }

  protected fun Project.moduleCheckProjects() =
    project.rootProject.allprojects
      .filter { gradleProject -> gradleProject.buildFile.exists() }
      .map { gradleProject -> MCP.from(gradleProject) }

  protected inline fun <T, R> T.measured(action: T.() -> R): R {
    var r: R

    val time = measureTimeMillis {
      r = action()
    }

    Output.printGreen("total parsing time --> $time milliseconds")

    return r
  }
}
