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

import modulecheck.parsing.source.Reference.UnqualifiedAndroidResourceReference
import modulecheck.testing.sealedSubclassInstances
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestFactory

class DeclaredNameTest : BaseNamedSymbolTest() {

  @Test
  fun `agnostic declaration should match self and any reference type`() {
    AgnosticDeclaredName("com.modulecheck.subject").matchedClasses() shouldBe listOf(
      AgnosticDeclaredName::class,
      Reference.ExplicitJavaReference::class,
      Reference.ExplicitKotlinReference::class,
      Reference.ExplicitXmlReference::class,
      Reference.InterpretedJavaReference::class,
      Reference.InterpretedKotlinReference::class
    )
  }

  @Test
  fun `kotlin specific declaration should match self and any KotlinReference type`() {
    KotlinSpecificDeclaredName("com.modulecheck.subject").matchedClasses() shouldBe listOf(
      Reference.ExplicitKotlinReference::class,
      Reference.InterpretedKotlinReference::class,
      KotlinSpecificDeclaredName::class
    )
  }

  @Test
  fun `java specific declaration should match self and any JavaReference or XmlReference type`() {
    JavaSpecificDeclaredName("com.modulecheck.subject").matchedClasses() shouldBe listOf(
      Reference.ExplicitJavaReference::class,
      Reference.ExplicitXmlReference::class,
      Reference.InterpretedJavaReference::class,
      JavaSpecificDeclaredName::class
    )
  }

  @Test
  fun `android r declaration should match self and any Reference type`() {
    AndroidRDeclaredName("com.modulecheck.R").matchedClasses() shouldBe listOf(
      AndroidRDeclaredName::class,
      Reference.ExplicitJavaReference::class,
      Reference.ExplicitKotlinReference::class,
      Reference.ExplicitXmlReference::class,
      Reference.InterpretedJavaReference::class,
      Reference.InterpretedKotlinReference::class
    )
  }

  @TestFactory
  fun `android resource declaration should match self and any Reference type`() =
    UnqualifiedAndroidResourceDeclaredName::class
      .sealedSubclassInstances("subject")
      .dynamic(
        testName = { subject -> subject::class.java.simpleName }
      ) { subject ->

        oneOfEach("subject")
          .plus(UnqualifiedAndroidResourceReference("R.${subject.prefix}.subject"))
          .filter { it == subject }
          .map { it::class }
          .sortedBy { it.java.simpleName } shouldBe listOf(
          subject::class,
          Reference.UnqualifiedAndroidResourceReference::class
        ).sortedBy { it.simpleName }
      }

  @Test
  fun `duplicate names of incompatible types are allowed in a set`() {
    val list = listOf(
      JavaSpecificDeclaredName("name"),
      KotlinSpecificDeclaredName("name")
    )

    val set = list.toSet()

    set.toList() shouldBe list
  }

  @Test
  fun `duplicate names of compatible types are allowed in a set`() {
    val list = listOf(
      AgnosticDeclaredName("name"),
      KotlinSpecificDeclaredName("name")
    )

    val set = list.toSet()

    set.toList() shouldBe list
  }

  @Test
  fun `sorting should be by name first, then class name`() {
    val names = listOf("a", "b", "c", "d")

    val instances = names
      .reversed()
      .flatMap {
        listOf(
          KotlinSpecificDeclaredName(it),
          AgnosticDeclaredName(it),
          JavaSpecificDeclaredName(it),
          AndroidRDeclaredName("$it.R")
        )
      }
      .shuffled()

    val prettySorted = instances.sorted()
      .joinToString("\n") { "${it::class.java.simpleName.padStart(28)} ${it.name}" }

    prettySorted shouldBe """
            AgnosticDeclaredName a
        JavaSpecificDeclaredName a
      KotlinSpecificDeclaredName a
            AndroidRDeclaredName a.R
            AgnosticDeclaredName b
        JavaSpecificDeclaredName b
      KotlinSpecificDeclaredName b
            AndroidRDeclaredName b.R
            AgnosticDeclaredName c
        JavaSpecificDeclaredName c
      KotlinSpecificDeclaredName c
            AndroidRDeclaredName c.R
            AgnosticDeclaredName d
        JavaSpecificDeclaredName d
      KotlinSpecificDeclaredName d
            AndroidRDeclaredName d.R
    """.trimIndent()
  }
}
