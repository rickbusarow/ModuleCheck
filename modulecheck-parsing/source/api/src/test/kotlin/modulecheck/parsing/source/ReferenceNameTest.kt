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

import modulecheck.parsing.source.ReferenceName.ExplicitJavaReferenceName
import modulecheck.parsing.source.ReferenceName.ExplicitKotlinReferenceName
import modulecheck.parsing.source.ReferenceName.ExplicitXmlReferenceName
import modulecheck.parsing.source.ReferenceName.InterpretedJavaReferenceName
import modulecheck.parsing.source.ReferenceName.InterpretedKotlinReferenceName
import modulecheck.parsing.source.UnqualifiedAndroidResourceDeclaredName.AndroidString
import org.junit.jupiter.api.Test

class ReferenceNameTest : BaseMcNameTest() {

  @Test
  fun `explicit java reference`() {
    ExplicitJavaReferenceName("com.modulecheck.subject").matchedClasses() shouldBe listOf(
      AgnosticDeclaredName::class,
      AndroidDataBindingReferenceName::class,
      AndroidRReferenceName::class,
      ExplicitJavaReferenceName::class,
      InterpretedJavaReferenceName::class,
      JavaSpecificDeclaredName::class,
      QualifiedAndroidResourceReferenceName::class
    )
  }

  @Test
  fun `explicit kotlin reference`() {
    ExplicitKotlinReferenceName("com.modulecheck.subject").matchedClasses() shouldBe listOf(
      AgnosticDeclaredName::class,
      AndroidDataBindingReferenceName::class,
      AndroidRReferenceName::class,
      ExplicitKotlinReferenceName::class,
      InterpretedKotlinReferenceName::class,
      KotlinSpecificDeclaredName::class,
      QualifiedAndroidResourceReferenceName::class
    )
  }

  @Test
  fun `explicit xml reference`() {
    ExplicitXmlReferenceName("com.modulecheck.subject").matchedClasses() shouldBe listOf(
      AgnosticDeclaredName::class,
      AndroidDataBindingReferenceName::class,
      AndroidRReferenceName::class,
      ExplicitJavaReferenceName::class,
      ExplicitXmlReferenceName::class,
      InterpretedJavaReferenceName::class,
      JavaSpecificDeclaredName::class,
      QualifiedAndroidResourceReferenceName::class
    )
  }

  @Test
  fun `interpreted java reference`() {
    InterpretedJavaReferenceName("com.modulecheck.subject").matchedClasses() shouldBe listOf(
      AgnosticDeclaredName::class,
      AndroidDataBindingReferenceName::class,
      ExplicitJavaReferenceName::class,
      InterpretedJavaReferenceName::class,
      JavaSpecificDeclaredName::class
    )
  }

  @Test
  fun `interpreted kotlin reference`() {
    InterpretedKotlinReferenceName("com.modulecheck.subject").matchedClasses() shouldBe listOf(
      AgnosticDeclaredName::class,
      AndroidDataBindingReferenceName::class,
      ExplicitKotlinReferenceName::class,
      InterpretedKotlinReferenceName::class,
      KotlinSpecificDeclaredName::class
    )
  }

  @Test
  fun `unqualified android resource reference`() {
    UnqualifiedAndroidResourceReferenceName("R.string.subject").matchedClasses() shouldBe listOf(
      AgnosticDeclaredName::class,
      AndroidDataBindingReferenceName::class,
      AndroidRReferenceName::class,
      AndroidString::class,
      ExplicitJavaReferenceName::class,
      ExplicitKotlinReferenceName::class,
      ExplicitXmlReferenceName::class,
      InterpretedJavaReferenceName::class,
      InterpretedKotlinReferenceName::class,
      QualifiedAndroidResourceReferenceName::class,
      UnqualifiedAndroidResourceReferenceName::class
    )
  }

  @Test
  fun `duplicate names of incompatible types are allowed in a set`() {
    val list = listOf(
      ExplicitKotlinReferenceName("name"),
      ExplicitJavaReferenceName("name")
    )

    val set = list.toSet()

    set.toList() shouldBe list
  }

  @Test
  fun `duplicate names of compatible types are allowed in a set`() {
    val list = listOf(
      ExplicitKotlinReferenceName("name"),
      ExplicitJavaReferenceName("name")
    )

    val set = list.toSet()

    set.toList() shouldBe list
  }
}
