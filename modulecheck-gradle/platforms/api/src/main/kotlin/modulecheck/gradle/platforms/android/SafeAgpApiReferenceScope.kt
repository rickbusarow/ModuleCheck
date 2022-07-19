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

package modulecheck.gradle.platforms.android

import modulecheck.gradle.platforms.sourcesets.AndroidSourceSetsParser
import modulecheck.parsing.gradle.model.AndroidPlatformPlugin
import modulecheck.parsing.gradle.model.Configurations
import modulecheck.parsing.gradle.model.GradleProject
import net.swiftzer.semver.SemVer

/**
 * Wrapper for accessing AGP declarations only after it's been established that they exist in the
 * classpath.
 *
 * @see AgpApiAccess
 * @see UnsafeDirectAgpApiReference
 */
@OptIn(UnsafeDirectAgpApiReference::class)
class SafeAgpApiReferenceScope @PublishedApi internal constructor(
  private val agpApiAccess: AgpApiAccess,
  private val gradleProject: GradleProject
) {

  /** Helper function for `this is AndroidAppExtension` which bypasses the opt-in requirement. */
  fun Any?.isAndroidAppExtension() = this is AndroidAppExtension

  /** Helper function for `this is AndroidBaseExtension` which bypasses the opt-in requirement. */
  fun Any?.isAndroidBaseExtension() = this is AndroidBaseExtension

  /**
   * Helper function for `this is AndroidCommonExtension` which bypasses the opt-in requirement.
   */
  fun Any?.isAndroidCommonExtension() = this is AndroidCommonExtension

  /**
   * Helper function for `this is AndroidLibraryExtension` which bypasses the opt-in requirement.
   */
  fun Any?.isAndroidLibraryExtension() = this is AndroidLibraryExtension

  /**
   * Helper function for `this is AndroidTestedExtension` which bypasses the opt-in requirement.
   */
  fun Any?.isAndroidTestedExtension() = this is AndroidTestedExtension

  /**
   * Direct access to the AGP [com.android.build.api.dsl.CommonExtension] type, only accessible
   * after it's been established that the type is in the classpath.
   */
  fun requireCommonExtension(): AndroidCommonExtension =
    gradleProject.extensions
      .getByType(com.android.build.api.dsl.CommonExtension::class.java)

  /**
   * Direct access to the AGP [AndroidBaseExtension] type, only accessible after it's been
   * established that the type is in the classpath.
   */
  fun requireBaseExtension(): AndroidBaseExtension =
    gradleProject.extensions
      .getByType(AndroidBaseExtension::class.java)

  private fun hasAgpTestFixtures(): Boolean = gradleProject.extensions
    .findByType(AndroidTestedExtension::class.java)
    ?.takeIf {
      val agpVersion = agpApiAccess.agpVersionOrNull ?: return@takeIf false
      // minimum API version which actually contains the testFixtures property is 7.1.0
      agpVersion >= SemVer.parse("7.1.0")
    }
    ?.let { extension ->
      @Suppress("UnstableApiUsage")
      extension.testFixtures.enable
    } ?: false

  /** @return A new [AndroidPlatformPlugin] using this scope's [gradleProject] */
  fun AndroidPlatformPluginFactory.create(
    hasTestFixturesPlugin: Boolean
  ): AndroidPlatformPlugin {
    return create(
      gradleProject = gradleProject,
      androidCommonExtension = requireCommonExtension(),
      hasTestFixturesPlugin = hasTestFixturesPlugin || hasAgpTestFixtures()
    )
  }

  /** @return A new [AndroidSourceSetsParser] using this scope's [gradleProject] */
  fun AndroidSourceSetsParser.Factory.create(
    mcConfigurations: Configurations,
    hasTestFixturesPlugin: Boolean
  ): AndroidSourceSetsParser {
    return create(
      parsedConfigurations = mcConfigurations,
      extension = requireBaseExtension(),
      hasTestFixturesPlugin = hasTestFixturesPlugin || hasAgpTestFixtures(),
      gradleProject = gradleProject
    )
  }
}
