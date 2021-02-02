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

import com.rickbusarow.modulecheck.Finding
import com.rickbusarow.modulecheck.Fixable
import com.rickbusarow.modulecheck.MCP
import com.rickbusarow.modulecheck.ModuleCheckExtension
import com.rickbusarow.modulecheck.internal.Output
import com.rickbusarow.modulecheck.parser.PsiElementWithSurroundingText
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
    val findings = getFindings()

    findings.finish()
  }

  protected abstract fun getFindings(): List<Finding>

  private fun List<Finding>.finish(): Boolean {
    val grouped = this.groupBy { it.dependentProject }

    Output.printMagenta("ModuleCheck found ${this.size} issues:\n")

    val unFixed = grouped
      .entries
      .sortedBy { it.key.path }
      .flatMap { (project, list) ->

        Output.printMagenta("\t${project.path}")

        val (fixed, toFix) = list.partition { finding ->
          autoCorrect.get() && (finding as? Fixable)?.fix() ?: false
        }

        fixed.forEach { finding ->
          Output.printYellow("\t\t${finding.logString()}")
        }

        toFix.forEach { finding ->
          Output.printRed("\t\t${finding.logString()}")
        }

        toFix
      }

    return unFixed.isEmpty()
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

    Output.printGreen("total parsing time: $time milliseconds")

    return r
  }
}
