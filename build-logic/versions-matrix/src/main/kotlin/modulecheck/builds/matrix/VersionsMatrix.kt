/*
 * Copyright (C) 2021-2023 Rick Busarow
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

  internal val gradleListDefault = listOf("7.5.1", "7.6.1", "8.0.2", "8.1.1")
  internal val agpListDefault = listOf("7.3.1", "7.4.2", "8.0.1")
  internal val anvilListDefault = listOf("2.4.5")
  internal val kotlinListDefault = listOf("1.8.0", "1.8.10", "1.8.21")

  val gradleList = gradleArg?.singletonList() ?: gradleListDefault
  val agpList = agpArg?.singletonList() ?: agpListDefault
  val anvilList = anvilArg?.singletonList() ?: anvilListDefault
  val kotlinList = kotlinArg?.singletonList() ?: kotlinListDefault

  internal val exclusions = listOf<Exclusion>(
    Exclusion(gradle = "8.1.1", agp = "7.3.1", anvil = null, kotlin = null),
    Exclusion(gradle = "8.1.1", agp = "7.4.2", anvil = null, kotlin = null),
    Exclusion(gradle = "7.5.1", agp = "8.0.1", anvil = null, kotlin = null),
    Exclusion(gradle = "7.6.1", agp = "8.0.1", anvil = null, kotlin = null),
  ).requireNoDuplicates()

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
  private fun combinations(
    gradleList: List<String>,
    agpList: List<String>,
    anvilList: List<String>,
    kotlinList: List<String>
  ) =
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

  val allValidDefaults = combinations(
    gradleList = gradleListDefault,
    agpList = agpListDefault,
    anvilList = anvilListDefault,
    kotlinList = kotlinListDefault
  ).filtered(exclusions).requireNotEmpty()

  val allValid = combinations(
    gradleList = gradleList,
    agpList = agpList,
    anvilList = anvilList,
    kotlinList = kotlinList
  ).filtered(exclusions).requireNotEmpty()

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
      val filteredWithout = combinations(
        gradleList = gradleList,
        agpList = agpList,
        anvilList = anvilList,
        kotlinList = kotlinList
      ).filtered(exclusions.filterNot { it == exclusion })

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
