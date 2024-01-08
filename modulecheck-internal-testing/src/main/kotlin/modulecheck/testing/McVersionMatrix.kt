/*
 * Copyright (C) 2021-2024 Rick Busarow
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

package modulecheck.testing

import com.rickbusarow.kase.KaseMatrix
import com.rickbusarow.kase.get
import com.rickbusarow.kase.gradle.AgpDependencyVersion
import com.rickbusarow.kase.gradle.AnvilDependencyVersion
import com.rickbusarow.kase.gradle.GradleDependencyVersion
import com.rickbusarow.kase.gradle.KotlinDependencyVersion
import modulecheck.utils.letIf
import kotlin.LazyThreadSafetyMode.NONE

/** */
class McVersionMatrix(
  gradleVersions: List<String> = Versions.gradleList,
  agpVersions: List<String> = Versions.agpList,
  anvilVersions: List<String> = Versions.anvilList,
  kotlinVersions: List<String> = Versions.kotlinList
) : KaseMatrix by KaseMatrix.invoke(
  listOf(
    gradleVersions.map(::GradleDependencyVersion),
    agpVersions.map(::AgpDependencyVersion),
    anvilVersions.map(::AnvilDependencyVersion),
    kotlinVersions.map(::KotlinDependencyVersion)
  )
) {

  /** every permutation in the matrix without any filtering */
  val allVersions: List<McTestVersions> by lazy(NONE) {
    get(
      GradleDependencyVersion,
      KotlinDependencyVersion,
      AnvilDependencyVersion,
      AgpDependencyVersion,
      ::McTestVersions
    )
  }

  /** either all permutations or just the last */
  fun versions(exhaustive: Boolean): List<McTestVersions> {
    return allVersions.letIf(!exhaustive) { it.takeLast(1) }
  }
}
