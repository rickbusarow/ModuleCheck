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

package modulecheck.gradle

import com.android.build.api.dsl.ApplicationExtension
import com.android.build.api.dsl.CommonExtension
import com.android.build.api.dsl.DynamicFeatureExtension
import com.android.build.api.dsl.TestExtension
import com.android.build.gradle.LibraryExtension
import modulecheck.core.rule.KOTLIN_ANDROID_EXTENSIONS_PLUGIN_ID
import modulecheck.gradle.AndroidPlatformPluginFactory.Type.Application
import modulecheck.gradle.AndroidPlatformPluginFactory.Type.DynamicFeature
import modulecheck.gradle.AndroidPlatformPluginFactory.Type.Library
import modulecheck.gradle.AndroidPlatformPluginFactory.Type.Test
import modulecheck.gradle.internal.androidManifests
import modulecheck.gradle.internal.orPropertyDefault
import modulecheck.parsing.gradle.AndroidPlatformPlugin
import modulecheck.parsing.gradle.AndroidPlatformPlugin.AndroidApplicationPlugin
import modulecheck.parsing.gradle.AndroidPlatformPlugin.AndroidDynamicFeaturePlugin
import modulecheck.parsing.gradle.AndroidPlatformPlugin.AndroidLibraryPlugin
import modulecheck.parsing.gradle.AndroidPlatformPlugin.AndroidTestPlugin
import javax.inject.Inject

typealias AndroidCommonExtension = CommonExtension<*, *, *, *>

class AndroidPlatformPluginFactory @Inject constructor(
  private val configurationsFactory: ConfigurationsFactory,
  private val sourceSetsFactory: SourceSetsFactory
) {

  fun create(
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

    val manifests = gradleProject.androidManifests().orEmpty()

    val hasKotlinAndroidExtensions = gradleProject
      .pluginManager
      .hasPlugin(KOTLIN_ANDROID_EXTENSIONS_PLUGIN_ID)

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
        "android.library.defaults.buildfeatures.androidresources", true
      )

    return when (type) {
      is Application -> AndroidApplicationPlugin(
        sourceSets = sourceSets,
        configurations = configurations,
        nonTransientRClass = nonTransientRClass,
        viewBindingEnabled = viewBindingEnabled,
        kotlinAndroidExtensionEnabled = hasKotlinAndroidExtensions,
        manifests = manifests
      )
      is DynamicFeature -> AndroidDynamicFeaturePlugin(
        sourceSets = sourceSets,
        configurations = configurations,
        nonTransientRClass = nonTransientRClass,
        viewBindingEnabled = viewBindingEnabled,
        kotlinAndroidExtensionEnabled = hasKotlinAndroidExtensions,
        manifests = manifests,
        buildConfigEnabled = buildConfigEnabled
      )
      is Library -> AndroidLibraryPlugin(
        sourceSets = sourceSets,
        configurations = configurations,
        nonTransientRClass = nonTransientRClass,
        viewBindingEnabled = viewBindingEnabled,
        kotlinAndroidExtensionEnabled = hasKotlinAndroidExtensions,
        manifests = manifests,
        androidResourcesEnabled = androidResourcesEnabled,
        buildConfigEnabled = buildConfigEnabled
      )
      is Test -> AndroidTestPlugin(
        sourceSets = sourceSets,
        configurations = configurations,
        nonTransientRClass = nonTransientRClass,
        viewBindingEnabled = viewBindingEnabled,
        kotlinAndroidExtensionEnabled = hasKotlinAndroidExtensions,
        manifests = manifests,
        buildConfigEnabled = buildConfigEnabled
      )
    }
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
