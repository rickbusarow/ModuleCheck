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

package modulecheck.model.dependency

import modulecheck.model.sourceset.SourceSetName
import modulecheck.parsing.source.PackageName
import modulecheck.parsing.source.UnqualifiedAndroidResource
import java.io.File
import kotlin.contracts.contract

/**
 * This represents the *prevailing* build/platform plugin for a given module.
 *
 * In JVM platform projects, the Kotlin jvm plugin automatically applies the `java-library` plugin,
 * but the latter is really just an implementation detail for Kotlin.
 *
 * In Android projects, the Kotlin Android plugin *must* be added in order to use Kotlin. In this
 * case, kotlin is the implementation detail, since it's just enabling kotlin compilation in the
 * build configured by AGP.
 *
 * @since 0.12.0
 */
sealed interface PlatformPlugin : HasConfigurations, HasSourceSets {

  fun asAndroidOrNull(): AndroidPlatformPlugin? = this as? AndroidPlatformPlugin
}

fun PlatformPlugin.isAndroid(): Boolean {

  contract {
    returns(true) implies (this@isAndroid is AndroidPlatformPlugin)
  }

  return this is AndroidPlatformPlugin
}

sealed interface JvmPlatformPlugin : PlatformPlugin {

  data class JavaLibraryPlugin(
    override val sourceSets: SourceSets,
    override val configurations: Configurations
  ) : JvmPlatformPlugin

  data class KotlinJvmPlugin constructor(
    override val sourceSets: SourceSets,
    override val configurations: Configurations
  ) : JvmPlatformPlugin
}

sealed interface AndroidPlatformPlugin : PlatformPlugin {

  val nonTransientRClass: Boolean
  val viewBindingEnabled: Boolean
  val kotlinAndroidExtensionEnabled: Boolean
  val manifests: Map<SourceSetName, File>

  /**
   * Base packages as defined via the AGP DSL. There are only two versions - the "main" `namespace`
   * which is the default for everything, and `testNamespace` which is applied to all `test` and
   * `androidTest` source sets.
   *
   * @since 0.13.0
   */
  val namespaces: Map<SourceSetName, PackageName>

  /**
   * All resource declarations -- without a package -- grouped by [SourceSetName]
   *
   * @since 0.12.0
   */
  val resValues: Map<SourceSetName, Set<UnqualifiedAndroidResource>>

  interface CanDisableAndroidResources {
    val androidResourcesEnabled: Boolean
  }

  interface CanDisableAndroidBuildConfig {
    val buildConfigEnabled: Boolean
  }

  @Suppress("LongParameterList")
  class AndroidApplicationPlugin(
    override val sourceSets: SourceSets,
    override val configurations: Configurations,
    override val nonTransientRClass: Boolean,
    override val viewBindingEnabled: Boolean,
    override val kotlinAndroidExtensionEnabled: Boolean,
    override val manifests: Map<SourceSetName, File>,
    override val namespaces: Map<SourceSetName, PackageName>,
    resValuesLazy: Lazy<Map<SourceSetName, Set<UnqualifiedAndroidResource>>>
  ) : PlatformPlugin, AndroidPlatformPlugin {
    override val resValues: Map<SourceSetName, Set<UnqualifiedAndroidResource>> by resValuesLazy
  }

  @Suppress("LongParameterList")
  class AndroidLibraryPlugin(
    override val sourceSets: SourceSets,
    override val configurations: Configurations,
    override val nonTransientRClass: Boolean,
    override val viewBindingEnabled: Boolean,
    override val kotlinAndroidExtensionEnabled: Boolean,
    override val manifests: Map<SourceSetName, File>,
    override val namespaces: Map<SourceSetName, PackageName>,
    override val androidResourcesEnabled: Boolean,
    override val buildConfigEnabled: Boolean,
    resValuesLazy: Lazy<Map<SourceSetName, Set<UnqualifiedAndroidResource>>>
  ) : PlatformPlugin,
    AndroidPlatformPlugin,
    CanDisableAndroidResources,
    CanDisableAndroidBuildConfig {
    override val resValues: Map<SourceSetName, Set<UnqualifiedAndroidResource>> by resValuesLazy
  }

  @Suppress("LongParameterList")
  class AndroidDynamicFeaturePlugin(
    override val sourceSets: SourceSets,
    override val configurations: Configurations,
    override val nonTransientRClass: Boolean,
    override val viewBindingEnabled: Boolean,
    override val kotlinAndroidExtensionEnabled: Boolean,
    override val manifests: Map<SourceSetName, File>,
    override val namespaces: Map<SourceSetName, PackageName>,
    override val buildConfigEnabled: Boolean,
    resValuesLazy: Lazy<Map<SourceSetName, Set<UnqualifiedAndroidResource>>>
  ) : PlatformPlugin,
    AndroidPlatformPlugin,
    CanDisableAndroidBuildConfig {
    override val resValues: Map<SourceSetName, Set<UnqualifiedAndroidResource>> by resValuesLazy
  }

  @Suppress("LongParameterList")
  class AndroidTestPlugin(
    override val sourceSets: SourceSets,
    override val configurations: Configurations,
    override val nonTransientRClass: Boolean,
    override val viewBindingEnabled: Boolean,
    override val kotlinAndroidExtensionEnabled: Boolean,
    override val manifests: Map<SourceSetName, File>,
    override val namespaces: Map<SourceSetName, PackageName>,
    override val buildConfigEnabled: Boolean,
    resValuesLazy: Lazy<Map<SourceSetName, Set<UnqualifiedAndroidResource>>>
  ) : PlatformPlugin,
    AndroidPlatformPlugin,
    CanDisableAndroidBuildConfig {
    override val resValues: Map<SourceSetName, Set<UnqualifiedAndroidResource>> by resValuesLazy
  }
}
