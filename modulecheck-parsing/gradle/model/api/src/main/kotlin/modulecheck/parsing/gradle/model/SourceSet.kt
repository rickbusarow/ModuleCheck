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

package modulecheck.parsing.gradle.model

import modulecheck.utils.capitalize
import modulecheck.utils.decapitalize
import java.io.File

data class SourceSet(
  val name: SourceSetName,
  val compileOnlyConfiguration: Config,
  val apiConfiguration: Config?,
  val implementationConfiguration: Config,
  val runtimeOnlyConfiguration: Config,
  val annotationProcessorConfiguration: Config?,
  val jvmFiles: Set<File>,
  val resourceFiles: Set<File>,
  val layoutFiles: Set<File>,
  private val upstreamLazy: Lazy<List<SourceSetName>>,
  private val downstreamLazy: Lazy<List<SourceSetName>>
) : Comparable<SourceSet> {

  val upstream: List<SourceSetName> by lazy { upstreamLazy.value }
  val downstream: List<SourceSetName> by lazy { downstreamLazy.value }

  fun withUpstream() = listOf(name) + upstream
  fun withDownstream() = listOf(name) + downstream

  val hasExistingSourceFiles by lazy {
    jvmFiles.hasExistingFiles() ||
      resourceFiles.hasExistingFiles() ||
      layoutFiles.hasExistingFiles()
  }

  private fun Set<File>.hasExistingFiles(): Boolean {
    return any { dir ->
      dir.walkBottomUp()
        .any { file -> file.isFile && file.exists() }
    }
  }

  /**
   * If one source set extends another, then the extended one should come before it in a collection.
   * For instance, in [withUpstream] for `TestDebug`, the list would be `[main, debug, testDebug]`.
   *
   * If two source sets are siblings (neither extends the other, such as in build flavors from
   * different dimensions), then they should be sorted alphabetically (by name). The alphabetical
   * sort just ensures that all lists are stable.
   */
  override fun compareTo(other: SourceSet): Int {

    if (this == other) return 0

    return when {
      upstream.contains(other.name) -> 1
      other.upstream.contains(name) -> -1
      else -> name.value.compareTo(other.name.value)
    }
  }

  override fun toString(): String {
    return """SourceSet(
        |  name=$name,
        |  compileOnlyConfiguration=$compileOnlyConfiguration,
        |  apiConfiguration=$apiConfiguration,
        |  implementationConfiguration=$implementationConfiguration,
        |  runtimeOnlyConfiguration=$runtimeOnlyConfiguration,
        |  kaptConfiguration=$annotationProcessorConfiguration,
        |  jvmFiles=$jvmFiles,
        |  resourceFiles=$resourceFiles,
        |  layoutFiles=$layoutFiles,
        |  upstreamLazy=${upstreamLazy.value.map { it.value }},
        |  downstreamLazy=${downstreamLazy.value.map { it.value }}
        |)
    """.trimMargin()
  }
}

fun Iterable<SourceSet>.names(): List<SourceSetName> = map { it.name }
fun Sequence<SourceSet>.names(): Sequence<SourceSetName> = map { it.name }

fun Collection<SourceSet>.sortedByInheritance(): Sequence<SourceSet> {

  val pending = sortedBy { it.upstream.size }.toMutableList()
  val history = mutableSetOf<SourceSetName>()

  return generateSequence {

    pending.firstOrNull { pendingSourceSet ->
      // find the first SourceSet where everything upstream has already been yielded to the sequence
      pendingSourceSet.upstream.isEmpty() || pendingSourceSet.upstream.all { history.contains(it) }
    }
      ?.also {
        history.add(it.name)
        pending.remove(it)
      }
  }
}

@JvmInline
value class SourceSetName(val value: String) {

  fun isTestingOnly() = when {
    this.value.startsWith(TEST_FIXTURES.value) -> false
    this.value.startsWith(ANDROID_TEST.value) -> true
    this.value.startsWith(TEST.value) -> true
    else -> false
  }

  fun isTestOrAndroidTest() = when {
    this.value.startsWith(ANDROID_TEST.value, ignoreCase = true) -> true
    this.value.startsWith(TEST.value, ignoreCase = true) -> true
    else -> false
  }

  fun isTestFixtures() = value.startsWith(TEST_FIXTURES.value, ignoreCase = true)

  fun nonTestSourceSetNameOrNull() = when {
    isTestingOnly() -> null
    value.endsWith(ANDROID_TEST.value, ignoreCase = true) -> {
      value.removePrefix(ANDROID_TEST.value).decapitalize().asSourceSetName()
    }
    value.endsWith(TEST.value, ignoreCase = true) -> {
      value.removePrefix(TEST.value).decapitalize().asSourceSetName()
    }
    this == TEST_FIXTURES -> MAIN
    else -> this
  }

  fun javaConfigurationNames(): List<ConfigurationName> {

    return if (this == MAIN) {
      ConfigurationName.main()
    } else {
      ConfigurationName.mainConfigurations
        .map { "${this.value}${it.capitalize()}".asConfigurationName() }
    }
  }

  fun apiConfig(): ConfigurationName {
    return if (this == MAIN) {
      ConfigurationName.api
    } else {
      "${value}Api".asConfigurationName()
    }
  }

  fun implementationConfig(): ConfigurationName {
    return if (this == MAIN) {
      ConfigurationName.implementation
    } else {
      "${value}Implementation".asConfigurationName()
    }
  }

  fun withUpstream(
    hasConfigurations: HasConfigurations
  ): List<SourceSetName> {
    return hasConfigurations.sourceSets[this]
      ?.withUpstream()
      .orEmpty()
  }

  fun withDownStream(
    hasConfigurations: HasConfigurations
  ): List<SourceSetName> {
    return hasConfigurations.sourceSets[this]
      ?.withDownstream()
      .orEmpty()
  }

  fun inheritsFrom(
    other: SourceSetName,
    hasConfigurations: HasConfigurations
  ): Boolean {

    // SourceSets can't inherit from themselves, so quit early and skip some lookups.
    if (this == other) return false

    return hasConfigurations.sourceSets[this]
      ?.upstream
      ?.contains(other)
      ?: false
  }

  override fun toString(): String = "(SourceSetName) `$value`"

  companion object {
    val ANDROID_TEST = SourceSetName("androidTest")
    val ANVIL = SourceSetName("anvil")
    val DEBUG = SourceSetName("debug")
    val KAPT = SourceSetName("kapt")
    val MAIN = SourceSetName("main")
    val RELEASE = SourceSetName("release")
    val TEST = SourceSetName("test")
    val TEST_FIXTURES = SourceSetName("testFixtures")
  }
}

fun String.asSourceSetName(): SourceSetName = SourceSetName(this)

class SourceSets(
  delegate: Map<SourceSetName, SourceSet>
) : Map<SourceSetName, SourceSet> by delegate

fun SourceSetName.removePrefix(prefix: String) = value.removePrefix(prefix)
  .decapitalize()
  .asSourceSetName()

fun SourceSetName.removePrefix(prefix: SourceSetName) = removePrefix(prefix.value)

fun SourceSetName.hasPrefix(prefix: String) = value.startsWith(prefix)
fun SourceSetName.hasPrefix(prefix: SourceSetName) = hasPrefix(prefix.value)

fun SourceSetName.addPrefix(prefix: String) = prefix.plus(value.capitalize())
  .asSourceSetName()

fun SourceSetName.addPrefix(prefix: SourceSetName) = addPrefix(prefix.value)

fun SourceSetName.removeSuffix(suffix: String) = value.removeSuffix(suffix.capitalize())
  .asSourceSetName()

fun SourceSetName.removeSuffix(suffix: SourceSetName) = removeSuffix(suffix.value.capitalize())

fun SourceSetName.addSuffix(suffix: String) = value.plus(suffix.capitalize())
  .asSourceSetName()

fun SourceSetName.addSuffix(suffix: SourceSetName) = addSuffix(suffix.value)
