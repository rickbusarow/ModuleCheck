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

@file:Suppress("DEPRECATION", "ForbiddenImport")

package modulecheck.gradle.platforms.android.sourcesets

import com.android.build.gradle.BaseExtension
import com.android.build.gradle.TestedExtension
import com.android.build.gradle.api.BaseVariant
import com.android.build.gradle.api.TestVariant
import com.android.build.gradle.api.UnitTestVariant
import com.android.build.gradle.internal.api.DefaultAndroidSourceSet
import com.squareup.anvil.annotations.ContributesBinding
import modulecheck.dagger.AppScope
import modulecheck.gradle.platforms.ConfigurationFileResolver
import modulecheck.gradle.platforms.KotlinEnvironmentFactory
import modulecheck.gradle.platforms.android.AndroidAppExtension
import modulecheck.gradle.platforms.android.AndroidLibraryExtension
import modulecheck.gradle.platforms.android.AndroidTestExtension
import modulecheck.gradle.platforms.android.sourcesets.internal.GradleSourceSetName
import modulecheck.gradle.platforms.android.sourcesets.internal.GradleSourceSetName.BuildTypeName
import modulecheck.gradle.platforms.android.sourcesets.internal.GradleSourceSetName.ConcatenatedFlavorsName
import modulecheck.gradle.platforms.android.sourcesets.internal.GradleSourceSetName.VariantName
import modulecheck.gradle.platforms.android.sourcesets.internal.ParsedNames
import modulecheck.gradle.platforms.sourcesets.AndroidSourceSetsParser
import modulecheck.gradle.platforms.sourcesets.jvmTarget
import modulecheck.gradle.platforms.sourcesets.kotlinLanguageVersionOrNull
import modulecheck.parsing.gradle.model.Configurations
import modulecheck.parsing.gradle.model.ProjectPath.StringProjectPath
import modulecheck.parsing.gradle.model.SourceSet
import modulecheck.parsing.gradle.model.SourceSetName
import modulecheck.parsing.gradle.model.SourceSets
import modulecheck.parsing.gradle.model.asConfigurationName
import modulecheck.parsing.gradle.model.asSourceSetName
import modulecheck.parsing.gradle.model.distinctSourceSetNames
import modulecheck.parsing.gradle.model.names
import modulecheck.parsing.gradle.model.removePrefix
import modulecheck.utils.capitalize
import modulecheck.utils.decapitalize
import modulecheck.utils.flatMapToSet
import modulecheck.utils.lazy.lazyDeferred
import modulecheck.utils.mapToSet
import org.gradle.api.DomainObjectSet
import org.gradle.api.artifacts.ExternalModuleDependency
import java.io.File
import javax.inject.Inject
import org.gradle.api.Project as GradleProject

/**
 * Given this Android config block:
 *
 * ```
 * android {
 *   buildTypes {
 *     register("internalRelease").configure {
 *       isMinifyEnabled = true
 *       isShrinkResources = true
 *       matchingFallbacks.add("release")
 *     }
 *   }
 *   flavorDimensions("shade", "color")
 *   productFlavors {
 *     create("light") { dimension = "shade" }
 *     create("dark") { dimension = "shade" }
 *     create("red") { dimension = "color" }
 *     create("blue") { dimension = "color" }
 *   }
 * }
 * ```
 *
 * `BuildType` names: [[debug, internalRelease, release]]
 *
 * Primitive flavor names: [[light, dark, red, blue]]
 *
 * Combined flavor names: [[lightRed, lightBlue, darkRed, darkBlue]]
 *
 * Flavor dimensions are just arbitrary keys which allow us to group flavors together. These names
 * are not used to create SourceSets. The final collection of SourceSets would be unaffected if
 * these were just named something like [["a", "b"]].
 *
 * ```
 *   Flavor dimensions (these do not become SourceSets): [[shade, color]]
 *   Product flavors: { color: [[blue, red]], shade: [[light, dark]] }
 * ```
 *
 * Flavors get combined via matrix multiplication and string concatenation in order to create more
 * SourceSets. The order of the concatenated string components ("light", "red", etc.) is determined
 * by the order in which their corresponding flavor dimensions are added. In this example, since
 * "shade" is added before "color", then the flavors of "shade" ("light", "dark") will be before the
 * flavors of "color" ("red", "blue").
 *
 * ```
 * [light, dark] x [red, blue] = [lightRed, lightBlue, darkRed, darkBlue]
 * ```
 *
 * Build Variants are now created via more matrix multiplication and string concatenation, between
 * the `BuildType` and combined flavor names. **BuildType names are always last.**
 *
 * ```
 * [lightRed, lightBlue, darkRed, darkBlue] x [debug, internalRelease, release] =
 *   [
 *     lightRedDebug, lightRedRelease, lightRedInternalRelease,
 *     lightBlueDebug, lightBlueRelease, lightBlueInternalRelease,
 *     darkRedDebug,  darkRedRelease, darkRedInternalRelease,
 *     darkBlueDebug, darkBlueRelease, darkBlueInternalRelease
 *   ]
 * ```
 *
 * Finally, a "main" SourceSet is always created.
 *
 * So just within the *production code* sources, we get all these SourceSets:
 *
 * ```
 * // primitives
 * main
 * light        dark
 * red          blue
 * debug        internalRelease     release
 *
 * // flavor combinations
 * lightRed     darkRed       lightBlue     darkBlue
 *
 * // flavor combinations with build types
 * lightRedDebug     lightRedInternalRelease    lightRedRelease
 * darkRedDebug      darkRedInternalRelease     darkRedRelease
 * lightBlueDebug    lightBlueInternalRelease   lightBlueRelease
 * darkBlueDebug     darkBlueInternalRelease    darkBlueRelease
 * ```
 */
class RealAndroidSourceSetsParser private constructor(
  private val parsedConfigurations: Configurations,
  private val extension: BaseExtension,
  private val hasTestFixturesPlugin: Boolean,
  private val gradleProject: GradleProject,
  private val kotlinEnvironmentFactory: KotlinEnvironmentFactory,
  private val workerFacade: ConfigurationFileResolver
) : AndroidSourceSetsParser {

  val projectPath = StringProjectPath(gradleProject.path)

  private val gradleAndroidSourceSets by lazy {
    extension.sourceSets
      .filterIsInstance<DefaultAndroidSourceSet>()
      .associateBy { it.name }
  }

  private val buildTypeNames by lazy {
    extension.buildTypes.map {
      BuildTypeName(it.name)
    }
  }
  private val flavorDimensions by lazy {
    extension.flavorDimensionList
      .ifEmpty { listOf("default_flavor_dimension") }
  }
  private val productFlavors2D by lazy {
    val mapped = extension.productFlavors
      .groupBy { it.dimension ?: "default_flavor_dimension" }
      .mapValues { (_, productFlavors) ->
        productFlavors.map { GradleSourceSetName.FlavorName(it.name) }
      }

    flavorDimensions.map { mapped.getValue(it) }
  }

  private val variantMap by lazy {

    val tested = (extension as? TestedExtension)
      ?.let { it.testVariants + it.unitTestVariants }
      .orEmpty()

    extension.publishedVariants()
      .plus(tested)
      .associateBy { GradleSourceSetName.VariantName(it.name) }
  }

  private val sourceSetNameToUpstreamMap = buildMap<String, List<GradleSourceSetName>> {

    put(GradleSourceSetName.MainName, listOf())
    // `test` and `androidTest` source sets automatically target the `debug` build type,
    // which means they get `debug` and `main` source sets automatically
    put(
      GradleSourceSetName.AndroidTestName,
      listOf(GradleSourceSetName.MainName, BuildTypeName.DEBUG)
    )
    put(GradleSourceSetName.UnitTestName, listOf(GradleSourceSetName.MainName, BuildTypeName.DEBUG))

    extension.publishedVariants()
      .map { GradleSourceSetName.VariantName(it.name) }
      .forEach { variantName ->

        val parsed = variantName.parseNames(null)

        saveNameHierarchy(parsed)
      }

    if (extension is TestedExtension) {

      extension.testVariants
        .map { GradleSourceSetName.VariantName(it.nameWithoutAndroidTestSuffix()) }
        .forEach { variantName ->

          val parsed = variantName.parseNames(GradleSourceSetName.AndroidTestName)

          saveNameHierarchy(parsed)
        }

      extension.unitTestVariants
        .map { GradleSourceSetName.VariantName(it.nameWithoutUnitTestSuffix()) }
        .forEach { variantName ->

          val parsed = variantName.parseNames(GradleSourceSetName.UnitTestName)

          saveNameHierarchy(parsed)
        }
    }
  }

  private val sourceSetCache = mutableMapOf<SourceSetName, SourceSet>()

  override fun parse(): SourceSets {

    val m = gradleAndroidSourceSets.values
      .mapNotNull { it.toSourceSetOrNull() }
      .associateBy { it.name }
      .toMutableMap()
      .maybeAddTestFixturesSourceSets()

    return SourceSets(m)
  }

  private fun GradleSourceSetName.VariantName.parseNames(
    testTypeOrNull: GradleSourceSetName.TestType?
  ): ParsedNames {

    if (extension.productFlavors.isEmpty()) {
      return ParsedNames(
        variantName = this,
        concatenatedFlavorsName = null,
        buildTypeName = BuildTypeName(value),
        flavors = emptyList(),
        testTypeOrNull = testTypeOrNull
      )
    }

    val (concatenatedFlavorsName, buildTypeName) = splitFlavorAndBuildType()

    val flavors = concatenatedFlavorsName.upstreamFlavors()

    return ParsedNames(
      variantName = this,
      concatenatedFlavorsName = concatenatedFlavorsName,
      buildTypeName = buildTypeName,
      flavors = flavors,
      testTypeOrNull = testTypeOrNull
    )
  }

  private fun MutableMap<String, List<GradleSourceSetName>>.put(
    key: GradleSourceSetName,
    values: List<GradleSourceSetName>
  ) {
    if (key is GradleSourceSetName.TestSourceName<*>) {
      putIfAbsent(key.value, (values + key.published).filterNot { it.value == key.value })
    } else {
      putIfAbsent(key.value, values.filterNot { it.value == key.value })
    }
  }

  private fun MutableMap<String, List<GradleSourceSetName>>.saveNameHierarchy(
    parsedNames: ParsedNames
  ) {

    val (
      variantName,
      concatenatedFlavorsName,
      buildTypeName,
      flavors,
      testTypeOrNull
    ) = parsedNames

    // either [main], [androidTest, main], or [test, main]
    val commonTypes = listOfNotNull(testTypeOrNull, GradleSourceSetName.MainName)

    val maybeTestVariant = testTypeOrNull
      ?.let { GradleSourceSetName.TestSourceName(testTypeOrNull, variantName) }
      ?: variantName

    val maybeTestConcatenatedFlavors = concatenatedFlavorsName?.let {
      testTypeOrNull
        ?.let { GradleSourceSetName.TestSourceName(testTypeOrNull, concatenatedFlavorsName) }
        ?: concatenatedFlavorsName
    }

    val maybeTestFlavors = testTypeOrNull
      ?.let {
        flavors
          .map { flavorName -> GradleSourceSetName.TestSourceName(testTypeOrNull, flavorName) }
          .plus(flavors)
      }
      ?: flavors

    val maybeTestBuildType = testTypeOrNull
      ?.let { GradleSourceSetName.TestSourceName(testTypeOrNull, buildTypeName) }
      ?: buildTypeName

    if (maybeTestConcatenatedFlavors != null) {
      put(maybeTestConcatenatedFlavors, maybeTestFlavors + commonTypes)
    }
    put(maybeTestBuildType, commonTypes)

    val concatenatedUpstream = concatenatedFlavorsName?.let {
      getValue(concatenatedFlavorsName.value)
    }.orEmpty()

    val upstreamOfVariant = maybeTestFlavors
      .asSequence()
      .plus(maybeTestConcatenatedFlavors)
      .plus(concatenatedUpstream)
      .plus(maybeTestBuildType)
      .plus(buildTypeName)
      .plus(GradleSourceSetName.MainName)
      .distinct()
      .toList()
      .filterNotNull()

    put(maybeTestVariant, upstreamOfVariant)

    maybeTestFlavors.forEach { flavor ->
      put(flavor, commonTypes)
    }
  }

  private fun DefaultAndroidSourceSet.toSourceSetOrNull(): SourceSet? {

    if (!sourceSetNameToUpstreamMap.containsKey(name)) return null

    val sourceSetName = name.asSourceSetName()

    return sourceSetCache.getOrPut(sourceSetName) {

      val jvmFiles = javaDirectories
        .plus(kotlinDirectories)
        .flatMapToSet { it.walkTopDown().toList() }

      val resourceFiles = resDirectories
        .flatMap { it.walkTopDown() }
        .toSet()

      val layoutFiles = resourceFiles
        .filter {
          it.isFile && it.path
            // replace `\` from Windows paths with `/`.
            .replace(File.separator, "/")
            .contains("""/res/layout.*/.*.xml""".toRegex())
        }
        .toSet()

      val namesMap = sourceSetNameToUpstreamMap

      val upstreamNames = namesMap.getValue(sourceSetName.value)

      val upstreamLazy = lazy {

        upstreamNames
          .flatMap { upstreamName ->
            sourceSetNameToUpstreamMap.getValue(upstreamName.value)
              .plus(upstreamName)
              .filter { gradleAndroidSourceSets.containsKey(it.value) }
          }
          .distinct()
          .map { it.value.asSourceSetName() }
      }

      val downstreamLazy = lazy {
        namesMap.entries
          .filter { (_, upstream) ->
            upstream.any { it.value == sourceSetName.value }
          }
          .mapToSet { it.key }
          .distinct()
          .filter { gradleAndroidSourceSets.containsKey(it) }
          .map { it.asSourceSetName() }
      }

      val classpath = upstreamLazy.value
        .asSequence()
        .map { it.value }
        .plus(name)
        .mapNotNull { variantMap[VariantName(it)] }
        .flatMapToSet { variant ->
          sequenceOf(
            variant.compileConfiguration,
            variant.runtimeConfiguration
          )
            .flatMap { config ->
              config.files { dependency -> dependency is ExternalModuleDependency }
            }
            .filter { it.exists() }
            .toSet()
        }

      val kotlinEnvironmentDeferred = lazyDeferred {
        kotlinEnvironmentFactory.create(
          projectPath = projectPath,
          sourceSetName = sourceSetName,
          classpathFiles = lazy { classpath },
          sourceDirs = jvmFiles,
          kotlinLanguageVersion = gradleProject.kotlinLanguageVersionOrNull(),
          jvmTarget = gradleProject.jvmTarget()
        )
      }

      SourceSet(
        name = sourceSetName,
        compileOnlyConfiguration = parsedConfigurations
          .getValue(compileOnlyConfigurationName.asConfigurationName()),
        apiConfiguration = parsedConfigurations[apiConfigurationName.asConfigurationName()],
        implementationConfiguration = parsedConfigurations
          .getValue(implementationConfigurationName.asConfigurationName()),
        runtimeOnlyConfiguration = parsedConfigurations
          .getValue(runtimeOnlyConfigurationName.asConfigurationName()),
        annotationProcessorConfiguration = parsedConfigurations
          .getValue(annotationProcessorConfigurationName.asConfigurationName()),
        jvmFiles = jvmFiles,
        resourceFiles = resourceFiles,
        layoutFiles = layoutFiles,
        jvmTarget = gradleProject.jvmTarget(),
        kotlinEnvironmentDeferred = kotlinEnvironmentDeferred,
        upstreamLazy = upstreamLazy,
        downstreamLazy = downstreamLazy
      )
    }
  }

  private fun ConcatenatedFlavorsName.upstreamFlavors(): List<GradleSourceSetName.FlavorName> {
    val upstreamNames = mutableListOf<GradleSourceSetName.FlavorName>()

    var runningName = value

    productFlavors2D.forEachIndexed { index, flavors ->

      fun String.expectedCapitalization() = if (index == 0) {
        this
      } else {
        capitalize()
      }

      val match = flavors
        .sortedByDescending { it.value.length }
        .first { runningName.startsWith(it.value.expectedCapitalization()) }

      upstreamNames.add(match)

      runningName = runningName.removePrefix(match.value.expectedCapitalization())
    }

    return upstreamNames.distinct()
  }

  private fun BaseExtension.publishedVariants(): DomainObjectSet<out BaseVariant> =
    when (this) {
      is AndroidAppExtension -> applicationVariants
      is AndroidLibraryExtension -> libraryVariants
      is AndroidTestExtension -> applicationVariants
      else -> error(
        "Expected the extension to be `AppExtension`, `LibraryExtension`, or `TestExtension`, " +
          "but it was `${this::class.qualifiedName}`."
      )
    }

  private fun GradleSourceSetName.VariantName.splitFlavorAndBuildType(): Pair<ConcatenatedFlavorsName, BuildTypeName> {
    buildTypeNames
      // Sort descending by length because there may be multiple build types with the same
      // suffix, like ["internalRelease", "Release"].  In that example, it's important that
      // "internalRelease" is removed first, because otherwise a source set name of
      // "fooInternalRelease" would be parsed as "fooInternal", which isn't valid.
      .sortedByDescending { it.value.length }
      .forEach { buildTypeName ->

        val buildTypeCapitalized = buildTypeName.value.capitalize()

        if (value.endsWith(buildTypeCapitalized)) {
          val withoutBuildType = value.removeSuffix(buildTypeCapitalized)
          return ConcatenatedFlavorsName(withoutBuildType) to buildTypeName
        }
      }
    error(
      """Unable to find a matching build type name from the provided build variant name.
      |   build variant name --- $value
      |   possible build types - ${buildTypeNames.map { it.value }}
      """.trimMargin()
    )
  }

  /*
  TestFixtures aren't actually defined completely in Gradle.  The source sets and their
  configurations are defined.  The configurations have a bit of a hierarchy, but they don't leave
  the scope of the java-test-fixtures plugin.  Nothing extends "main" source stuff.

  So, for each testFixtures source set name, remove the `testFixtures-` prefix in order to determine
  its upstream source set, then look up that source set and add its upstream sets to the
  corresponding testFixtures one.
   */
  private fun MutableMap<SourceSetName, SourceSet>.maybeAddTestFixturesSourceSets() = apply {
    // The testFixtures source sets are defined regardless of whether the testFixtures feature is
    // actually enabled.  If it isn't enabled, don't add the Gradle source sets to our types.
    if (!hasTestFixturesPlugin) return@apply

    gradleAndroidSourceSets.values
      .filter { it.name.contains(SourceSetName.TEST_FIXTURES.value) }
      .forEach { androidSourceSet ->

        val sourceSetName = androidSourceSet.name.asSourceSetName()

        val configs = listOf(
          androidSourceSet.compileOnlyConfigurationName,
          androidSourceSet.apiConfigurationName,
          androidSourceSet.implementationConfigurationName,
          androidSourceSet.runtimeOnlyConfigurationName
        ).mapNotNull { parsedConfigurations[it.asConfigurationName()] }

        val upstreamLazy = lazy {
          val upstream = androidSourceSet.name.removeTestFixturesPrefix()
            .let { this.getValue(it) }
            .withUpstream()
            .plus(SourceSetName.MAIN)

          configs
            .names()
            .distinctSourceSetNames()
            .filter { containsKey(it) }
            .plus(upstream)
            .filterNot { it == sourceSetName }
            .distinct()
        }

        val downstreamLazy = lazy {
          configs
            .names()
            .distinctSourceSetNames()
            .mapNotNull { this[it] }
            .names()
            .filterNot { it == sourceSetName }
            .distinct()
        }

        val jvmFiles = androidSourceSet.javaDirectories
          .plus(androidSourceSet.kotlinDirectories)
          .flatMapToSet { it.walkTopDown().toList() }

        val resourceFiles = androidSourceSet.resDirectories
          .flatMap { it.walkTopDown() }
          .toSet()

        val layoutFiles = resourceFiles
          .filter {
            it.isFile && it.path
              // replace `\` from Windows paths with `/`.
              .replace(File.separator, "/")
              .contains("""/res/layout.*/.*.xml""".toRegex())
          }
          .toSet()

        val kotlinEnvironmentDeferred = lazyDeferred {
          kotlinEnvironmentFactory.create(
            projectPath = projectPath,
            sourceSetName = sourceSetName,
            classpathFiles = lazy { extension.bootClasspath.toSet() },
            sourceDirs = jvmFiles,
            kotlinLanguageVersion = gradleProject.kotlinLanguageVersionOrNull(),
            jvmTarget = gradleProject.jvmTarget()
          )
        }

        put(
          sourceSetName,
          SourceSet(
            name = sourceSetName,
            compileOnlyConfiguration = parsedConfigurations
              .getValue(androidSourceSet.compileOnlyConfigurationName.asConfigurationName()),
            apiConfiguration = parsedConfigurations
              .get(androidSourceSet.apiConfigurationName.asConfigurationName()),
            implementationConfiguration = parsedConfigurations
              .getValue(androidSourceSet.implementationConfigurationName.asConfigurationName()),
            runtimeOnlyConfiguration = parsedConfigurations
              .getValue(androidSourceSet.runtimeOnlyConfigurationName.asConfigurationName()),
            annotationProcessorConfiguration = parsedConfigurations
              .getValue(androidSourceSet.annotationProcessorConfigurationName.asConfigurationName()),
            jvmFiles = jvmFiles,
            resourceFiles = resourceFiles,
            layoutFiles = layoutFiles,
            jvmTarget = gradleProject.jvmTarget(),
            kotlinEnvironmentDeferred = kotlinEnvironmentDeferred,
            upstreamLazy = upstreamLazy,
            downstreamLazy = downstreamLazy
          )
        )
      }
  }

  /**
   * This removes the `-AndroidTest` suffix from **variant** names. SourceSet names don't get this
   * suffix
   */
  @Suppress("DEPRECATION")
  fun TestVariant.nameWithoutAndroidTestSuffix(): String {
    return name.removeSuffix("AndroidTest")
  }

  fun String.removeAndroidTestPrefix(): SourceSetName {
    return asSourceSetName().removePrefix(SourceSetName.ANDROID_TEST)
  }

  /**
   * This removes the `-UnitTest` suffix from **variant** names. SourceSet names don't get this
   * suffix
   */
  @Suppress("DEPRECATION")
  fun UnitTestVariant.nameWithoutUnitTestSuffix(): String {
    return name.removeSuffix("UnitTest")
  }

  fun String.removeTestPrefix(): String {
    return removePrefix(SourceSetName.TEST.value).decapitalize()
  }

  fun String.removeTestFixturesPrefix(): SourceSetName {
    return asSourceSetName().removePrefix(SourceSetName.TEST_FIXTURES)
      .takeIf { it.value.isNotBlank() }
      ?: SourceSetName.MAIN
  }

  @ContributesBinding(AppScope::class)
  class Factory @Inject constructor(
    private val kotlinEnvironmentFactory: KotlinEnvironmentFactory,
    private val workerFacade: ConfigurationFileResolver
  ) : AndroidSourceSetsParser.Factory {
    override fun create(
      parsedConfigurations: Configurations,
      extension: BaseExtension,
      hasTestFixturesPlugin: Boolean,
      gradleProject: GradleProject
    ): RealAndroidSourceSetsParser {
      return RealAndroidSourceSetsParser(
        parsedConfigurations = parsedConfigurations,
        extension = extension,
        hasTestFixturesPlugin = hasTestFixturesPlugin,
        gradleProject = gradleProject,
        kotlinEnvironmentFactory = kotlinEnvironmentFactory,
        workerFacade = workerFacade
      )
    }
  }
}
