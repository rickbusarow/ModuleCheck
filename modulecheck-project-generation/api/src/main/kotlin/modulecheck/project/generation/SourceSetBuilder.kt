/*
 * Copyright (C) 2021-2024 Rick Busarow
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

package modulecheck.project.generation

import modulecheck.model.dependency.ConfigurationName
import modulecheck.model.dependency.McConfiguration
import modulecheck.model.dependency.McSourceSet
import modulecheck.model.dependency.ProjectPath.StringProjectPath
import modulecheck.model.sourceset.SourceSetName
import modulecheck.model.sourceset.removePrefix
import modulecheck.model.sourceset.removeSuffix
import modulecheck.parsing.kotlin.compiler.impl.DependencyModuleDescriptorAccess
import modulecheck.parsing.kotlin.compiler.impl.RealKotlinEnvironment
import modulecheck.reporting.logging.PrintLogger
import modulecheck.testing.assertions.requireNotNullOrFail
import modulecheck.utils.capitalize
import modulecheck.utils.lazy.ResetManager
import modulecheck.utils.lazy.lazyDeferred
import org.jetbrains.kotlin.config.JvmTarget
import org.jetbrains.kotlin.config.JvmTarget.JVM_11
import org.jetbrains.kotlin.config.LanguageVersion
import org.jetbrains.kotlin.config.LanguageVersion.KOTLIN_1_6
import java.io.File

/**
 * @property name Name of the source set.
 * @property compileOnlyConfiguration Dependencies that
 *   are required to compile the project's source set.
 * @property apiConfiguration Dependencies that are part
 *   of the API of the source set exposed to consumers.
 * @property implementationConfiguration Dependencies that are used internally in the source set.
 * @property runtimeOnlyConfiguration Dependencies that are required
 *   to run the project's source set, but not required to compile it.
 * @property annotationProcessorConfiguration Dependencies that are used for annotation processing.
 * @property jvmFiles The JVM source files of this source set.
 * @property resourceFiles The resource files of this source set.
 * @property layoutFiles The layout files of this source set.
 * @property classpath The classpath required to compile this source set.
 * @property upstream The list of upstream source set names.
 * @property downstream The list of downstream source set names.
 * @property kotlinLanguageVersion The version of Kotlin used to compile this source set.
 * @property jvmTarget The target version of the generated JVM bytecode.
 */
data class SourceSetBuilder(
  var name: SourceSetName,
  var compileOnlyConfiguration: McConfiguration,
  var apiConfiguration: McConfiguration?,
  var implementationConfiguration: McConfiguration,
  var runtimeOnlyConfiguration: McConfiguration,
  var annotationProcessorConfiguration: McConfiguration?,
  var jvmFiles: Set<File>,
  var resourceFiles: Set<File>,
  var layoutFiles: Set<File>,
  var classpath: MutableList<File> = mutableListOf(
    File(CharRange::class.java.protectionDomain.codeSource.location.path)
  ),
  val upstream: MutableList<SourceSetName>,
  val downstream: MutableList<SourceSetName>,
  var kotlinLanguageVersion: LanguageVersion? = null,
  var jvmTarget: JvmTarget = JVM_11
) {
  /**
   * Converts a `SourceSetBuilder` to a `McSourceSet`.
   *
   * @param dependencyModuleDescriptorAccess The access point to obtain module descriptors.
   * @param projectPath The path to the project.
   * @return A `McSourceSet` built from this `SourceSetBuilder`.
   */
  fun toSourceSet(
    dependencyModuleDescriptorAccess: DependencyModuleDescriptorAccess,
    projectPath: StringProjectPath
  ): McSourceSet {
    val kotlinEnvironmentDeferred = lazyDeferred {
      RealKotlinEnvironment(
        projectPath = projectPath,
        sourceSetName = name,
        classpathFiles = lazyDeferred { classpath },
        sourceDirs = jvmFiles,
        kotlinLanguageVersion = kotlinLanguageVersion,
        jvmTarget = jvmTarget,
        dependencyModuleDescriptorAccess = dependencyModuleDescriptorAccess,
        logger = PrintLogger(),
        resetManager = ResetManager()
      )
    }

    return McSourceSet(
      name = name,
      compileOnlyConfiguration = compileOnlyConfiguration,
      apiConfiguration = apiConfiguration,
      implementationConfiguration = implementationConfiguration,
      runtimeOnlyConfiguration = runtimeOnlyConfiguration,
      annotationProcessorConfiguration = annotationProcessorConfiguration,
      jvmFiles = jvmFiles,
      resourceFiles = resourceFiles,
      layoutFiles = layoutFiles,
      jvmTarget = jvmTarget,
      kotlinEnvironmentDeferred = kotlinEnvironmentDeferred,
      upstreamLazy = lazy { upstream },
      downstreamLazy = lazy { downstream }
    )
  }

  companion object {
    /**
     * Constructs a `SourceSetBuilder` from a given `McSourceSet`.
     *
     * @param sourceSet The `McSourceSet` to construct from.
     * @return A `SourceSetBuilder` constructed from the provided `McSourceSet`.
     */
    suspend fun fromSourceSet(sourceSet: McSourceSet): SourceSetBuilder {
      val kotlinEnvironment = sourceSet.kotlinEnvironmentDeferred.await() as RealKotlinEnvironment

      return SourceSetBuilder(
        name = sourceSet.name,
        compileOnlyConfiguration = sourceSet.compileOnlyConfiguration,
        apiConfiguration = sourceSet.apiConfiguration,
        implementationConfiguration = sourceSet.implementationConfiguration,
        runtimeOnlyConfiguration = sourceSet.runtimeOnlyConfiguration,
        annotationProcessorConfiguration = sourceSet.annotationProcessorConfiguration,
        jvmFiles = sourceSet.jvmFiles,
        resourceFiles = sourceSet.resourceFiles,
        layoutFiles = sourceSet.layoutFiles,
        classpath = kotlinEnvironment.classpathFiles.await().toMutableList(),
        upstream = sourceSet.upstream.toMutableList(),
        downstream = sourceSet.downstream.toMutableList(),
        kotlinLanguageVersion = kotlinEnvironment.kotlinLanguageVersion,
        jvmTarget = sourceSet.jvmTarget
      )
    }
  }
}

/** Populates the source sets for a project. */
@PublishedApi
internal fun McProjectBuilder<*>.populateSourceSets() {
  platformPlugin
    .configurations
    .keys
    .map { it.toSourceSetName() }
    .distinct()
    .forEach { maybeAddSourceSet(it) }
}

/**
 * Attempts to add a source set to the project, if it does not already exist.
 *
 * @param name The name of the source set.
 * @param jvmFiles The JVM source files for the source set.
 * @param resourceFiles The resource files for the source set.
 * @param layoutFiles The layout files for the source set.
 * @param classpath The classpath required to compile the source set.
 * @param upstreamNames The names of the upstream source sets.
 * @param downstreamNames The names of the downstream source sets.
 * @param jvmTarget The target version of the generated JVM bytecode.
 * @return The `SourceSetBuilder` of the newly added or existing source set.
 */
fun McProjectBuilder<*>.maybeAddSourceSet(
  name: SourceSetName,
  jvmFiles: Set<File> = emptySet(),
  resourceFiles: Set<File> = emptySet(),
  layoutFiles: Set<File> = emptySet(),
  classpath: Set<File> = setOf(
    File(CharRange::class.java.protectionDomain.codeSource.location.path)
  ),
  upstreamNames: List<SourceSetName> = emptyList(),
  downstreamNames: List<SourceSetName> = emptyList(),
  jvmTarget: JvmTarget = JVM_11
): SourceSetBuilder {
  if (name.isTestFixtures()) {
    hasTestFixturesPlugin = true
  }

  val upstream = sequenceOf(
    SourceSetName.MAIN,
    name.removePrefix(SourceSetName.TEST),
    name.removePrefix(SourceSetName.TEST_FIXTURES),
    name.removePrefix(SourceSetName.ANDROID_TEST),
    name.removeSuffix(SourceSetName.DEBUG),
    name.removeSuffix(SourceSetName.RELEASE)
  )
    .filterNot { it in upstreamNames }
    .filterNot { it == name }
    .filter { platformPlugin.sourceSets.containsKey(it) }
    .plus(upstreamNames)
    .distinct()
    .toMutableList()

  val configFactory = platformPlugin.configFactory(
    projectDependencies = projectDependencies,
    externalDependencies = externalDependencies
  )

  val kotlinLanguageVersion = when (platformPlugin) {
    is JavaLibraryPluginBuilder -> null
    else -> KOTLIN_1_6
  }

  val sourceSet = platformPlugin.sourceSets.getOrPut(name) {
    SourceSetBuilder(
      name = name,
      compileOnlyConfiguration = configFactory.create(name.configurationName("compileOnly")),
      apiConfiguration = configFactory.create(name.configurationName("api")),
      implementationConfiguration = configFactory.create(name.configurationName("implementation")),
      runtimeOnlyConfiguration = configFactory.create(name.configurationName("runtimeOnly")),
      annotationProcessorConfiguration = configFactory.create(name.configurationName("kapt")),
      jvmFiles = jvmFiles,
      resourceFiles = resourceFiles,
      layoutFiles = layoutFiles,
      classpath = classpath.toMutableList(),
      upstream = upstream,
      downstream = downstreamNames.toMutableList(),
      kotlinLanguageVersion = kotlinLanguageVersion,
      jvmTarget = jvmTarget
    )
  }
  platformPlugin.populateConfigsFromSourceSets()
  return sourceSet
}

/** Asserts that the hierarchy of source sets is valid. */
@PublishedApi
internal fun MutableMap<SourceSetName, SourceSetBuilder>.validateHierarchy() {
  values.forEach { sourceSet ->
    sourceSet.downstream
      .forEach { requireSourceSetExists(it) }
    sourceSet.upstream
      .forEach { requireSourceSetExists(it) }
  }
}

/**
 * Asserts that a source set with a given name exists.
 *
 * @param name The name of the source set to verify.
 */
internal fun PlatformPluginBuilder<*>.requireSourceSetExists(name: SourceSetName) {
  sourceSets.requireSourceSetExists(name)
}

/**
 * Asserts that a source set with a given name exists.
 *
 * @param name The name of the source set to check.
 */
internal fun MutableMap<SourceSetName, SourceSetBuilder>.requireSourceSetExists(
  name: SourceSetName
) {
  get(name)
    .requireNotNullOrFail {
      """
        The source set named `${name.value}` doesn't exist in the `sourceSets` map.

          missing source set name: ${name.value}
          existing source sets: ${keys.map { it.value }}
      """.trimIndent()
    }
}

/** Finds the downstream source sets for each source set. */
@PublishedApi
internal fun MutableMap<SourceSetName, SourceSetBuilder>.populateDownstreams() {
  values.forEach { sourceSetBuilder ->
    sourceSetBuilder.downstream.clear()
    sourceSetBuilder.downstream.addAll(
      values.filter { it.upstream.contains(sourceSetBuilder.name) }
        .map { it.name }
        .distinct()
    )
  }
}

/**
 * Derives this source set's name for the base [configName].
 *
 * For instance, the "test" source set with a `configName`
 * of "implementation" will return `testImplementation`.
 *
 * @param configName The base name of the configuration.
 * @return The complete configuration name.
 */
internal fun SourceSetName.configurationName(configName: String): String {
  return if (this == SourceSetName.MAIN) {
    configName
  } else if (configName == ConfigurationName.kapt.value) {
    "${configName}${value.capitalize()}"
  } else {
    "${value}${configName.capitalize()}"
  }
}
