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

import modulecheck.model.dependency.ConfigurationName
import modulecheck.model.dependency.McConfiguration
import modulecheck.model.dependency.McSourceSet
import modulecheck.model.dependency.ProjectPath.StringProjectPath
import modulecheck.model.sourceset.SourceSetName
import modulecheck.model.sourceset.removePrefix
import modulecheck.model.sourceset.removeSuffix
import modulecheck.parsing.kotlin.compiler.impl.RealKotlinEnvironment
import modulecheck.parsing.kotlin.compiler.impl.SafeAnalysisResultAccess
import modulecheck.testing.requireNotNullOrFail
import modulecheck.utils.capitalize
import modulecheck.utils.lazy.lazyDeferred
import org.jetbrains.kotlin.config.JvmTarget
import org.jetbrains.kotlin.config.JvmTarget.JVM_11
import org.jetbrains.kotlin.config.LanguageVersion
import org.jetbrains.kotlin.config.LanguageVersion.KOTLIN_1_6
import java.io.File

data class SourceSetBuilder constructor(
  var name: SourceSetName,
  var compileOnlyConfiguration: McConfiguration,
  var apiConfiguration: McConfiguration?,
  var implementationConfiguration: McConfiguration,
  var runtimeOnlyConfiguration: McConfiguration,
  var annotationProcessorConfiguration: McConfiguration?,
  var jvmFiles: Set<File>,
  var resourceFiles: Set<File>,
  var layoutFiles: Set<File>,
  var classpath: MutableSet<File> = mutableSetOf(
    File(CharRange::class.java.protectionDomain.codeSource.location.path)
  ),
  val upstream: MutableList<SourceSetName>,
  val downstream: MutableList<SourceSetName>,
  var kotlinLanguageVersion: LanguageVersion? = null,
  var jvmTarget: JvmTarget = JVM_11
) {
  fun toSourceSet(
    safeAnalysisResultAccess: SafeAnalysisResultAccess,
    projectPath: StringProjectPath
  ): McSourceSet {
    val kotlinEnvironmentDeferred = lazyDeferred {
      RealKotlinEnvironment(
        projectPath = projectPath,
        sourceSetName = name,
        classpathFiles = lazy { classpath },
        sourceDirs = jvmFiles,
        kotlinLanguageVersion = kotlinLanguageVersion,
        jvmTarget = jvmTarget,
        safeAnalysisResultAccess = safeAnalysisResultAccess
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
        classpath = kotlinEnvironment.classpathFiles.value.toMutableSet(),
        upstream = sourceSet.upstream.toMutableList(),
        downstream = sourceSet.downstream.toMutableList(),
        kotlinLanguageVersion = kotlinEnvironment.kotlinLanguageVersion,
        jvmTarget = sourceSet.jvmTarget
      )
    }
  }
}

@PublishedApi
internal fun McProjectBuilder<*>.populateSourceSets() {
  platformPlugin
    .configurations
    .keys
    .map { it.toSourceSetName() }
    .distinct()
    .forEach { maybeAddSourceSet(it) }
}

@Suppress("LongParameterList")
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

  val configFactory = platformPlugin.configFactory

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
      classpath = classpath.toMutableSet(),
      upstream = upstream,
      downstream = downstreamNames.toMutableList(),
      kotlinLanguageVersion = kotlinLanguageVersion,
      jvmTarget = jvmTarget
    )
  }
  platformPlugin.populateConfigsFromSourceSets()
  return sourceSet
}

@PublishedApi
internal fun MutableMap<SourceSetName, SourceSetBuilder>.validateHierarchy() {
  values.forEach { sourceSet ->
    sourceSet.downstream
      .forEach { requireSourceSetExists(it) }
    sourceSet.upstream
      .forEach { requireSourceSetExists(it) }
  }
}

internal fun PlatformPluginBuilder<*>.requireSourceSetExists(name: SourceSetName) {
  sourceSets.requireSourceSetExists(name)
}

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

internal fun SourceSetName.configurationName(configName: String): String {
  return if (this == SourceSetName.MAIN) {
    configName
  } else if (configName == ConfigurationName.kapt.value) {
    "${configName}${value.capitalize()}"
  } else {
    "${value}${configName.capitalize()}"
  }
}
