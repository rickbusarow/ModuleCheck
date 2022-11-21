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

import com.android.build.api.dsl.AndroidSourceSet
import com.android.build.gradle.TestedExtension
import modulecheck.gradle.platforms.sourcesets.AndroidSourceSetsParser
import modulecheck.model.dependency.AndroidPlatformPlugin
import modulecheck.model.dependency.Configurations
import modulecheck.parsing.gradle.model.GradleProject
import net.swiftzer.semver.SemVer
import org.gradle.api.DomainObjectSet

/**
 * Wrapper for accessing AGP declarations only after it's been established that they exist in the
 * classpath.
 *
 * @see AgpApiAccess
 * @see UnsafeDirectAgpApiReference
 * @since 0.12.0
 */
@Suppress("TYPEALIAS_EXPANSION_DEPRECATION")
@OptIn(UnsafeDirectAgpApiReference::class)
open class SafeAgpApiReferenceScope @PublishedApi internal constructor(
  private val agpApiAccess: AgpApiAccess,
  private val gradleProject: GradleProject
) {

  protected constructor(
    gradleProject: GradleProject
  ) : this(AgpApiAccess(), gradleProject)

  /**
   * Helper function for `this is AndroidAppExtension` which bypasses the opt-in requirement.
   *
   * @since 0.12.0
   */
  fun AndroidBaseExtension?.isAndroidAppExtension(): Boolean = this is AndroidAppExtension

  /**
   * Helper function for `this is AndroidCommonExtension` which bypasses the opt-in requirement.
   *
   * @since 0.12.0
   */
  fun Any?.isAndroidCommonExtension(): Boolean = this is AndroidCommonExtension

  /**
   * Helper function for `this is AndroidLibraryExtension` which bypasses the opt-in requirement.
   *
   * @since 0.12.0
   */
  fun Any?.isAndroidLibraryExtension(): Boolean = this is AndroidLibraryExtension

  /**
   * Helper function for `this is AndroidTestedExtension` which bypasses the opt-in requirement.
   *
   * @since 0.12.0
   */
  fun Any?.isAndroidTestedExtension(): Boolean = this is AndroidTestedExtension

  /**
   * Direct access to the AGP [com.android.build.api.dsl.CommonExtension] type, only accessible
   * after it's been established that the type is in the classpath.
   *
   * @since 0.12.0
   */
  fun requireCommonExtension(): AndroidCommonExtension =
    gradleProject.extensions
      .getByType(com.android.build.api.dsl.CommonExtension::class.java)

  /**
   * Direct access to the AGP [AndroidBaseExtension] type, only accessible after it's been
   * established that the type is in the classpath.
   *
   * @since 0.12.0
   */
  fun requireBaseExtension(): AndroidBaseExtension =
    gradleProject.extensions
      .getByType(AndroidBaseExtension::class.java)

  /**
   * @return if this [BaseVariant][com.android.build.gradle.api.BaseVariant] is a
   *     [TestedVariant][com.android.build.gradle.internal.api.TestedVariant], returns its
   *     [testVariant][com.android.build.gradle.internal.api.TestedVariant.testVariant]. Otherwise,
   *     returns null.
   * @since 0.13.0
   */
  fun AndroidBaseVariant.androidTestVariantOrNull(): AndroidTestVariant? {
    return when (this) {
      is AndroidTestedVariant -> testVariant
      else -> null
    }
  }

  /**
   * @return if this [BaseVariant][com.android.build.gradle.api.BaseVariant] is a
   *     [TestedVariant][com.android.build.gradle.internal.api.TestedVariant], returns its
   *     [unitTestVariant][com.android.build.gradle.internal.api.TestedVariant.unitTestVariant].
   *     Otherwise, returns null.
   * @since 0.13.0
   */
  fun AndroidBaseVariant.unitTestVariantOrNull(): AndroidUnitTestVariant? {
    return when (this) {
      is AndroidTestedVariant -> unitTestVariant
      else -> null
    }
  }

  @UnsafeDirectAgpApiReference
  fun AndroidBaseExtension.baseVariants(): DomainObjectSet<out AndroidBaseVariant> =
    when (this) {
      is AndroidAppExtension -> applicationVariants
      is AndroidLibraryExtension -> libraryVariants
      is AndroidTestExtension -> applicationVariants
      else -> TODO()
      // else -> DefaultDomainObjectSet(BaseVariant::class.java, CollectionCallbackActionDecorator.NOOP)
    }

  @UnsafeDirectAgpApiReference
  fun AndroidBaseExtension.testingVariants(): Set<AndroidBaseVariant> =
    when (this) {
      is TestedExtension -> testVariants + unitTestVariants
      else -> emptySet()
    }

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

  /**
   * `api`, `implementation`, `compileOnly`, and `runtimeOnly` configuration names for this source
   * set.
   *
   * Inspired by
   * [KotlinSourceSet.relatedConfigurationNames][org.jetbrains.kotlin.gradle.plugin.KotlinSourceSet.relatedConfigurationNames].
   *
   * @since 0.13.0
   */
  @Suppress("UnstableApiUsage")
  val AndroidSourceSet.relatedConfigurationNames: List<String>
    get() = listOf(
      apiConfigurationName,
      implementationConfigurationName,
      compileOnlyConfigurationName,
      runtimeOnlyConfigurationName
    )

  /**
   * @return A new [AndroidPlatformPlugin] using this scope's [gradleProject]
   * @since 0.12.0
   */
  fun AndroidPlatformPluginFactory.create(
    hasTestFixturesPlugin: Boolean
  ): AndroidPlatformPlugin {
    return create(
      gradleProject = gradleProject,
      androidCommonExtension = requireCommonExtension(),
      hasTestFixturesPlugin = hasTestFixturesPlugin || hasAgpTestFixtures()
    )
  }

  /**
   * @return A new [AndroidSourceSetsParser] using this scope's [gradleProject]
   * @since 0.12.0
   */
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
