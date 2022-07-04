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

package modulecheck.parsing.source

import modulecheck.parsing.source.SimpleName.Companion.asSimpleName
import org.junit.jupiter.api.Test

class DeclaredNameTest : BaseMcNameTest() {

  @Test
  fun `duplicate names of incompatible types are allowed in a set`() {
    val list = listOf(
      DeclaredName.java(
        PackageName("com.modulecheck"),
        listOf("name".asSimpleName())
      ),
      DeclaredName.kotlin(
        PackageName("com.modulecheck"),
        listOf("name".asSimpleName())
      )
    )

    val set = list.toSet()

    set.toList() shouldBe list
  }

  @Test
  fun `duplicate names of compatible types are allowed in a set`() {
    val list = listOf(
      DeclaredName.agnostic(
        PackageName("com.modulecheck"),
        listOf("name".asSimpleName())
      ),
      DeclaredName.kotlin(
        PackageName("com.modulecheck"),
        listOf("name".asSimpleName())
      )
    )

    val set = list.toSet()

    set.toList() shouldBe list
  }

  @Test
  fun `sorting should be by name first, then the name of the DeclaredName class`() {
    val packageNames = listOf("a", "b", "c", "d")
    val simpleNames = listOf("X", "Y", "Z")

    val instances = packageNames
      .reversed()
      .flatMap { packageName ->
        simpleNames
          .reversed()
          .flatMap { simpleName ->

            listOf(
              DeclaredName.kotlin(
                PackageName(packageName),
                listOf(simpleName.asSimpleName())
              ),
              DeclaredName.agnostic(
                PackageName(packageName),
                listOf(simpleName.asSimpleName())
              ),
              DeclaredName.java(
                PackageName(packageName),
                listOf(simpleName.asSimpleName())
              ),
              AndroidResourceDeclaredName.r(PackageName(packageName))
            )
          }
      }
      .shuffled()
      // Android R names will be duplicated, so clean those up
      .distinctBy { it.name to it::class }

    val prettySorted = instances.sorted()
      .joinToString("\n") { "${it::class.java.simpleName.padStart(28)} ${it.name}" }

    prettySorted shouldBe """
         AndroidRDeclaredName a.R
    QualifiedDeclaredNameImpl a.X
    QualifiedDeclaredNameImpl a.Y
    QualifiedDeclaredNameImpl a.Z
         AndroidRDeclaredName b.R
    QualifiedDeclaredNameImpl b.X
    QualifiedDeclaredNameImpl b.Y
    QualifiedDeclaredNameImpl b.Z
         AndroidRDeclaredName c.R
    QualifiedDeclaredNameImpl c.X
    QualifiedDeclaredNameImpl c.Y
    QualifiedDeclaredNameImpl c.Z
         AndroidRDeclaredName d.R
    QualifiedDeclaredNameImpl d.X
    QualifiedDeclaredNameImpl d.Y
    QualifiedDeclaredNameImpl d.Z
    """.trimIndent()
  }
}
