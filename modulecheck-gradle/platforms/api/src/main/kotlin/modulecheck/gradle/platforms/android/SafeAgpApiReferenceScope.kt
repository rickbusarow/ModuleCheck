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

import modulecheck.gradle.platforms.internal.GradleProject
import modulecheck.gradle.platforms.sourcesets.AndroidSourceSetsParser
import modulecheck.model.dependency.AndroidPlatformPlugin
import modulecheck.model.dependency.Configurations
import net.swiftzer.semver.SemVer
import org.gradle.api.DomainObjectSet

/**
 * Wrapper for accessing AGP declarations only after it's
 * been established that they exist in the classpath.
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
  fun AgpBaseExtension?.isAndroidAppExtension(): Boolean = this is AgpAppExtension

  /**
   * Helper function for `this is AndroidCommonExtension` which bypasses the opt-in requirement.
   *
   * @since 0.12.0
   */
  fun Any?.isAndroidCommonExtension(): Boolean = this is AgpCommonExtension

  /**
   * Helper function for `this is AndroidLibraryExtension` which bypasses the opt-in requirement.
   *
   * @since 0.12.0
   */
  fun Any?.isAndroidLibraryExtension(): Boolean = this is AgpLibraryExtension

  /**
   * Helper function for `this is AndroidTestedExtension` which bypasses the opt-in requirement.
   *
   * @since 0.12.0
   */
  fun Any?.isAndroidTestedExtension(): Boolean = this is AgpTestedExtension

  /**
   * Direct access to the AGP [com.android.build.api.dsl.CommonExtension] type,
   * only accessible after it's been established that the type is in the classpath.
   *
   * @since 0.12.0
   */
  fun requireCommonExtension(): AgpCommonExtension = gradleProject.extensions
    .getByType(com.android.build.api.dsl.CommonExtension::class.java)

  /**
   * Direct access to the AGP [AgpBaseExtension] type, only accessible
   * after it's been established that the type is in the classpath.
   *
   * @since 0.12.0
   */
  fun requireBaseExtension(): AgpBaseExtension = gradleProject.extensions
    .getByType(AgpBaseExtension::class.java)

  /**
   * @return if this [BaseVariant][com.android.build.gradle.api.BaseVariant] is a
   *   [TestedVariant][com.android.build.gradle.internal.api.TestedVariant], returns its
   *   [testVariant][com.android.build.gradle.internal.api.TestedVariant.getUnitTestVariant].
   *   Otherwise, returns null.
   * @since 0.13.0
   */
  fun AgpBaseVariant.androidTestVariantOrNull(): AgpTestVariant? {
    return when (this) {
      is AgpTestedVariant -> testVariant
      else -> null
    }
  }

  /**
   * @return if this [BaseVariant][com.android.build.gradle.api.BaseVariant] is a
   *   [TestedVariant][com.android.build.gradle.internal.api.TestedVariant], returns its
   *   [unitTestVariant][com.android.build.gradle.internal.api.TestedVariant.getUnitTestVariant].
   *   Otherwise, returns null.
   * @since 0.13.0
   */
  fun AgpBaseVariant.unitTestVariantOrNull(): AgpUnitTestVariant? {
    return when (this) {
      is AgpTestedVariant -> unitTestVariant
      else -> null
    }
  }

  /**
   * Syntactic sugar for getting application or library
   * variants, since they don't have a common function in AGP
   */
  @UnsafeDirectAgpApiReference
  fun AgpBaseExtension.baseVariants(): DomainObjectSet<out AgpBaseVariant> = when (this) {
    is AgpAppExtension -> applicationVariants
    is AgpLibraryExtension -> libraryVariants
    is AgpTestExtension -> applicationVariants
    else -> TODO()
    // else -> DefaultDomainObjectSet(BaseVariant::class.java, CollectionCallbackActionDecorator.NOOP)
  }

  /** */
  @UnsafeDirectAgpApiReference
  fun AgpBaseExtension.testingVariants(): Set<AgpBaseVariant> = when (this) {
    is AgpTestedExtension -> testVariants + unitTestVariants
    else -> emptySet()
  }

  private fun hasAgpTestFixtures(): Boolean = gradleProject.extensions
    .findByType(AgpTestedExtension::class.java)
    ?.takeIf {
      val agpVersion = agpApiAccess.agpVersionOrNull ?: return@takeIf false
      // minimum API version which actually contains the testFixtures property is 7.1.0
      agpVersion >= SemVer.parse("7.1.0")
    }
    ?.let { extension ->
      @Suppress("UnstableApiUsage")
      extension.testFixtures.enable
    } ?: false

  /** Inspired by [org.jetbrains.kotlin.gradle.plugin.KotlinSourceSet.relatedConfigurationNames]. */
  val AgpSourceSet.relatedConfigurationNames: List<String>
    get() = listOf(
      apiConfigurationName,
      implementationConfigurationName,
      compileOnlyConfigurationName,
      runtimeOnlyConfigurationName,
      "${name}AnnotationProcessorClasspath",
      "${name}CompileClasspath"
    )

  /**
   * @return A new [AndroidPlatformPlugin] using this scope's [gradleProject]
   * @since 0.12.0
   */
  fun AndroidPlatformPluginFactory.create(hasTestFixturesPlugin: Boolean): AndroidPlatformPlugin {
    return create(
      gradleProject = gradleProject,
      agpCommonExtension = requireCommonExtension(),
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
