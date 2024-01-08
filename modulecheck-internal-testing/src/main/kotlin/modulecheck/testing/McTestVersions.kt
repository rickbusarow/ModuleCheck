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

import com.rickbusarow.kase.Kase4
import com.rickbusarow.kase.KaseMatrix
import com.rickbusarow.kase.get
import com.rickbusarow.kase.gradle.AgpDependencyVersion
import com.rickbusarow.kase.gradle.AnvilDependencyVersion
import com.rickbusarow.kase.gradle.GradleDependencyVersion
import com.rickbusarow.kase.gradle.GradleKotlinTestVersions
import com.rickbusarow.kase.gradle.GradleTestVersions
import com.rickbusarow.kase.gradle.HasAgpDependencyVersion
import com.rickbusarow.kase.gradle.HasAnvilDependencyVersion
import com.rickbusarow.kase.gradle.HasGradleDependencyVersion
import com.rickbusarow.kase.gradle.HasKotlinDependencyVersion
import com.rickbusarow.kase.gradle.KotlinDependencyVersion
import com.rickbusarow.kase.gradle.TestVersionsFactory
import com.rickbusarow.kase.kase

/**
 * The versions of dependencies which are changed during parameterized tests.
 *
 * @param a1 Gradle
 * @param a2 Kotlin
 * @param a3 Anvil
 * @param a4 AGP
 */
class McTestVersions(
  a1: GradleDependencyVersion,
  a2: KotlinDependencyVersion,
  a3: AnvilDependencyVersion,
  a4: AgpDependencyVersion
) : GradleKotlinAnvilAgpTestVersions by DefaultGradleKotlinAnvilAgpTestVersions(
  a1 = a1,
  a2 = a2,
  a3 = a3,
  a4 = a4
)

/** Trait interface for [McTestVersions]*/
interface HasTestVersions {
  /** immutable */
  val testVersions: McTestVersions
}

/**
 * Holds [GradleDependencyVersion], [KotlinDependencyVersion], and [AgpDependencyVersion] versions
 */
interface GradleKotlinAnvilAgpTestVersions :
  com.rickbusarow.kase.gradle.TestVersions,
  HasGradleDependencyVersion,
  HasKotlinDependencyVersion,
  HasAnvilDependencyVersion,
  HasAgpDependencyVersion,
  Kase4<GradleDependencyVersion, KotlinDependencyVersion, AnvilDependencyVersion, AgpDependencyVersion>,
  GradleTestVersions,
  GradleKotlinTestVersions {

  companion object : TestVersionsFactory<GradleKotlinAnvilAgpTestVersions> {
    override fun extract(matrix: KaseMatrix): List<GradleKotlinAnvilAgpTestVersions> = matrix.get(
      a1Key = GradleDependencyVersion,
      a2Key = KotlinDependencyVersion,
      a3Key = AnvilDependencyVersion,
      a4Key = AgpDependencyVersion,
      instanceFactory = ::DefaultGradleKotlinAnvilAgpTestVersions
    )
  }
}

/**
 * Holds [GradleDependencyVersion], [KotlinDependencyVersion],
 * [AnvilDependencyVersion], and [AgpDependencyVersion] versions
 */
class DefaultGradleKotlinAnvilAgpTestVersions(
  override val a1: GradleDependencyVersion,
  override val a2: KotlinDependencyVersion,
  override val a3: AnvilDependencyVersion,
  override val a4: AgpDependencyVersion
) : GradleKotlinAnvilAgpTestVersions,
  HasGradleDependencyVersion by HasGradleDependencyVersion(a1),
  HasKotlinDependencyVersion by HasKotlinDependencyVersion(a2),
  HasAnvilDependencyVersion by HasAnvilDependencyVersion(a3),
  HasAgpDependencyVersion by HasAgpDependencyVersion(a4),
  Kase4<GradleDependencyVersion, KotlinDependencyVersion, AnvilDependencyVersion, AgpDependencyVersion>
  by kase(a1, a2, a3, a4) {

  override val displayName: String = toString()
  override fun toString(): String = "[gradle $gradle | agp $agp | anvil $anvil | kotlin $kotlin]"
}
