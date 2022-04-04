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

import modulecheck.parsing.source.Reference.ExplicitJavaReference
import modulecheck.parsing.source.Reference.ExplicitKotlinReference
import modulecheck.parsing.source.Reference.ExplicitXmlReference
import modulecheck.parsing.source.Reference.InterpretedJavaReference
import modulecheck.parsing.source.Reference.InterpretedKotlinReference
import modulecheck.parsing.source.Reference.UnqualifiedAndroidResourceReference
import modulecheck.parsing.source.UnqualifiedAndroidResourceDeclaredName.AndroidString
import org.junit.jupiter.api.Test

class ReferenceTest : BaseNamedSymbolTest() {

  @Test
  fun `explicit java reference`() {
    ExplicitJavaReference("com.modulecheck.subject").matchedClasses() shouldBe listOf(
      AgnosticDeclaredName::class,
      ExplicitJavaReference::class,
      InterpretedJavaReference::class,
      JavaSpecificDeclaredName::class
    )
  }

  @Test
  fun `explicit kotlin reference`() {
    ExplicitKotlinReference("com.modulecheck.subject").matchedClasses() shouldBe listOf(
      AgnosticDeclaredName::class,
      ExplicitKotlinReference::class,
      InterpretedKotlinReference::class,
      KotlinSpecificDeclaredName::class
    )
  }

  @Test
  fun `explicit xml reference`() {
    ExplicitXmlReference("com.modulecheck.subject").matchedClasses() shouldBe listOf(
      AgnosticDeclaredName::class,
      ExplicitJavaReference::class,
      ExplicitXmlReference::class,
      InterpretedJavaReference::class,
      JavaSpecificDeclaredName::class
    )
  }

  @Test
  fun `interpreted java reference`() {
    InterpretedJavaReference("com.modulecheck.subject").matchedClasses() shouldBe listOf(
      AgnosticDeclaredName::class,
      ExplicitJavaReference::class,
      InterpretedJavaReference::class,
      JavaSpecificDeclaredName::class
    )
  }

  @Test
  fun `interpreted kotlin reference`() {
    InterpretedKotlinReference("com.modulecheck.subject").matchedClasses() shouldBe listOf(
      AgnosticDeclaredName::class,
      ExplicitKotlinReference::class,
      InterpretedKotlinReference::class,
      KotlinSpecificDeclaredName::class
    )
  }

  @Test
  fun `unqualified android resource reference`() {
    UnqualifiedAndroidResourceReference("R.string.subject").matchedClasses() shouldBe listOf(
      AgnosticDeclaredName::class,
      AndroidString::class,
      UnqualifiedAndroidResourceReference::class
    )
  }

  @Test
  fun `duplicate names of incompatible types are allowed in a set`() {
    val list = listOf(
      ExplicitKotlinReference("name"),
      ExplicitJavaReference("name")
    )

    val set = list.toSet()

    set.toList() shouldBe list
  }

  @Test
  fun `duplicate names of compatible types are allowed in a set`() {
    val list = listOf(
      ExplicitKotlinReference("name"),
      ExplicitJavaReference("name")
    )

    val set = list.toSet()

    set.toList() shouldBe list
  }
}
