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

package modulecheck.parsing.gradle

import modulecheck.parsing.source.UnqualifiedAndroidResourceDeclaredName
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
 */
sealed interface PlatformPlugin : HasConfigurations {

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

  data class KotlinJvmPlugin(
    override val sourceSets: SourceSets,
    override val configurations: Configurations
  ) : JvmPlatformPlugin
}

sealed interface AndroidPlatformPlugin : PlatformPlugin {

  val nonTransientRClass: Boolean
  val viewBindingEnabled: Boolean
  val kotlinAndroidExtensionEnabled: Boolean
  val manifests: Map<SourceSetName, File>
  val resValues: Map<SourceSetName, Set<UnqualifiedAndroidResourceDeclaredName>>

  interface CanDisableAndroidResources {
    val androidResourcesEnabled: Boolean
  }

  interface CanDisableAndroidBuildConfig {
    val buildConfigEnabled: Boolean
  }

  data class AndroidApplicationPlugin(
    override val sourceSets: SourceSets,
    override val configurations: Configurations,
    override val nonTransientRClass: Boolean,
    override val viewBindingEnabled: Boolean,
    override val kotlinAndroidExtensionEnabled: Boolean,
    override val manifests: Map<SourceSetName, File>,
    override val resValues: Map<SourceSetName, Set<UnqualifiedAndroidResourceDeclaredName>>
  ) : PlatformPlugin, AndroidPlatformPlugin

  data class AndroidLibraryPlugin(
    override val sourceSets: SourceSets,
    override val configurations: Configurations,
    override val nonTransientRClass: Boolean,
    override val viewBindingEnabled: Boolean,
    override val kotlinAndroidExtensionEnabled: Boolean,
    override val manifests: Map<SourceSetName, File>,
    override val androidResourcesEnabled: Boolean,
    override val buildConfigEnabled: Boolean,
    override val resValues: Map<SourceSetName, Set<UnqualifiedAndroidResourceDeclaredName>>
  ) : PlatformPlugin,
    AndroidPlatformPlugin,
    CanDisableAndroidResources,
    CanDisableAndroidBuildConfig

  data class AndroidDynamicFeaturePlugin(
    override val sourceSets: SourceSets,
    override val configurations: Configurations,
    override val nonTransientRClass: Boolean,
    override val viewBindingEnabled: Boolean,
    override val kotlinAndroidExtensionEnabled: Boolean,
    override val manifests: Map<SourceSetName, File>,
    override val buildConfigEnabled: Boolean,
    override val resValues: Map<SourceSetName, Set<UnqualifiedAndroidResourceDeclaredName>>
  ) : PlatformPlugin,
    AndroidPlatformPlugin,
    CanDisableAndroidBuildConfig

  data class AndroidTestPlugin(
    override val sourceSets: SourceSets,
    override val configurations: Configurations,
    override val nonTransientRClass: Boolean,
    override val viewBindingEnabled: Boolean,
    override val kotlinAndroidExtensionEnabled: Boolean,
    override val manifests: Map<SourceSetName, File>,
    override val buildConfigEnabled: Boolean,
    override val resValues: Map<SourceSetName, Set<UnqualifiedAndroidResourceDeclaredName>>
  ) : PlatformPlugin,
    AndroidPlatformPlugin,
    CanDisableAndroidBuildConfig
}
