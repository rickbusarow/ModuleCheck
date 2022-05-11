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

package modulecheck.gradle.platforms.sourcesets.internal

import modulecheck.gradle.platforms.sourcesets.internal.GradleSourceSetName.BuildTypeName
import modulecheck.gradle.platforms.sourcesets.internal.GradleSourceSetName.ConcatenatedFlavorsName
import modulecheck.gradle.platforms.sourcesets.internal.GradleSourceSetName.FlavorName
import modulecheck.gradle.platforms.sourcesets.internal.GradleSourceSetName.TestType
import modulecheck.gradle.platforms.sourcesets.internal.GradleSourceSetName.VariantName
import modulecheck.utils.capitalize

internal data class ParsedNames(
  val variantName: VariantName,
  val concatenatedFlavorsName: ConcatenatedFlavorsName?,
  val buildTypeName: BuildTypeName,
  val flavors: List<FlavorName>,
  val testTypeOrNull: TestType?
)

internal sealed interface GradleSourceSetName : Comparable<GradleSourceSetName> {
  val value: String

  sealed interface TestType : GradleSourceSetName

  @JvmInline
  value class VariantName(override val value: String) : GradleSourceSetName

  @JvmInline
  value class BuildTypeName(override val value: String) : GradleSourceSetName

  @JvmInline
  value class FlavorName(override val value: String) : GradleSourceSetName

  @JvmInline
  value class ConcatenatedFlavorsName(override val value: String) : GradleSourceSetName

  object MainName : GradleSourceSetName {
    override val value = "main"
  }

  object AndroidTestName : GradleSourceSetName, TestType {
    override val value = "androidTest"
  }

  object UnitTestName : GradleSourceSetName, TestType {
    override val value = "test"
  }

  /**
   * Represents the `test` or `androidTest` corollary to a normal/published source set.
   *
   * For instance, a source set of `testDebug` has a test prefix of `test` and a published property
   * of `debug`.
   */
  data class TestSourceName<T : GradleSourceSetName>(
    val testPrefix: TestType,
    val published: T
  ) : GradleSourceSetName {
    override val value = "${testPrefix.value}${published.value.capitalize()}"
  }

  override fun compareTo(other: GradleSourceSetName): Int {

    @Suppress("MagicNumber")
    fun GradleSourceSetName.score(): Int {
      return when (this) {
        is BuildTypeName -> 4
        is ConcatenatedFlavorsName -> 7
        is FlavorName -> 10
        MainName -> 0
        is VariantName -> 2
        AndroidTestName -> 1
        UnitTestName -> 1
        is TestSourceName<*> -> 2
      }
    }

    val score1 = score()
    val score2 = other.score()

    return score1.compareTo(score2)
  }
}
