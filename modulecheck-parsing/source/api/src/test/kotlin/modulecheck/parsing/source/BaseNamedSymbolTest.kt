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

import io.kotest.assertions.asClue
import io.kotest.inspectors.forAll
import modulecheck.parsing.source.ReferenceName.ExplicitJavaReferenceName
import modulecheck.parsing.source.ReferenceName.ExplicitKotlinReferenceName
import modulecheck.parsing.source.ReferenceName.ExplicitXmlReferenceName
import modulecheck.parsing.source.ReferenceName.InterpretedJavaReferenceName
import modulecheck.parsing.source.ReferenceName.InterpretedKotlinReferenceName
import modulecheck.parsing.source.UnqualifiedAndroidResourceDeclaredName.AndroidInteger
import modulecheck.parsing.source.UnqualifiedAndroidResourceDeclaredName.AndroidString
import modulecheck.parsing.source.UnqualifiedAndroidResourceDeclaredName.Anim
import modulecheck.parsing.source.UnqualifiedAndroidResourceDeclaredName.Animator
import modulecheck.parsing.source.UnqualifiedAndroidResourceDeclaredName.Arrays
import modulecheck.parsing.source.UnqualifiedAndroidResourceDeclaredName.Bool
import modulecheck.parsing.source.UnqualifiedAndroidResourceDeclaredName.Color
import modulecheck.parsing.source.UnqualifiedAndroidResourceDeclaredName.Dimen
import modulecheck.parsing.source.UnqualifiedAndroidResourceDeclaredName.Drawable
import modulecheck.parsing.source.UnqualifiedAndroidResourceDeclaredName.Font
import modulecheck.parsing.source.UnqualifiedAndroidResourceDeclaredName.ID
import modulecheck.parsing.source.UnqualifiedAndroidResourceDeclaredName.Layout
import modulecheck.parsing.source.UnqualifiedAndroidResourceDeclaredName.Menu
import modulecheck.parsing.source.UnqualifiedAndroidResourceDeclaredName.Mipmap
import modulecheck.parsing.source.UnqualifiedAndroidResourceDeclaredName.Raw
import modulecheck.parsing.source.UnqualifiedAndroidResourceDeclaredName.Style
import modulecheck.testing.BaseTest
import modulecheck.testing.DynamicTests
import modulecheck.testing.sealedSubclassesRecursive
import modulecheck.utils.capitalize
import modulecheck.utils.suffixIfNot
import kotlin.reflect.full.isSubclassOf

abstract class BaseNamedSymbolTest : BaseTest(), DynamicTests {

  inline fun <C : Collection<T>, reified T : NamedSymbol> C.requireIsExhaustive(): C = apply {
    val allSealedSubclasses = NamedSymbol::class.sealedSubclassesRecursive()

    val actualClasses = map { it::class }

    // assert that every possible sealed subclass/interface is represented by at least one of the
    // elements in the receiver
    allSealedSubclasses.forAll { subInterface ->

      "should have a subtype in this list".asClue {
        actualClasses.any { actual ->
          actual.isSubclassOf(subInterface)
        } shouldBe true
      }
    }
  }

  fun NamedSymbol.matches() = oneOfEach(name).filter { it == this }
  fun NamedSymbol.matchedClasses() = matches()
    .map { it::class }
    .sortedBy { it.java.simpleName }

  fun oneOfEach(name: String, packageName: String = "com.subject"): List<NamedSymbol> {
    val identifier = name.split(".").last()

    return listOf(
      AndroidRDeclaredName(
        name.suffixIfNot(".R"),
        packageName = PackageName(packageName)
      ),
      AndroidDataBindingDeclaredName(
        "com.modulecheck.databinding.${identifier.capitalize()}Binding",
        sourceLayoutDeclaration = Layout(identifier), packageName = PackageName(packageName)
      ),

      AndroidInteger(identifier),
      AndroidString(identifier),
      Anim(identifier),
      Animator(identifier),
      Arrays(identifier),
      Bool(identifier),
      Color(identifier),
      Dimen(identifier),
      Drawable(identifier),
      Font(identifier),
      ID(identifier),
      Layout(identifier),
      Menu(identifier),
      Mipmap(identifier),
      Raw(identifier),
      Style(identifier),

      AndroidString(identifier).toNamespacedDeclaredName(
        AndroidRDeclaredName(
          "com.modulecheck.R",
          packageName = PackageName(packageName)
        )
      ),

      AgnosticDeclaredName(name, packageName = PackageName(packageName)),
      JavaSpecificDeclaredName(name, packageName = PackageName(packageName)),
      KotlinSpecificDeclaredName(name, packageName = PackageName(packageName)),

      ExplicitJavaReferenceName(name),
      ExplicitKotlinReferenceName(name),
      ExplicitXmlReferenceName(name),
      InterpretedJavaReferenceName(name),
      InterpretedKotlinReferenceName(name),
      AndroidRReferenceName(name),
      UnqualifiedAndroidResourceReferenceName(name),
      QualifiedAndroidResourceReferenceName(name),
      AndroidDataBindingReferenceName(name),

      PackageNameImpl(name),
      PackageName.DEFAULT
    ).requireIsExhaustive()
      .sortedBy { it::class.qualifiedName }
  }
}
