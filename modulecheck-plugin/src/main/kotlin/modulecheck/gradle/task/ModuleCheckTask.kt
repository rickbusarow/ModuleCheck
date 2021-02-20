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

package modulecheck.gradle.task

import modulecheck.api.AndroidProject2
import modulecheck.api.Finding
import modulecheck.core.kapt.UnusedKaptRule
import modulecheck.core.mcp
import modulecheck.core.overshot.OvershotRule
import modulecheck.core.rule.*
import modulecheck.core.rule.android.DisableAndroidResourcesRule
import modulecheck.core.rule.android.DisableViewBindingRule
import modulecheck.core.rule.sort.SortDependenciesRule
import modulecheck.core.rule.sort.SortPluginsRule
import modulecheck.gradle.ModuleCheckExtension
import modulecheck.gradle.project2
import org.gradle.kotlin.dsl.getByType

abstract class ModuleCheckTask : AbstractModuleCheckTask() {

  @Suppress("LongMethod", "ComplexMethod")
  override fun getFindings(): List<Finding> {
    val settings = extension

    val checks = extension.checksSettings

    val findings = mutableListOf<Finding>()

    // use a mutable list and with(findings) { ... }
    // because buildList { ... } requires Kotlin 1.4.0, which means Gradle 6.8+
    with(findings) {
      measured {
        project
          .project2()
          .allprojects
          .filter { it.buildFile.exists() }
          .sortedByDescending { it.mcp().getMainDepth() }
          .forEach { proj ->

            if (checks.overshot) {
              addAll(
                OvershotRule(settings).check(proj)
                  .distinctBy { it.dependencyProject.path }
              )
            }

            if (checks.redundant) {
              addAll(
                RedundantRule(settings).check(proj)
                  .distinctBy { it.dependencyProject.path }
              )
            }

            if (checks.unused) {
              addAll(
                UnusedDependencyRule(settings).check(proj)
                  .distinctBy { it.dependencyProject.path }
              )
            }

            if (checks.mustBeApi) {
              addAll(
                MustBeApiRule(settings).check(proj)
                  .distinctBy { it.dependencyProject.path }
              )
            }

            if (checks.inheritedImplementation) {
              addAll(
                InheritedImplementationRule(settings).check(proj)
                  .distinctBy { it.dependencyProject.path }
              )
            }

            if (checks.sortDependencies) {
              addAll(
                SortDependenciesRule(settings)
                  .check(proj)
              )
            }

            if (checks.sortPlugins) {
              addAll(
                SortPluginsRule(settings)
                  .check(proj)
              )
            }

            if (checks.kapt) {
              val additionalKaptMatchers = project.extensions
                .getByType<ModuleCheckExtension>()
                .additionalKaptMatchers

              addAll(
                UnusedKaptRule(settings).check(proj)
              )
            }

            if (checks.anvilFactories) {
              addAll(AnvilFactoryRule(settings).check(proj))
            }

            if (checks.disableAndroidResources && proj is AndroidProject2) {
              addAll(DisableAndroidResourcesRule(settings).check(proj))
            }

            if (checks.disableViewBinding && proj is AndroidProject2) {
              addAll(DisableViewBindingRule(settings).check(proj))
            }
          }
      }
    }

    return findings
  }
}
