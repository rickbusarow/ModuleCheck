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

package modulecheck.parsing.test

import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.runBlocking
import modulecheck.parsing.source.AgnosticDeclaredName
import modulecheck.parsing.source.AndroidDataBindingDeclaredName
import modulecheck.parsing.source.AndroidDataBindingReference
import modulecheck.parsing.source.AndroidRDeclaredName
import modulecheck.parsing.source.AndroidRReference
import modulecheck.parsing.source.DeclaredName
import modulecheck.parsing.source.GeneratedAndroidResourceDeclaredName
import modulecheck.parsing.source.JavaSpecificDeclaredName
import modulecheck.parsing.source.KotlinSpecificDeclaredName
import modulecheck.parsing.source.NamedSymbol
import modulecheck.parsing.source.QualifiedAndroidResourceReference
import modulecheck.parsing.source.Reference
import modulecheck.parsing.source.Reference.ExplicitJavaReference
import modulecheck.parsing.source.Reference.ExplicitKotlinReference
import modulecheck.parsing.source.Reference.ExplicitXmlReference
import modulecheck.parsing.source.Reference.InterpretedJavaReference
import modulecheck.parsing.source.Reference.InterpretedKotlinReference
import modulecheck.parsing.source.UnqualifiedAndroidResourceDeclaredName
import modulecheck.parsing.source.UnqualifiedAndroidResourceReference
import modulecheck.parsing.source.asDeclaredName
import modulecheck.parsing.source.asJavaDeclaredName
import modulecheck.parsing.source.asKotlinDeclaredName
import modulecheck.testing.FancyShould
import modulecheck.testing.trimmedShouldBe
import modulecheck.utils.LazyDeferred
import modulecheck.utils.LazySet

interface NamedSymbolTest : FancyShould {

  infix fun Collection<DeclaredName>.shouldBe(other: Collection<DeclaredName>) {
    prettyPrint().trimmedShouldBe(other.prettyPrint(), NamedSymbolTest::class)
  }

  infix fun LazySet<Reference>.shouldBe(other: Collection<Reference>) {
    runBlocking {
      toList()
        .distinct()
        .prettyPrint().trimmedShouldBe(other.prettyPrint(), NamedSymbolTest::class)
    }
  }

  infix fun LazyDeferred<Set<Reference>>.shouldBe(other: Collection<Reference>) {
    runBlocking {
      await()
        .distinct()
        .prettyPrint().trimmedShouldBe(other.prettyPrint(), NamedSymbolTest::class)
    }
  }

  infix fun List<LazySet.DataSource<Reference>>.shouldBe(other: Collection<Reference>) {
    runBlocking {
      flatMap { it.get() }
        .distinct()
        .prettyPrint().trimmedShouldBe(other.prettyPrint(), NamedSymbolTest::class)
    }
  }

  fun kotlin(name: String) = name.asKotlinDeclaredName()

  fun java(name: String) = name.asJavaDeclaredName()

  fun agnostic(name: String) = name.asDeclaredName()

  fun androidR(name: String) = AndroidRReference(name)
  fun androidDataBinding(name: String) = AndroidDataBindingReference(name)
  fun qualifiedAndroidResource(name: String) = QualifiedAndroidResourceReference(name)
  fun unqualifiedAndroidResource(name: String) = UnqualifiedAndroidResourceReference(name)
}

@Suppress("ComplexMethod")
fun Collection<NamedSymbol>.prettyPrint() = groupBy { it::class }
  .toList()
  .sortedBy { it.first.qualifiedName }
  .joinToString("\n") { (_, names) ->
    val name = when (val symbol = names.first()) {
      // declarations
      is ExplicitJavaReference -> "explicitJava"
      is ExplicitKotlinReference -> "explicitKotlin"
      is ExplicitXmlReference -> "explicitXml"
      is InterpretedJavaReference -> "interpretedJava"
      is InterpretedKotlinReference -> "interpretedKotlin"
      is UnqualifiedAndroidResourceReference -> "unqualifiedAndroidResource"
      is AndroidRReference -> "androidR"
      is QualifiedAndroidResourceReference -> "qualifiedAndroidResource"
      is AndroidDataBindingReference -> "androidDataBinding"
      // references
      is AgnosticDeclaredName -> "agnostic"
      is AndroidRDeclaredName -> "androidR"
      is JavaSpecificDeclaredName -> "java"
      is KotlinSpecificDeclaredName -> "kotlin"
      is UnqualifiedAndroidResourceDeclaredName -> symbol.prefix
      is GeneratedAndroidResourceDeclaredName -> "qualifiedAndroidResource"
      is AndroidDataBindingDeclaredName -> "androidDataBinding"
    }
    names
      .sortedBy { it.name }
      .joinToString("\n", "$name {\n", "\n}") { "\t${it.name}" }
  }
