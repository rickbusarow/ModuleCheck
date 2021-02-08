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
import com.rickbusarow.modulecheck.ModuleCheckExtension
import com.rickbusarow.modulecheck.kapt.UnusedKaptRule
import com.rickbusarow.modulecheck.kapt.kaptMatchers
import com.rickbusarow.modulecheck.mcp
import com.rickbusarow.modulecheck.overshot.OvershotRule
import com.rickbusarow.modulecheck.parser.DslBlockParser
import com.rickbusarow.modulecheck.rule.RedundantRule
import com.rickbusarow.modulecheck.rule.UnusedRule
import com.rickbusarow.modulecheck.rule.android.DisableAndroidResourcesRule
import com.rickbusarow.modulecheck.rule.android.DisableViewBindingRule
import com.rickbusarow.modulecheck.sort.SortDependenciesRule
import com.rickbusarow.modulecheck.sort.SortPluginsRule
import org.gradle.kotlin.dsl.getByType

abstract class ModuleCheckTask : AbstractModuleCheckTask() {

  @Suppress("LongMethod")
  override fun getFindings(): List<Finding> {
    val alwaysIgnore = alwaysIgnore.get()
    val ignoreAll = ignoreAll.get()

    val checks = extension.checks.get()

    val findings = mutableListOf<Finding>()

    // use a mutable list and with(findings) { ... }
    // because buildList { ... } requires Kotlin 1.4.0, which means Gradle 6.8+
    with(findings) {
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

    return findings
  }
}
