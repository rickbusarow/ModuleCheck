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

package modulecheck.project.test

import modulecheck.parsing.gradle.model.AndroidPlatformPlugin
import modulecheck.parsing.gradle.model.AndroidPlatformPlugin.AndroidApplicationPlugin
import modulecheck.parsing.gradle.model.AndroidPlatformPlugin.AndroidDynamicFeaturePlugin
import modulecheck.parsing.gradle.model.AndroidPlatformPlugin.AndroidLibraryPlugin
import modulecheck.parsing.gradle.model.AndroidPlatformPlugin.AndroidTestPlugin
import modulecheck.parsing.gradle.model.ConfigurationName
import modulecheck.parsing.gradle.model.Configurations
import modulecheck.parsing.gradle.model.JvmPlatformPlugin.JavaLibraryPlugin
import modulecheck.parsing.gradle.model.JvmPlatformPlugin.KotlinJvmPlugin
import modulecheck.parsing.gradle.model.PlatformPlugin
import modulecheck.parsing.gradle.model.SourceSetName
import modulecheck.parsing.gradle.model.SourceSets
import modulecheck.parsing.source.UnqualifiedAndroidResourceDeclaredName
import java.io.File

interface PlatformPluginBuilder<T : PlatformPlugin> {
  val sourceSets: MutableMap<SourceSetName, SourceSetBuilder>
  val configurations: MutableMap<ConfigurationName, ConfigBuilder>

  fun toPlugin(): T
}

data class JavaLibraryPluginBuilder(
  override val sourceSets: MutableMap<SourceSetName, SourceSetBuilder> = mutableMapOf(),
  override val configurations: MutableMap<ConfigurationName, ConfigBuilder> = mutableMapOf()
) : PlatformPluginBuilder<JavaLibraryPlugin> {

  override fun toPlugin(): JavaLibraryPlugin = JavaLibraryPlugin(
    sourceSets = SourceSets(sourceSets.mapValues { it.value.toSourceSet() }),
    configurations = Configurations(configurations.mapValues { it.value.toConfig(configFactory) })
  )
}

data class KotlinJvmPluginBuilder(
  override val sourceSets: MutableMap<SourceSetName, SourceSetBuilder> = mutableMapOf(),
  override val configurations: MutableMap<ConfigurationName, ConfigBuilder> = mutableMapOf()
) : PlatformPluginBuilder<KotlinJvmPlugin> {
  override fun toPlugin(): KotlinJvmPlugin = KotlinJvmPlugin(
    sourceSets = SourceSets(sourceSets.mapValues { it.value.toSourceSet() }),
    configurations = Configurations(configurations.mapValues { it.value.toConfig(configFactory) })
  )
}

interface AndroidPlatformPluginBuilder<T : AndroidPlatformPlugin> : PlatformPluginBuilder<T> {
  var viewBindingEnabled: Boolean
  var nonTransientRClass: Boolean
  var kotlinAndroidExtensionEnabled: Boolean
  val manifests: MutableMap<SourceSetName, File>
  val resValues: MutableMap<SourceSetName, Set<UnqualifiedAndroidResourceDeclaredName>>
}

data class AndroidApplicationPluginBuilder(
  override var viewBindingEnabled: Boolean = true,
  override var nonTransientRClass: Boolean = false,
  override var kotlinAndroidExtensionEnabled: Boolean = true,
  override val manifests: MutableMap<SourceSetName, File> = mutableMapOf(),
  override val sourceSets: MutableMap<SourceSetName, SourceSetBuilder> = mutableMapOf(),
  override val configurations: MutableMap<ConfigurationName, ConfigBuilder> = mutableMapOf(),
  override val resValues: MutableMap<SourceSetName, Set<UnqualifiedAndroidResourceDeclaredName>> = mutableMapOf()
) : AndroidPlatformPluginBuilder<AndroidApplicationPlugin> {
  override fun toPlugin(): AndroidApplicationPlugin = AndroidApplicationPlugin(
    sourceSets = SourceSets(sourceSets.mapValues { it.value.toSourceSet() }),
    configurations = Configurations(configurations.mapValues { it.value.toConfig(configFactory) }),
    nonTransientRClass = nonTransientRClass,
    viewBindingEnabled = viewBindingEnabled,
    kotlinAndroidExtensionEnabled = kotlinAndroidExtensionEnabled,
    manifests = manifests,
    resValues = resValues
  )
}

data class AndroidLibraryPluginBuilder(
  override var viewBindingEnabled: Boolean = true,
  override var nonTransientRClass: Boolean = false,
  override var kotlinAndroidExtensionEnabled: Boolean = true,
  var buildConfigEnabled: Boolean = true,
  var androidResourcesEnabled: Boolean = true,
  override val manifests: MutableMap<SourceSetName, File> = mutableMapOf(),
  override val sourceSets: MutableMap<SourceSetName, SourceSetBuilder> = mutableMapOf(),
  override val configurations: MutableMap<ConfigurationName, ConfigBuilder> = mutableMapOf(),
  override val resValues: MutableMap<SourceSetName, Set<UnqualifiedAndroidResourceDeclaredName>> = mutableMapOf()
) : AndroidPlatformPluginBuilder<AndroidLibraryPlugin> {
  override fun toPlugin(): AndroidLibraryPlugin = AndroidLibraryPlugin(
    sourceSets = SourceSets(sourceSets.mapValues { it.value.toSourceSet() }),
    configurations = Configurations(configurations.mapValues { it.value.toConfig(configFactory) }),
    nonTransientRClass = nonTransientRClass,
    viewBindingEnabled = viewBindingEnabled,
    kotlinAndroidExtensionEnabled = kotlinAndroidExtensionEnabled,
    manifests = manifests,
    androidResourcesEnabled = androidResourcesEnabled,
    buildConfigEnabled = buildConfigEnabled,
    resValues = resValues
  )
}

data class AndroidDynamicFeaturePluginBuilder(
  override var viewBindingEnabled: Boolean = true,
  override var nonTransientRClass: Boolean = false,
  override var kotlinAndroidExtensionEnabled: Boolean = true,
  var buildConfigEnabled: Boolean = true,
  override val manifests: MutableMap<SourceSetName, File> = mutableMapOf(),
  override val sourceSets: MutableMap<SourceSetName, SourceSetBuilder> = mutableMapOf(),
  override val configurations: MutableMap<ConfigurationName, ConfigBuilder> = mutableMapOf(),
  override val resValues: MutableMap<SourceSetName, Set<UnqualifiedAndroidResourceDeclaredName>> = mutableMapOf()
) : AndroidPlatformPluginBuilder<AndroidDynamicFeaturePlugin> {
  override fun toPlugin(): AndroidDynamicFeaturePlugin = AndroidDynamicFeaturePlugin(
    sourceSets = SourceSets(sourceSets.mapValues { it.value.toSourceSet() }),
    configurations = Configurations(configurations.mapValues { it.value.toConfig(configFactory) }),
    nonTransientRClass = nonTransientRClass,
    viewBindingEnabled = viewBindingEnabled,
    kotlinAndroidExtensionEnabled = kotlinAndroidExtensionEnabled,
    manifests = manifests,
    buildConfigEnabled = buildConfigEnabled,
    resValues = resValues
  )
}

data class AndroidTestPluginBuilder(
  override var viewBindingEnabled: Boolean = true,
  override var nonTransientRClass: Boolean = false,
  override var kotlinAndroidExtensionEnabled: Boolean = true,
  var buildConfigEnabled: Boolean = true,
  override val manifests: MutableMap<SourceSetName, File> = mutableMapOf(),
  override val sourceSets: MutableMap<SourceSetName, SourceSetBuilder> = mutableMapOf(),
  override val configurations: MutableMap<ConfigurationName, ConfigBuilder> = mutableMapOf(),
  override val resValues: MutableMap<SourceSetName, Set<UnqualifiedAndroidResourceDeclaredName>> = mutableMapOf()
) : AndroidPlatformPluginBuilder<AndroidTestPlugin> {
  override fun toPlugin(): AndroidTestPlugin = AndroidTestPlugin(
    sourceSets = SourceSets(sourceSets.mapValues { it.value.toSourceSet() }),
    configurations = Configurations(configurations.mapValues { it.value.toConfig(configFactory) }),
    nonTransientRClass = nonTransientRClass,
    viewBindingEnabled = viewBindingEnabled,
    kotlinAndroidExtensionEnabled = kotlinAndroidExtensionEnabled,
    manifests = manifests,
    buildConfigEnabled = buildConfigEnabled,
    resValues = resValues
  )
}
