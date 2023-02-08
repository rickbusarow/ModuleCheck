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

@file:Suppress("ForbiddenImport")

package modulecheck.gradle.platforms.android

import com.android.build.api.dsl.ApplicationExtension
import com.android.build.api.dsl.DynamicFeatureExtension
import com.android.build.api.dsl.TestExtension
import com.android.build.gradle.AppExtension
import com.android.build.gradle.LibraryExtension
import com.android.build.gradle.internal.api.ApplicationVariantImpl
import com.android.build.gradle.internal.api.LibraryVariantImpl
import com.android.build.gradle.internal.core.InternalBaseVariant.MergedFlavor
import com.squareup.anvil.annotations.ContributesBinding
import modulecheck.dagger.TaskScope
import modulecheck.gradle.platforms.ConfigurationsFactory
import modulecheck.gradle.platforms.SourceSetsFactory
import modulecheck.gradle.platforms.android.RealAndroidPlatformPluginFactory.Type.Application
import modulecheck.gradle.platforms.android.RealAndroidPlatformPluginFactory.Type.DynamicFeature
import modulecheck.gradle.platforms.android.RealAndroidPlatformPluginFactory.Type.Library
import modulecheck.gradle.platforms.android.RealAndroidPlatformPluginFactory.Type.Test
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
import javax.inject.Inject

@ContributesBinding(TaskScope::class)
class RealAndroidPlatformPluginFactory @Inject constructor(
  private val agpApiAccess: AgpApiAccess,
  private val configurationsFactory: ConfigurationsFactory,
  private val sourceSetsFactory: SourceSetsFactory
) : AndroidPlatformPluginFactory {

  @UnsafeDirectAgpApiReference
  override fun create(
    gradleProject: GradleProject,
    androidCommonExtension: AndroidCommonExtension,
    hasTestFixturesPlugin: Boolean
  ): AndroidPlatformPlugin {

    val type = Type.from(androidCommonExtension)

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

    val nonTransientRClass = gradleProject
      .findProperty("android.nonTransitiveRClass") as? Boolean ?: false

    @Suppress("UnstableApiUsage")
    val buildConfigEnabled = type.extension.buildFeatures.buildConfig
      .orPropertyDefault(gradleProject, "android.defaults.buildfeatures.buildconfig", true)

    @Suppress("UnstableApiUsage")
    val viewBindingEnabled = type.extension.buildFeatures.viewBinding
      .orPropertyDefault(gradleProject, "android.defaults.buildfeatures.viewbinding", false)

    @Suppress("UnstableApiUsage")
    val androidResourcesEnabled = (type.extension as? LibraryExtension)
      ?.buildFeatures
      ?.androidResources
      .orPropertyDefault(
        gradleProject,
        "android.library.defaults.buildfeatures.androidresources",
        true
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
    type: Type<*>
  ): MutableMap<SourceSetName, Set<UnqualifiedAndroidResource>> {
    fun AndroidCommonExtension.mergedFlavors(): List<MergedFlavor> {
      return when (this) {
        is AppExtension -> applicationVariants.map { it.cast<ApplicationVariantImpl>().mergedFlavor }
        is LibraryExtension -> libraryVariants.map { it.cast<LibraryVariantImpl>().mergedFlavor }
        else -> emptyList()
      }
    }

    fun AndroidCommonExtension.buildTypes(): List<com.android.builder.model.BuildType> {
      return when (this) {
        is AppExtension -> applicationVariants.mapNotNull { it.buildType }
        is LibraryExtension -> libraryVariants.mapNotNull { it.buildType }
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

  sealed interface Type<T : AndroidCommonExtension> {
    val extension: T

    data class Library(override val extension: LibraryExtension) : Type<LibraryExtension>
    data class Application(
      override val extension: ApplicationExtension
    ) : Type<ApplicationExtension>

    data class Test(override val extension: TestExtension) : Type<TestExtension>
    data class DynamicFeature(
      override val extension: DynamicFeatureExtension
    ) : Type<DynamicFeatureExtension>

    companion object {
      fun from(extension: AndroidCommonExtension): Type<*> {
        return when (extension) {
          is LibraryExtension -> Library(extension)
          is ApplicationExtension -> Application(extension)
          is TestExtension -> Test(extension)
          is DynamicFeatureExtension -> DynamicFeature(extension)
          else -> error("unrecognized Android extension ${extension::class.java.canonicalName}")
        }
      }
    }
  }
}
