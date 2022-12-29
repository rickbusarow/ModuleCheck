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

import kotlinx.serialization.Serializable
import modulecheck.utils.capitalize
import modulecheck.utils.decapitalize
import java.io.Serializable as JavaSerializable

/**
 * Something associated with a specific [SourceSetName][modulecheck.model.sourceset.SourceSetName].
 *
 * @since 0.13.0
 */
interface HasSourceSetName : JavaSerializable {

  /**
   * ex: `main`, `test`, `debug`
   *
   * @since 0.13.0
   */
  val sourceSetName: SourceSetName
}

/**
 * ex: `main`, `test`, `debug`
 *
 * @since 0.13.0
 */
@Serializable
@JvmInline
value class SourceSetName(val value: String) : JavaSerializable {

  fun isTestFixtures(): Boolean = value.startsWith(TEST_FIXTURES.value, ignoreCase = true)

  override fun toString(): String = "(SourceSetName) `$value`"

  companion object {
    val ANDROID_TEST: SourceSetName = SourceSetName("androidTest")
    val ANVIL: SourceSetName = SourceSetName("anvil")
    val DEBUG: SourceSetName = SourceSetName("debug")
    val KAPT: SourceSetName = SourceSetName("kapt")
    val MAIN: SourceSetName = SourceSetName("main")
    val RELEASE: SourceSetName = SourceSetName("release")
    val TEST: SourceSetName = SourceSetName("test")
    val TEST_FIXTURES: SourceSetName = SourceSetName("testFixtures")
  }
}

fun SourceSetName.toProto() = SourceSetName_Proto(value)
fun SourceSetName.toPojo() = SourceSetName(value)

fun String.asSourceSetName(): SourceSetName = SourceSetName(this)

fun SourceSetName.removePrefix(prefix: String): SourceSetName = value.removePrefix(prefix)
  .decapitalize()
  .asSourceSetName()

fun SourceSetName.removePrefix(prefix: SourceSetName): SourceSetName = removePrefix(prefix.value)

fun SourceSetName.hasPrefix(prefix: String): Boolean = value.startsWith(prefix)
fun SourceSetName.hasPrefix(prefix: SourceSetName): Boolean = hasPrefix(prefix.value)

fun SourceSetName.addPrefix(prefix: String): SourceSetName = prefix.plus(value.capitalize())
  .asSourceSetName()

fun SourceSetName.addPrefix(prefix: SourceSetName): SourceSetName = addPrefix(prefix.value)

fun SourceSetName.removeSuffix(suffix: String): SourceSetName =
  value.removeSuffix(suffix.capitalize())
    .asSourceSetName()

fun SourceSetName.removeSuffix(suffix: SourceSetName): SourceSetName =
  removeSuffix(suffix.value.capitalize())

fun SourceSetName.addSuffix(suffix: String): SourceSetName = value.plus(suffix.capitalize())
  .asSourceSetName()

fun SourceSetName.addSuffix(suffix: SourceSetName): SourceSetName = addSuffix(suffix.value)
