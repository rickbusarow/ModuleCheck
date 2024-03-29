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

@file:Suppress("DEPRECATION")

package modulecheck.config

import modulecheck.api.KaptMatcher

interface ModuleCheckSettings {

  /**
   * If true, ModuleCheck will delete declarations of unused dependencies entirely.
   *
   * If false, ModuleCheck will comment out declarations of unused dependencies.
   *
   * Default value is false.
   *
   * @since 0.12.0
   */
  var deleteUnused: Boolean

  /**
   * If true, ModuleCheck will collect a trace of expensive and delicate
   * operations. This trace is added to any thrown exceptions. Tracing
   * is disabled by default, as it does incur a performance penalty.
   *
   * Default value is false
   *
   * @since 0.12.0
   */
  var trace: Boolean

  /**
   * Set of modules which are allowed to be unused.
   *
   * For instance, given:
   *
   * ```
   * ignoreUnusedFinding = setOf(":core")
   * ```
   *
   * If a module declares `:core` as a dependency but does not use it, no finding will be reported.
   *
   * @since 0.12.0
   */
  var ignoreUnusedFinding: Set<String>

  /**
   * Set of modules which will not be excluded from error reporting. The most common use-case
   * would be if the module is the root of a dependency graph, like an Android application
   * module, and it needs everything in its classpath for dependency injection purposes.
   *
   * @since 0.12.0
   */
  var doNotCheck: Set<String>

  /**
   * List of [KaptMatcher]'s to be checked, which aren't included by default with ModuleCheck.
   *
   * @since 0.12.0
   */
  @Suppress("DEPRECATION")
  @Deprecated("use additionalCodeGenerators instead")
  var additionalKaptMatchers: List<KaptMatcher>

  /**
   * List of [CodeGeneratorBinding]'s to be checked,
   * which aren't included by default with ModuleCheck.
   *
   * ```
   * moduleCheck {
   *   additionalCompilerExtensions = listOf(
   *     modulecheck.config.CompilerExtension.AnnotationProcessor(
   *       name = "My Processor",
   *       generatorMavenCoordinates = "my-project.codegen:processor",
   *       annotationNames = listOf(
   *         "myproject.MyInject",
   *         "myproject.MyInject.Factory",
   *         "myproject.MyInjectParam",
   *         "myproject.MyInjectModule"
   *       )
   *     )
   *   )
   * }
   * ```
   *
   * @since 0.12.0
   */
  var additionalCodeGenerators: List<CodeGeneratorBinding>

  val checks: ChecksSettings

  val sort: SortSettings

  /**
   * Configures reporting options
   *
   * @since 0.12.0
   */
  val reports: ReportsSettings
}

interface SortSettings {
  var pluginComparators: List<String>
  var dependencyComparators: List<String>

  companion object {

    val PLUGIN_COMPARATORS_DEFAULT: List<String> = listOf(
      """id\("com\.android.*"\)""",
      """id\("android-.*"\)""",
      """id\("java-library"\)""",
      """kotlin\("jvm"\)""",
      """android.*""",
      """javaLibrary.*""",
      """kotlin.*""",
      """id.*"""
    )
    val DEPENDENCY_COMPARATORS_DEFAULT: List<String> = listOf(
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
  var unusedKotlinAndroidExtensions: Boolean
  var depths: Boolean

  companion object {

    const val REDUNDANT_DEPENDENCY_DEFAULT: Boolean = false
    const val UNUSED_DEPENDENCY_DEFAULT: Boolean = true
    const val OVERSHOT_DEPENDENCY_DEFAULT: Boolean = true
    const val MUST_BE_API_DEFAULT: Boolean = true
    const val INHERITED_DEPENDENCY_DEFAULT: Boolean = true
    const val SORT_DEPENDENCIES_DEFAULT: Boolean = false
    const val SORT_PLUGINS_DEFAULT: Boolean = false
    const val UNUSED_KAPT_DEFAULT: Boolean = true
    const val UNUSED_KOTLIN_ANDROID_EXTENSIONS_DEFAULT: Boolean = false
    const val ANVIL_FACTORY_GENERATION_DEFAULT: Boolean = true
    const val DISABLE_ANDROID_RESOURCES_DEFAULT: Boolean = false
    const val DISABLE_VIEW_BINDING_DEFAULT: Boolean = false
    const val DEPTHS_DEFAULT: Boolean = false
  }
}

interface ReportsSettings {

  /**
   * checkstyle-formatted xml report
   *
   * @since 0.12.0
   */
  val checkstyle: ReportSettings

  /**
   * SARIF-formatted report
   *
   * @since 0.12.0
   */
  val sarif: ReportSettings

  /**
   * plain-text report file matching the console output
   *
   * @since 0.12.0
   */
  val text: ReportSettings

  /**
   * report of the depth for each source set for each module
   *
   * @since 0.12.0
   */
  val depths: ReportSettings

  /**
   * create dependency graphs for each source set for each module
   *
   * @since 0.12.0
   */
  val graphs: PerModuleReportSettings

  companion object {
    const val CHECKSTYLE_ENABLED_DEFAULT: Boolean = false
    const val CHECKSTYLE_PATH_DEFAULT: String = "build/reports/modulecheck/report.xml"

    const val TEXT_ENABLED_DEFAULT: Boolean = false
    const val TEXT_PATH_DEFAULT: String = "build/reports/modulecheck/report.txt"

    const val DEPTHS_ENABLED_DEFAULT: Boolean = false
    const val DEPTHS_PATH_DEFAULT: String = "build/reports/modulecheck/depths.txt"

    const val SARIF_ENABLED_DEFAULT: Boolean = false
    const val SARIF_PATH_DEFAULT: String = "build/reports/modulecheck/modulecheck.sarif"

    const val GRAPH_ENABLED_DEFAULT: Boolean = false
  }
}

interface ReportSettings {
  var enabled: Boolean
  var outputPath: String
}

interface PerModuleReportSettings {
  var enabled: Boolean
  var outputDir: String?
}
