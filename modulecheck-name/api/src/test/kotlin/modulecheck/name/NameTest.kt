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

package modulecheck.name

import modulecheck.name.SimpleName.Companion.asSimpleName
import modulecheck.testing.BaseTest
import modulecheck.testing.TestEnvironment
import org.junit.jupiter.api.Test

class NameTest : BaseTest<TestEnvironment>() {

  @Test
  fun `sorting should be by name first, then the name of the Name class`() {
    val packageNames = listOf("a", "b", "c", "d")
    val simpleNames = listOf("X", "Y", "Z")

    val instances = packageNames
      .reversed()
      .flatMap { packageName ->
        simpleNames
          .reversed()
          .flatMap { simpleName ->

            listOf(
              NameWithPackageName(
                PackageName(packageName),
                listOf(simpleName.asSimpleName())
              ),
              NameWithPackageName(
                PackageName(packageName),
                listOf(simpleName.asSimpleName())
              ),
              NameWithPackageName(
                PackageName(packageName),
                listOf(simpleName.asSimpleName())
              ),
              AndroidRName(PackageName(packageName))
            )
          }
      }
      .shuffled()
      // Android R names will be duplicated, so clean those up
      .distinctBy { it.asString to it::class }

    val prettySorted = instances.sorted()
      .joinToString("\n") { "${it::class.java.simpleName.padStart(28)} ${it.asString}" }

    prettySorted shouldBe """
               AndroidRName a.R
    NameWithPackageNameImpl a.X
    NameWithPackageNameImpl a.Y
    NameWithPackageNameImpl a.Z
               AndroidRName b.R
    NameWithPackageNameImpl b.X
    NameWithPackageNameImpl b.Y
    NameWithPackageNameImpl b.Z
               AndroidRName c.R
    NameWithPackageNameImpl c.X
    NameWithPackageNameImpl c.Y
    NameWithPackageNameImpl c.Z
               AndroidRName d.R
    NameWithPackageNameImpl d.X
    NameWithPackageNameImpl d.Y
    NameWithPackageNameImpl d.Z
    """.trimIndent()
  }
}
