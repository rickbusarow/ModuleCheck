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

import modulecheck.parsing.source.ReferenceName.JavaReferenceName
import modulecheck.parsing.source.ReferenceName.JavaReferenceNameImpl
import modulecheck.parsing.source.ReferenceName.KotlinReferenceName
import modulecheck.parsing.source.ReferenceName.KotlinReferenceNameImpl
import modulecheck.parsing.source.ReferenceName.XmlReferenceNameImpl
import modulecheck.parsing.source.UnqualifiedAndroidResourceDeclaredName.AndroidString
import org.junit.jupiter.api.Test

class ReferenceNameTest : BaseMcNameTest() {

  @Test
  fun `java reference`() {
    JavaReferenceName("com.modulecheck.subject").matchedClasses() shouldBe listOf(
      AgnosticDeclaredName::class,
      AndroidDataBindingReferenceName::class,
      AndroidRReferenceName::class,
      JavaReferenceNameImpl::class,
      JavaSpecificDeclaredName::class,
      QualifiedAndroidResourceReferenceName::class
    )
  }

  @Test
  fun `kotlin reference`() {
    KotlinReferenceName("com.modulecheck.subject").matchedClasses() shouldBe listOf(
      AgnosticDeclaredName::class,
      AndroidDataBindingReferenceName::class,
      AndroidRReferenceName::class,
      KotlinReferenceNameImpl::class,
      QualifiedAndroidResourceReferenceName::class,
      TopLevelKotlinSpecificDeclaredName::class
    )
  }

  @Test
  fun `xml reference`() {
    XmlReferenceNameImpl("com.modulecheck.subject").matchedClasses() shouldBe listOf(
      AgnosticDeclaredName::class,
      AndroidDataBindingReferenceName::class,
      AndroidRReferenceName::class,
      JavaReferenceNameImpl::class,
      JavaSpecificDeclaredName::class,
      QualifiedAndroidResourceReferenceName::class,
      XmlReferenceNameImpl::class
    )
  }

  @Test
  fun `unqualified android resource reference`() {
    UnqualifiedAndroidResourceReferenceName("R.string.subject").matchedClasses() shouldBe listOf(
      AgnosticDeclaredName::class,
      AndroidDataBindingReferenceName::class,
      AndroidRReferenceName::class,
      AndroidString::class,
      JavaReferenceNameImpl::class,
      KotlinReferenceNameImpl::class,
      QualifiedAndroidResourceReferenceName::class,
      UnqualifiedAndroidResourceReferenceName::class,
      XmlReferenceNameImpl::class
    )
  }

  @Test
  fun `duplicate names of incompatible types are allowed in a set`() {
    val list = listOf(
      KotlinReferenceName("name"),
      JavaReferenceName("name")
    )

    val set = list.toSet()

    set.toList() shouldBe list
  }

  @Test
  fun `duplicate names of compatible types are allowed in a set`() {
    val list = listOf(
      KotlinReferenceName("name"),
      JavaReferenceName("name")
    )

    val set = list.toSet()

    set.toList() shouldBe list
  }
}
