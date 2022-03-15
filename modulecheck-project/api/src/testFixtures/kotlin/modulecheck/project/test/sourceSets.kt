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

import modulecheck.parsing.gradle.SourceSet
import modulecheck.parsing.gradle.SourceSetName
import modulecheck.parsing.gradle.addPrefix
import modulecheck.parsing.gradle.addSuffix
import modulecheck.parsing.gradle.removePrefix
import modulecheck.parsing.gradle.removeSuffix
import modulecheck.utils.capitalize
import java.io.File

internal fun McProjectBuilderScope.populateSourceSets() {
  configurations
    .keys
    .map { it.toSourceSetName() }
    .distinct()
    .forEach { maybeAddSourceSet(it) }
}

@Suppress("LongParameterList")
fun McProjectBuilderScope.maybeAddSourceSet(
  name: SourceSetName,
  jvmFiles: Set<File> = emptySet(),
  resourceFiles: Set<File> = emptySet(),
  layoutFiles: Set<File> = emptySet(),
  upstreamNames: List<SourceSetName> = emptyList(),
  downstreamNames: List<SourceSetName> = emptyList()
): SourceSet {

  if (name.isTestFixtures()) {
    hasTestFixturesPlugin = true
  }

  val upstreamLazy = lazy {

    upstreamNames.forEach(::requireSourceSetExists)

    sequenceOf(
      SourceSetName.MAIN,
      name.removePrefix(SourceSetName.TEST),
      name.removePrefix(SourceSetName.TEST_FIXTURES),
      name.removePrefix(SourceSetName.ANDROID_TEST),
      name.removeSuffix(SourceSetName.DEBUG),
      name.removeSuffix(SourceSetName.RELEASE)
    )
      .filterNot { it in upstreamNames }
      .filterNot { it == name }
      .filter { sourceSets.containsKey(it) }
      .plus(upstreamNames)
      .distinct()
      .toList()
  }

  val downstreamLazy = lazy {

    downstreamNames.forEach(::requireSourceSetExists)

    sequence {

      val isAndroid = this@maybeAddSourceSet is AndroidMcProjectBuilderScope

      yield(SourceSetName.TEST)
      yield(name.addPrefix(SourceSetName.TEST))

      if (isAndroid) {
        yield(SourceSetName.ANDROID_TEST)
        yield(SourceSetName.DEBUG)
        yield(SourceSetName.RELEASE)

        yield(name.addPrefix(SourceSetName.ANDROID_TEST))
        yield(name.addSuffix(SourceSetName.DEBUG))
        yield(name.addSuffix(SourceSetName.RELEASE))
      }

      if (hasTestFixturesPlugin) {
        yield(SourceSetName.TEST_FIXTURES)
      }

      // this is a hack
      if (name == SourceSetName.MAIN) {
        yieldAll(sourceSets.keys)
      }
    }
      .filterNot { it in downstreamNames }
      .filterNot { it == name }
      .filter { sourceSets.containsKey(it) }
      .plus(downstreamNames)
      .distinct()
      .toList()
  }

  val sourceSet = sourceSets.getOrPut(name) {
    SourceSet(
      name = name,
      compileOnlyConfiguration = configFactory.create(name.configurationName("compileOnly")),
      apiConfiguration = configFactory.create(name.configurationName("api")),
      implementationConfiguration = configFactory.create(name.configurationName("implementation")),
      runtimeOnlyConfiguration = configFactory.create(name.configurationName("runtimeOnly")),
      annotationProcessorConfiguration = configFactory.create(name.configurationName("kapt")),
      jvmFiles = jvmFiles,
      resourceFiles = resourceFiles,
      layoutFiles = layoutFiles,
      upstreamLazy = upstreamLazy,
      downstreamLazy = downstreamLazy
    )
  }
  populateConfigsFromSourceSets()
  return sourceSet
}

internal fun SourceSetName.configurationName(configName: String): String {
  return if (this == SourceSetName.MAIN) {
    configName
  } else {
    "${value}${configName.capitalize()}"
  }
}
