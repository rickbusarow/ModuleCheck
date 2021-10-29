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

package modulecheck.api.settings

import modulecheck.api.KaptMatcher

interface ModuleCheckSettings {

  /**
   * ModuleCheck will attempt to fix its findings automatically. This means removing unused
   * dependencies, changing the configuration to `api` if necessary, and adding inherited
   * dependencies which should be declared explicitly.
   *
   * By default, ModuleCheck will "remove" declarations of unused dependencies by simply commenting
   * them out. See the [deleteUnused] for the option to delete them entirely.
   */
  var autoCorrect: Boolean

  /**
   * If true, ModuleCheck will delete declarations of unused dependencies entirely.
   *
   * If false, ModuleCheck will comment out declarations of unused dependencies.
   *
   * Default value is false.
   */
  var deleteUnused: Boolean

  /**
   * Set of modules which are allowed to be unused.
   *
   * For instance, given:
   * ```
   * ignoreUnusedFinding = setOf(":core")
   * ```
   * If a module declares `:core` as a dependency but does not use it, no finding will be reported.
   */
  var ignoreUnusedFinding: Set<String>

  /**
   * Set of modules which will not be excluded from error reporting.
   * The most common use-case would be if the module is the root of a dependency graph,
   * like an Android application module, and it needs everything in its classpath
   * for dependency injection purposes.
   */
  var doNotCheck: Set<String>

  /**
   * List of [KaptMatcher]'s to be checked, which aren't included by default with ModuleCheck.
   */
  var additionalKaptMatchers: List<KaptMatcher>

  val checks: ChecksSettings

  val sort: SortSettings

  /**
   * Configures reporting options
   */
  val reports: ReportsSettings
}

interface SortSettings {
  var pluginComparators: List<String>
  var dependencyComparators: List<String>

  companion object {

    val PLUGIN_COMPARATORS_DEFAULT = listOf(
      """id\("com\.android.*"\)""",
      """id\("android-.*"\)""",
      """id\("java-library"\)""",
      """kotlin\("jvm"\)""",
      """android.*""",
      """javaLibrary.*""",
      """kotlin.*""",
      """id.*"""
    )
    val DEPENDENCY_COMPARATORS_DEFAULT = listOf(
      """.*""",
      """kapt.*"""
    )
  }
}

interface ChecksSettings {
  var redundantDependency: Boolean
  var unusedDependency: Boolean
  var overShotDependency: Boolean
  var mustBeApi: Boolean
  var inheritedDependency: Boolean
  var sortDependencies: Boolean
  var sortPlugins: Boolean
  var unusedKapt: Boolean
  var anvilFactoryGeneration: Boolean
  var disableAndroidResources: Boolean
  var disableViewBinding: Boolean

  companion object {

    const val REDUNDANT_DEPENDENCY_DEFAULT = false
    const val UNUSED_DEPENDENCY_DEFAULT = true
    const val OVERSHOT_DEPENDENCY_DEFAULT = true
    const val MUST_BE_API_DEFAULT = true
    const val INHERITED_DEPENDENCY_DEFAULT = true
    const val SORT_DEPENDENCIES_DEFAULT = false
    const val SORT_PLUGINS_DEFAULT = false
    const val UNUSED_KAPT_DEFAULT = true
    const val ANVIL_FACTORY_GENERATION_DEFAULT = true
    const val DISABLE_ANDROID_RESOURCES_DEFAULT = false
    const val DISABLE_VIEW_BINDING_DEFAULT = false
  }
}

interface ReportsSettings {

  /**
   * checkstyle-formatted xml report
   */
  val checkstyle: ReportSettings

  /**
   * plain-text report file matching the console output
   */
  val text: ReportSettings

  companion object {
    const val CHECKSTYLE_ENABLED_DEFAULT = false
    const val CHECKSTYLE_PATH_DEFAULT = "build/reports/modulecheck/report.xml"

    const val TEXT_ENABLED_DEFAULT = false
    const val TEXT_PATH_DEFAULT = "build/reports/modulecheck/report.txt"
  }
}

interface ReportSettings {
  var enabled: Boolean
  var outputPath: String
}
