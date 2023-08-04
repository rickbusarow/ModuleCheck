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

package modulecheck.gradle.platforms.android

import com.squareup.anvil.annotations.ContributesBinding
import modulecheck.dagger.TaskScope
import modulecheck.gradle.platforms.ConfigurationsFactory
import modulecheck.gradle.platforms.SourceSetsFactory
import modulecheck.gradle.platforms.android.RealAndroidPlatformPluginFactory.AgpExtensionType.Application
import modulecheck.gradle.platforms.android.RealAndroidPlatformPluginFactory.AgpExtensionType.DynamicFeature
import modulecheck.gradle.platforms.android.RealAndroidPlatformPluginFactory.AgpExtensionType.Library
import modulecheck.gradle.platforms.android.RealAndroidPlatformPluginFactory.AgpExtensionType.Test
import modulecheck.gradle.platforms.android.internal.androidManifests
import modulecheck.gradle.platforms.android.internal.androidNamespaces
import modulecheck.gradle.platforms.android.internal.orPropertyDefault
import modulecheck.model.dependency.AndroidPlatformPlugin
import modulecheck.model.dependency.AndroidPlatformPlugin.AndroidApplicationPlugin
import modulecheck.model.dependency.AndroidPlatformPlugin.AndroidDynamicFeaturePlugin
import modulecheck.model.dependency.AndroidPlatformPlugin.AndroidLibraryPlugin
import modulecheck.model.dependency.AndroidPlatformPlugin.AndroidTestPlugin
import modulecheck.model.sourceset.SourceSetName
import modulecheck.model.sourceset.asSourceSetName
import modulecheck.parsing.gradle.model.GradleProject
import modulecheck.parsing.source.UnqualifiedAndroidResource
import modulecheck.utils.cast
import modulecheck.utils.requireNotNull
import net.swiftzer.semver.SemVer
import javax.inject.Inject
import kotlin.LazyThreadSafetyMode.NONE

/**
 * Factory for creating [AndroidPlatformPlugin] instances
 * based on the type of Android Gradle Plugin (AGP) extension.
 *
 * @property agpApiAccess Provides access to AGP APIs.
 * @property configurationsFactory Factory for creating [Configurations] instances.
 * @property sourceSetsFactory Factory for creating [SourceSets] instances.
 */
@ContributesBinding(TaskScope::class)
class RealAndroidPlatformPluginFactory @Inject constructor(
  private val agpApiAccess: AgpApiAccess,
  private val configurationsFactory: ConfigurationsFactory,
  private val sourceSetsFactory: SourceSetsFactory
) : AndroidPlatformPluginFactory {

  private val agp8 by lazy(NONE) { SemVer(major = 8, minor = 0, patch = 0) }

  /**
   * Creates an [AndroidPlatformPlugin] based on the provided parameters. It
   * handles different AGP extension types and applies the necessary configurations.
   *
   * @param gradleProject The Gradle project information.
   * @param agpCommonExtension Common AGP extension interface.
   * @param hasTestFixturesPlugin Flag indicating if the test fixtures plugin is present.
   * @return The created [AndroidPlatformPlugin] instance.
   */
  @UnsafeDirectAgpApiReference
  override fun create(
    gradleProject: GradleProject,
    agpCommonExtension: AgpCommonExtension,
    hasTestFixturesPlugin: Boolean
  ): AndroidPlatformPlugin {

    val type = AgpExtensionType.from(agpCommonExtension)

    val configurations = configurationsFactory.create(gradleProject)

    val sourceSets = sourceSetsFactory.create(
      gradleProject = gradleProject,
      configurations = configurations,
      hasTestFixturesPlugin = hasTestFixturesPlugin
    )

    val manifests = gradleProject.androidManifests(agpApiAccess).orEmpty()

    val namespaces = gradleProject.androidNamespaces(agpApiAccess, sourceSets).orEmpty()

    val resValuesLazy = lazy { parseResValues(type) }

    val hasKotlinAndroidExtensions = gradleProject
      .pluginManager
      .hasPlugin("android-extensions")

    // Defaults for `nonTransitiveRClass` and `buildconfig` changed with AGP 8.0,
    // so we have to do the version check.
    // https://developer.android.com/build/releases/gradle-plugin#default-changes
    val agpVersion = agpApiAccess.agpVersionOrNull
      .requireNotNull { "Could not parse the AGP version" }

    val nonTransientRClass = gradleProject
      .findProperty("android.nonTransitiveRClass")?.toString()
      ?.toBooleanStrictOrNull()
      ?: (agpVersion >= agp8)

    @Suppress("UnstableApiUsage")
    val buildConfigEnabled = type.extension.buildFeatures.buildConfig
      .orPropertyDefault(
        gradleProject = gradleProject,
        key = "android.defaults.buildfeatures.buildconfig",
        defaultValue = agpVersion < agp8
      )

    @Suppress("UnstableApiUsage")
    val viewBindingEnabled = type.extension.buildFeatures.viewBinding
      .orPropertyDefault(
        gradleProject = gradleProject,
        key = "android.defaults.buildfeatures.viewbinding",
        defaultValue = false
      )

    @Suppress("UnstableApiUsage")
    val androidResourcesEnabled = (type.extension as? AgpLibraryExtension)
      ?.buildFeatures
      ?.androidResources
      .orPropertyDefault(
        gradleProject = gradleProject,
        key = "android.library.defaults.buildfeatures.androidresources",
        defaultValue = true
      )

    return when (type) {
      is Application -> AndroidApplicationPlugin(
        sourceSets = sourceSets,
        configurations = configurations,
        nonTransientRClass = nonTransientRClass,
        viewBindingEnabled = viewBindingEnabled,
        kotlinAndroidExtensionEnabled = hasKotlinAndroidExtensions,
        manifests = manifests,
        namespaces = namespaces,
        resValuesLazy = resValuesLazy
      )

      is DynamicFeature -> AndroidDynamicFeaturePlugin(
        sourceSets = sourceSets,
        configurations = configurations,
        nonTransientRClass = nonTransientRClass,
        viewBindingEnabled = viewBindingEnabled,
        kotlinAndroidExtensionEnabled = hasKotlinAndroidExtensions,
        manifests = manifests,
        namespaces = namespaces,
        buildConfigEnabled = buildConfigEnabled,
        resValuesLazy = resValuesLazy
      )

      is Library -> AndroidLibraryPlugin(
        sourceSets = sourceSets,
        configurations = configurations,
        nonTransientRClass = nonTransientRClass,
        viewBindingEnabled = viewBindingEnabled,
        kotlinAndroidExtensionEnabled = hasKotlinAndroidExtensions,
        manifests = manifests,
        namespaces = namespaces,
        androidResourcesEnabled = androidResourcesEnabled,
        buildConfigEnabled = buildConfigEnabled,
        resValuesLazy = resValuesLazy
      )

      is Test -> AndroidTestPlugin(
        sourceSets = sourceSets,
        configurations = configurations,
        nonTransientRClass = nonTransientRClass,
        viewBindingEnabled = viewBindingEnabled,
        kotlinAndroidExtensionEnabled = hasKotlinAndroidExtensions,
        manifests = manifests,
        namespaces = namespaces,
        buildConfigEnabled = buildConfigEnabled,
        resValuesLazy = resValuesLazy
      )
    }
  }

  @UnsafeDirectAgpApiReference
  private fun parseResValues(
    type: AgpExtensionType<*>
  ): MutableMap<SourceSetName, Set<UnqualifiedAndroidResource>> {
    fun Any.mergedFlavors(): List<AgpMergedFlavor> {
      return when (this) {
        is AgpAppExtension -> applicationVariants.map { it.cast<AgpApplicationVariantImpl>().mergedFlavor }
        is AgpLibraryExtension -> libraryVariants.map { it.cast<AgpLibraryVariantImpl>().mergedFlavor }
        else -> emptyList()
      }
    }

    fun Any.buildTypes(): List<com.android.builder.model.BuildType> {
      return when (this) {
        is AgpAppExtension -> applicationVariants.mapNotNull { it.buildType }
        is AgpLibraryExtension -> libraryVariants.mapNotNull { it.buildType }
        else -> emptyList()
      }
    }

    val map = type.extension.mergedFlavors()
      .associate { mergedFlavor ->
        val sourceSetName = mergedFlavor.name.asSourceSetName()

        sourceSetName to mergedFlavor.resValues.values
          .mapNotNull { classField ->
            UnqualifiedAndroidResource.fromValuePair(classField.type, classField.name)
          }.toSet()
      }.toMutableMap()

    type.extension.buildTypes()
      .forEach { buildType ->
        val sourceSetName = buildType.name.asSourceSetName()

        map[sourceSetName] = buildType.resValues.values
          .mapNotNull { classField ->
            UnqualifiedAndroidResource.fromValuePair(classField.type, classField.name)
          }.toSet()
      }

    return map
  }

  /** Makes the base AGP extension types exhaustive. */
  sealed interface AgpExtensionType<T : AgpCommonExtension> {
    /** */
    val extension: T

    /** [com.android.build.gradle.LibraryExtension] */
    data class Library(
      override val extension: AgpLibraryExtension
    ) : AgpExtensionType<AgpLibraryExtension>

    /** [com.android.build.api.dsl.ApplicationExtension] */
    data class Application(
      override val extension: AgpApplicationExtension
    ) : AgpExtensionType<AgpApplicationExtension>

    /** [com.android.build.gradle.TestExtension] */
    data class Test(override val extension: AgpTestExtension) : AgpExtensionType<AgpTestExtension>

    /** [com.android.build.api.dsl.DynamicFeatureExtension] */
    data class DynamicFeature(
      override val extension: AgpDynamicFeatureExtension
    ) : AgpExtensionType<AgpDynamicFeatureExtension>

    companion object {
      /**
       * Factory method to create an [AgpExtensionType] from the given extension object.
       *
       * @param extension The extension object.
       * @return The corresponding [AgpExtensionType].
       * @throws IllegalArgumentException If the extension type is unrecognized.
       */
      fun from(extension: Any): AgpExtensionType<*> {
        return when (extension) {
          is AgpLibraryExtension -> Library(extension)
          is AgpApplicationExtension -> Application(extension)
          is AgpTestExtension -> Test(extension)
          is AgpDynamicFeatureExtension -> DynamicFeature(extension)
          else -> error("unrecognized Android extension ${extension::class.java.canonicalName}")
        }
      }
    }
  }
}
