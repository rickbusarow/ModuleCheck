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

package modulecheck.model.sourceset

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