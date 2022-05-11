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

import modulecheck.parsing.gradle.model.Config
import modulecheck.parsing.gradle.model.SourceSet
import modulecheck.parsing.gradle.model.SourceSetName
import modulecheck.parsing.gradle.model.removePrefix
import modulecheck.parsing.gradle.model.removeSuffix
import modulecheck.testing.requireNotNullOrFail
import modulecheck.utils.capitalize
import java.io.File

data class SourceSetBuilder(
  var name: SourceSetName,
  var compileOnlyConfiguration: Config,
  var apiConfiguration: Config?,
  var implementationConfiguration: Config,
  var runtimeOnlyConfiguration: Config,
  var annotationProcessorConfiguration: Config?,
  var jvmFiles: Set<File>,
  var resourceFiles: Set<File>,
  var layoutFiles: Set<File>,
  val upstream: MutableList<SourceSetName>,
  val downstream: MutableList<SourceSetName>
) {
  fun toSourceSet() = SourceSet(
    name = name,
    compileOnlyConfiguration = compileOnlyConfiguration,
    apiConfiguration = apiConfiguration,
    implementationConfiguration = implementationConfiguration,
    runtimeOnlyConfiguration = runtimeOnlyConfiguration,
    annotationProcessorConfiguration = annotationProcessorConfiguration,
    jvmFiles = jvmFiles,
    resourceFiles = resourceFiles,
    layoutFiles = layoutFiles,
    upstreamLazy = lazy { upstream },
    downstreamLazy = lazy { downstream }
  )

  companion object {
    fun fromSourceSet(sourceSet: SourceSet): SourceSetBuilder {
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
        upstream = sourceSet.upstream.toMutableList(),
        downstream = sourceSet.downstream.toMutableList()
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
  upstreamNames: List<SourceSetName> = emptyList(),
  downstreamNames: List<SourceSetName> = emptyList()
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
      upstream = upstream,
      downstream = downstreamNames.toMutableList()
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
  } else {
    "${value}${configName.capitalize()}"
  }
}
