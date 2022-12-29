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

package modulecheck.builds.matrix

class VersionsMatrix(
  val exhaustive: Boolean = false,
  private val gradleArg: String? = null,
  private val agpArg: String? = null,
  private val anvilArg: String? = null,
  private val kotlinArg: String? = null
) {

  val agpList = agpArg?.singletonList()
    ?: listOf("7.0.1", "7.1.3", "7.2.2", "7.3.1")
  val anvilList = anvilArg?.singletonList()
    ?: listOf("2.4.3", "2.4.3-1-8-0-RC")
  val gradleList = gradleArg?.singletonList()
    ?: listOf("7.2", "7.4.2", "7.5.1", "7.6", "8.0-rc-1")
  val kotlinList = kotlinArg?.singletonList()
    ?: listOf("1.7.0", "1.7.10", "1.7.22", "1.8.0-RC2")

  internal val exclusions = listOf(
    Exclusion(gradle = null, agp = "7.0.1", anvil = "2.4.3", kotlin = null),
    Exclusion(gradle = null, agp = "7.0.1", anvil = "2.4.3-1-8-0-RC", kotlin = null),
    Exclusion(gradle = "7.2", agp = "7.2.2", anvil = null, kotlin = null),
    Exclusion(gradle = "7.2", agp = "7.3.1", anvil = null, kotlin = null),
    Exclusion(gradle = "7.4.2", agp = null, anvil = "2.4.3", kotlin = null),
    Exclusion(gradle = "7.4.2", agp = null, anvil = "2.4.3-1-8-0-RC", kotlin = null)
  )
    .requireNoDuplicates()

  private val latest by lazy { allValid.last() }

  val defaultGradle by lazy { gradleArg ?: latest.gradle }
  val defaultAgp by lazy { agpArg ?: latest.agp }
  val defaultAnvil by lazy { anvilArg ?: latest.anvil }
  val defaultKotlin by lazy { kotlinArg ?: latest.kotlin }

  // ORDER MATTERS.
  // ...at least with regard to Gradle.
  // Gradle > AGP > Anvil > Kotlin
  // By testing all the Gradle versions together, TestKit doesn't have to re-download everything
  // for each new test. As soon as the Gradle version changes, the previous Gradle version is
  // deleted.
  private val combinations =
    gradleList.flatMap { gradle ->
      agpList.flatMap { agp ->
        anvilList.flatMap { anvil ->
          kotlinList.map { kotlin ->
            TestVersions(
              gradle = gradle,
              agp = agp,
              anvil = anvil,
              kotlin = kotlin
            )
          }
        }
      }
    }

  val allValid = combinations.filtered(exclusions).requireNotEmpty()

  init {

    requireNoUselessExclusions()
  }

  private fun List<TestVersions>.requireNotEmpty() = apply {
    require(isNotEmpty()) {
      val arguments = listOf(
        "gradle" to gradleArg,
        "agp" to agpArg,
        "anvil" to anvilArg,
        "kotlin" to kotlinArg
      ).filter { pair -> pair.second != null }
        .map { (name, version) -> "$name=$version" }

      "There are no valid version combinations to be made " +
        "from the provided arguments: $arguments"
    }
  }

  private fun List<Exclusion>.requireNoDuplicates() = also { exclusions ->
    require(exclusions.toSet().size == exclusions.size) {
      val duplicates = exclusions.filter { target ->
        exclusions.filter { it == target }.size > 1
      }
        .distinct()

      "There are duplicate (identical) exclusions (this list shows one of each type):\n" +
        duplicates.joinToString("\n\t")
    }
  }

  private fun requireNoUselessExclusions() {
    // If we're using arguments, then the baseline `combinations` list will naturally be smaller.
    // This check can be skipped.
    if (listOfNotNull(gradleArg, agpArg, anvilArg, kotlinArg).isNotEmpty()) return

    val redundant = mutableListOf<Exclusion>()

    exclusions.forEach { exclusion ->
      val filteredWithout = combinations.filtered(exclusions.filterNot { it == exclusion })

      val leftOut = filteredWithout.subtract(allValid.toSet())
        .sortedBy { it.hashCode() }

      if (leftOut.isEmpty()) {
        redundant.add(exclusion)
      }
    }

    require(redundant.isEmpty()) {
      "The following defined exclusions all achieve the same filtered result.  Leave one:\n" +
        redundant.joinToString("\n\t", "\t")
    }
  }

  private fun <T> T.singletonList() = listOf(this)

  internal operator fun Collection<Exclusion>.contains(
    testVersions: TestVersions
  ): Boolean {
    return any { testVersions.excludedBy(it) }
  }

  private fun TestVersions.excludedBy(exclusion: Exclusion): Boolean {
    return when {
      exclusion.gradle != null && exclusion.gradle != gradle -> false
      exclusion.agp != null && exclusion.agp != agp -> false
      exclusion.kotlin != null && exclusion.kotlin != kotlin -> false
      exclusion.anvil != null && exclusion.anvil != anvil -> false
      else -> true
    }
  }

  private fun List<TestVersions>.filtered(exclusions: List<Exclusion>): List<TestVersions> {
    return filterNot { versions -> exclusions.contains(versions) }
  }

  data class Exclusion(
    val gradle: String? = null,
    val agp: String? = null,
    val anvil: String? = null,
    val kotlin: String? = null
  )
}
