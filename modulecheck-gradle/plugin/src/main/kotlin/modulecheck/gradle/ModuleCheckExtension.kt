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

@file:Suppress("DEPRECATION")

package modulecheck.gradle

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
import modulecheck.config.KaptMatcher
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

@Suppress("UnstableApiUsage")
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
   */
  override var deleteUnused: Boolean by objects.property(false)

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
   */
  override var ignoreUnusedFinding: Set<String> by objects.setProperty()

  /**
   * Set of modules which will not be excluded from error reporting. The most common use-case would
   * be if the module is the root of a dependency graph, like an Android application module, and it
   * needs everything in its classpath for dependency injection purposes.
   */
  override var doNotCheck: Set<String> by objects.setProperty()

  /** List of [KaptMatcher]'s to be checked, which aren't included by default with ModuleCheck. */
  @Suppress("DEPRECATION")
  @Deprecated("use additionalCodeGenerators instead")
  override var additionalKaptMatchers: List<KaptMatcher> by objects.listProperty()

  /**
   * List of [CodeGeneratorBinding]'s to be checked, which aren't included by default with ModuleCheck.
   */
  override var additionalCodeGenerators: List<CodeGeneratorBinding> by objects.listProperty()

  override val checks: ChecksSettings = ChecksExtension(objects)
  fun checks(action: Action<ChecksSettings>) {
    action.execute(checks)
  }

  override val sort = SortExtension(objects)
  fun sort(action: Action<SortExtension>) {
    action.execute(sort)
  }

  /** Configures reporting options */
  override val reports = ReportsExtension(objects, projectLayout)

  /** Configures reporting options */
  fun reports(action: Action<ReportsExtension>) {
    action.execute(reports)
  }
}

@Suppress("UnstableApiUsage")
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

@Suppress("UnstableApiUsage")
open class SortExtension @Inject constructor(
  objects: ObjectFactory
) : SortSettings {
  override var pluginComparators by objects
    .listProperty(PLUGIN_COMPARATORS_DEFAULT)

  override var dependencyComparators by objects
    .listProperty(DEPENDENCY_COMPARATORS_DEFAULT)
}

@Suppress("UnstableApiUsage")
open class ReportsExtension @Inject constructor(
  objects: ObjectFactory,
  projectLayout: ProjectLayout
) : ReportsSettings {

  /** checkstyle-formatted xml report */
  override val checkstyle = ReportExtension(
    objects = objects,
    enabledDefault = CHECKSTYLE_ENABLED_DEFAULT,
    outputPath = projectLayout.projectDirectory.dir(CHECKSTYLE_PATH_DEFAULT).toString()
  )

  /** checkstyle-formatted xml report */
  fun checkstyle(action: Action<ReportExtension>) {
    action.execute(checkstyle)
  }

  /** SARIF-formatted report */
  override val sarif = ReportExtension(
    objects = objects,
    enabledDefault = SARIF_ENABLED_DEFAULT,
    outputPath = projectLayout.projectDirectory.dir(SARIF_PATH_DEFAULT).toString()
  )

  /** SARIF-formatted report */
  fun sarif(action: Action<ReportExtension>) {
    action.execute(sarif)
  }

  /** plain-text report file matching the console output */
  override val text = ReportExtension(
    objects = objects,
    enabledDefault = TEXT_ENABLED_DEFAULT,
    outputPath = projectLayout.projectDirectory.dir(TEXT_PATH_DEFAULT).toString()
  )

  /** plain-text report file matching the console output */
  fun text(action: Action<ReportExtension>) {
    action.execute(text)
  }

  /** report of the depth for each source set for each module */
  override val depths = ReportExtension(
    objects = objects,
    enabledDefault = DEPTHS_ENABLED_DEFAULT,
    outputPath = projectLayout.projectDirectory.dir(DEPTHS_PATH_DEFAULT).toString()
  )

  /** report of the depth for each source set for each module */
  fun depths(action: Action<ReportExtension>) {
    action.execute(depths)
  }

  /** create dependency graphs for each source set for each module */
  override val graphs = PerModuleReportExtension(
    objects = objects,
    enabledDefault = GRAPH_ENABLED_DEFAULT,
    outputDir = null
  )

  /** create dependency graphs for each source set for each module */
  fun graphs(action: Action<PerModuleReportExtension>) {
    action.execute(graphs)
  }
}

@Suppress("UnstableApiUsage")
open class ReportExtension(
  objects: ObjectFactory,
  enabledDefault: Boolean,
  outputPath: String
) : ReportSettings {

  override var enabled: Boolean by objects.property(enabledDefault)

  /** Path for the generated file, relative to the project root. */
  override var outputPath: String by objects.property(outputPath)
}

@Suppress("UnstableApiUsage")
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
   */
  override var outputDir: String? by objects.nullableProperty(outputDir)
}
