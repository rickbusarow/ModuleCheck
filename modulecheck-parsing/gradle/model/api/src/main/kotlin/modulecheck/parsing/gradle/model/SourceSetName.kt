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
        .filterNot { it.asConfigurationName().isKapt() }
        .map { "${this.value}${it.capitalize()}".asConfigurationName() }
        .plus(kaptVariant())
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

  /**
   * @return the 'kapt' name for this source set, such as `kapt`, `kaptTest`, or `kaptAndroidTest`
   */
  fun kaptVariant(): ConfigurationName {
    return if (this == MAIN) {
      ConfigurationName.kapt
    } else {
      "${ConfigurationName.kapt.value}${value.capitalize()}".asConfigurationName()
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

  fun removePrefix(prefix: String) = value.removePrefix(prefix).decapitalize().asSourceSetName()
  fun removePrefix(prefix: SourceSetName) = removePrefix(prefix.value)
  fun hasPrefix(prefix: String) = value.startsWith(prefix)
  fun hasPrefix(prefix: SourceSetName) = hasPrefix(prefix.value)
  fun addPrefix(prefix: String) = prefix.plus(value.capitalize()).asSourceSetName()
  fun addPrefix(prefix: SourceSetName) = addPrefix(prefix.value)
  fun removeSuffix(suffix: String) = value.removeSuffix(suffix.capitalize()).asSourceSetName()
  fun removeSuffix(suffix: SourceSetName) = removeSuffix(suffix.value.capitalize())
  fun addSuffix(suffix: String) = value.plus(suffix.capitalize()).asSourceSetName()
  fun addSuffix(suffix: SourceSetName) = addSuffix(suffix.value)

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

    fun String.asSourceSetName(): SourceSetName = SourceSetName(this)
  }
}
