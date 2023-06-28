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

@file:Suppress("DEPRECATION", "unused")

package modulecheck.gradle

import modulecheck.api.KaptMatcher
import modulecheck.config.ChecksSettings
import modulecheck.config.ChecksSettings.Companion.ANVIL_FACTORY_GENERATION_DEFAULT
import modulecheck.config.ChecksSettings.Companion.DEPTHS_DEFAULT
import modulecheck.config.ChecksSettings.Companion.DISABLE_ANDROID_RESOURCES_DEFAULT
import modulecheck.config.ChecksSettings.Companion.DISABLE_VIEW_BINDING_DEFAULT
import modulecheck.config.ChecksSettings.Companion.INHERITED_DEPENDENCY_DEFAULT
import modulecheck.config.ChecksSettings.Companion.MUST_BE_API_DEFAULT
import modulecheck.config.ChecksSettings.Companion.OVERSHOT_DEPENDENCY_DEFAULT
import modulecheck.config.ChecksSettings.Companion.REDUNDANT_DEPENDENCY_DEFAULT
import modulecheck.config.ChecksSettings.Companion.SORT_DEPENDENCIES_DEFAULT
import modulecheck.config.ChecksSettings.Companion.SORT_PLUGINS_DEFAULT
import modulecheck.config.ChecksSettings.Companion.UNUSED_DEPENDENCY_DEFAULT
import modulecheck.config.ChecksSettings.Companion.UNUSED_KAPT_DEFAULT
import modulecheck.config.ChecksSettings.Companion.UNUSED_KOTLIN_ANDROID_EXTENSIONS_DEFAULT
import modulecheck.config.CodeGeneratorBinding
import modulecheck.config.ModuleCheckSettings
import modulecheck.config.PerModuleReportSettings
import modulecheck.config.ReportSettings
import modulecheck.config.ReportsSettings
import modulecheck.config.ReportsSettings.Companion.CHECKSTYLE_ENABLED_DEFAULT
import modulecheck.config.ReportsSettings.Companion.CHECKSTYLE_PATH_DEFAULT
import modulecheck.config.ReportsSettings.Companion.DEPTHS_ENABLED_DEFAULT
import modulecheck.config.ReportsSettings.Companion.DEPTHS_PATH_DEFAULT
import modulecheck.config.ReportsSettings.Companion.GRAPH_ENABLED_DEFAULT
import modulecheck.config.ReportsSettings.Companion.SARIF_ENABLED_DEFAULT
import modulecheck.config.ReportsSettings.Companion.SARIF_PATH_DEFAULT
import modulecheck.config.ReportsSettings.Companion.TEXT_ENABLED_DEFAULT
import modulecheck.config.ReportsSettings.Companion.TEXT_PATH_DEFAULT
import modulecheck.config.SortSettings
import modulecheck.config.SortSettings.Companion.DEPENDENCY_COMPARATORS_DEFAULT
import modulecheck.config.SortSettings.Companion.PLUGIN_COMPARATORS_DEFAULT
import modulecheck.gradle.internal.listProperty
import modulecheck.gradle.internal.nullableProperty
import modulecheck.gradle.internal.property
import modulecheck.gradle.internal.setProperty
import org.gradle.api.Action
import org.gradle.api.file.ProjectLayout
import org.gradle.api.model.ObjectFactory
import javax.inject.Inject

@Suppress("ClassOrdering")
open class ModuleCheckExtension @Inject constructor(
  objects: ObjectFactory,
  projectLayout: ProjectLayout
) : ModuleCheckSettings {

  /**
   * If true, ModuleCheck will delete declarations of unused dependencies entirely.
   *
   * If false, ModuleCheck will comment out declarations of unused dependencies.
   *
   * Default value is false.
   *
   * @since 0.12.0
   */
  override var deleteUnused: Boolean by objects.property(false)

  /**
   * If true, ModuleCheck will collect a trace of expensive and delicate
   * operations. This trace is added to any thrown exceptions. Tracing
   * is disabled by default, as it does incur a performance penalty.
   *
   * Default value is false
   *
   * @since 0.12.0
   */
  override var trace: Boolean by objects.property(false)

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
  override var ignoreUnusedFinding: Set<String> by objects.setProperty()

  /**
   * Set of modules which will not be excluded from error reporting. The most common use-case
   * would be if the module is the root of a dependency graph, like an Android application
   * module, and it needs everything in its classpath for dependency injection purposes.
   *
   * @since 0.12.0
   */
  override var doNotCheck: Set<String> by objects.setProperty()

  /**
   * List of [KaptMatcher]'s to be checked, which aren't included by default with ModuleCheck.
   *
   * @since 0.12.0
   */
  @Suppress("DEPRECATION")
  @Deprecated("use additionalCodeGenerators instead")
  override var additionalKaptMatchers: List<KaptMatcher> by objects.listProperty()

  /**
   * List of [CodeGeneratorBinding]'s to be checked,
   * which aren't included by default with ModuleCheck.
   *
   * @since 0.12.0
   */
  override var additionalCodeGenerators: List<CodeGeneratorBinding> by objects.listProperty()

  override val checks: ChecksSettings = ChecksExtension(objects)
  fun checks(action: Action<ChecksSettings>) {
    action.execute(checks)
  }

  override val sort: SortExtension = SortExtension(objects)
  fun sort(action: Action<SortExtension>) {
    action.execute(sort)
  }

  /**
   * Configures reporting options
   *
   * @since 0.12.0
   */
  override val reports: ReportsExtension = ReportsExtension(objects, projectLayout)

  /**
   * Configures reporting options
   *
   * @since 0.12.0
   */
  fun reports(action: Action<ReportsExtension>) {
    action.execute(reports)
  }
}

open class ChecksExtension @Inject constructor(
  objects: ObjectFactory
) : ChecksSettings {
  override var redundantDependency: Boolean by objects.property(REDUNDANT_DEPENDENCY_DEFAULT)
  override var unusedDependency: Boolean by objects.property(UNUSED_DEPENDENCY_DEFAULT)
  override var overShotDependency: Boolean by objects.property(OVERSHOT_DEPENDENCY_DEFAULT)
  override var mustBeApi: Boolean by objects.property(MUST_BE_API_DEFAULT)
  override var inheritedDependency: Boolean by objects.property(INHERITED_DEPENDENCY_DEFAULT)
  override var sortDependencies: Boolean by objects.property(SORT_DEPENDENCIES_DEFAULT)
  override var sortPlugins: Boolean by objects.property(SORT_PLUGINS_DEFAULT)
  override var unusedKapt: Boolean by objects.property(UNUSED_KAPT_DEFAULT)
  override var anvilFactoryGeneration: Boolean by objects.property(ANVIL_FACTORY_GENERATION_DEFAULT)
  override var disableAndroidResources: Boolean by objects.property(
    DISABLE_ANDROID_RESOURCES_DEFAULT
  )
  override var disableViewBinding: Boolean by objects.property(DISABLE_VIEW_BINDING_DEFAULT)
  override var unusedKotlinAndroidExtensions: Boolean by objects.property(
    UNUSED_KOTLIN_ANDROID_EXTENSIONS_DEFAULT
  )
  override var depths: Boolean by objects.property(DEPTHS_DEFAULT)
}

open class SortExtension @Inject constructor(
  objects: ObjectFactory
) : SortSettings {
  override var pluginComparators: List<String> by objects
    .listProperty(PLUGIN_COMPARATORS_DEFAULT)

  override var dependencyComparators: List<String> by objects
    .listProperty(DEPENDENCY_COMPARATORS_DEFAULT)
}

@Suppress("ClassOrdering")
open class ReportsExtension @Inject constructor(
  objects: ObjectFactory,
  projectLayout: ProjectLayout
) : ReportsSettings {

  /**
   * checkstyle-formatted xml report
   *
   * @since 0.12.0
   */
  override val checkstyle: ReportExtension = ReportExtension(
    objects = objects,
    enabledDefault = CHECKSTYLE_ENABLED_DEFAULT,
    outputPath = projectLayout.projectDirectory.dir(CHECKSTYLE_PATH_DEFAULT).toString()
  )

  /**
   * checkstyle-formatted xml report
   *
   * @since 0.12.0
   */
  fun checkstyle(action: Action<ReportExtension>) {
    action.execute(checkstyle)
  }

  /**
   * SARIF-formatted report
   *
   * @since 0.12.0
   */
  override val sarif: ReportExtension = ReportExtension(
    objects = objects,
    enabledDefault = SARIF_ENABLED_DEFAULT,
    outputPath = projectLayout.projectDirectory.dir(SARIF_PATH_DEFAULT).toString()
  )

  /**
   * SARIF-formatted report
   *
   * @since 0.12.0
   */
  fun sarif(action: Action<ReportExtension>) {
    action.execute(sarif)
  }

  /**
   * plain-text report file matching the console output
   *
   * @since 0.12.0
   */
  override val text: ReportExtension = ReportExtension(
    objects = objects,
    enabledDefault = TEXT_ENABLED_DEFAULT,
    outputPath = projectLayout.projectDirectory.dir(TEXT_PATH_DEFAULT).toString()
  )

  /**
   * plain-text report file matching the console output
   *
   * @since 0.12.0
   */
  fun text(action: Action<ReportExtension>) {
    action.execute(text)
  }

  /**
   * report of the depth for each source set for each module
   *
   * @since 0.12.0
   */
  override val depths: ReportExtension = ReportExtension(
    objects = objects,
    enabledDefault = DEPTHS_ENABLED_DEFAULT,
    outputPath = projectLayout.projectDirectory.dir(DEPTHS_PATH_DEFAULT).toString()
  )

  /**
   * report of the depth for each source set for each module
   *
   * @since 0.12.0
   */
  fun depths(action: Action<ReportExtension>) {
    action.execute(depths)
  }

  /**
   * create dependency graphs for each source set for each module
   *
   * @since 0.12.0
   */
  override val graphs: PerModuleReportExtension = PerModuleReportExtension(
    objects = objects,
    enabledDefault = GRAPH_ENABLED_DEFAULT,
    outputDir = null
  )

  /**
   * create dependency graphs for each source set for each module
   *
   * @since 0.12.0
   */
  fun graphs(action: Action<PerModuleReportExtension>) {
    action.execute(graphs)
  }
}

open class ReportExtension(
  objects: ObjectFactory,
  enabledDefault: Boolean,
  outputPath: String
) : ReportSettings {

  override var enabled: Boolean by objects.property(enabledDefault)

  /**
   * Path for the generated file, relative to the project root.
   *
   * @since 0.12.0
   */
  override var outputPath: String by objects.property(outputPath)
}

open class PerModuleReportExtension(
  objects: ObjectFactory,
  enabledDefault: Boolean,
  outputDir: String?
) : PerModuleReportSettings {

  override var enabled: Boolean by objects.property(enabledDefault)

  /**
   * Path to the root directory of the generated files, relative to the project root.
   *
   * If this is null, then reports will be created in
   * `$projectDir/build/reports/modulecheck/graphs/`.
   *
   * @since 0.12.0
   */
  override var outputDir: String? by objects.nullableProperty(outputDir)
}
