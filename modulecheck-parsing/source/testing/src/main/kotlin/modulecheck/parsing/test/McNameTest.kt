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
import modulecheck.parsing.element.McElement
import modulecheck.parsing.source.AgnosticDeclaredName
import modulecheck.parsing.source.AndroidDataBindingDeclaredName
import modulecheck.parsing.source.AndroidDataBindingReferenceName
import modulecheck.parsing.source.AndroidRDeclaredName
import modulecheck.parsing.source.AndroidRReferenceName
import modulecheck.parsing.source.DeclaredName
import modulecheck.parsing.source.GeneratedAndroidResourceDeclaredName
import modulecheck.parsing.source.JavaSpecificDeclaredName
import modulecheck.parsing.source.JvmFile
import modulecheck.parsing.source.KotlinSpecificDeclaredName
import modulecheck.parsing.source.McName
import modulecheck.parsing.source.PackageName
import modulecheck.parsing.source.QualifiedAndroidResourceReferenceName
import modulecheck.parsing.source.ReferenceName
import modulecheck.parsing.source.ReferenceName.JavaReferenceNameImpl
import modulecheck.parsing.source.ReferenceName.KotlinReferenceNameImpl
import modulecheck.parsing.source.ReferenceName.XmlReferenceNameImpl
import modulecheck.parsing.source.UnqualifiedAndroidResourceDeclaredName
import modulecheck.parsing.source.UnqualifiedAndroidResourceReferenceName
import modulecheck.parsing.source.asDeclaredName
import modulecheck.parsing.source.asJavaDeclaredName
import modulecheck.parsing.source.asJavaReference
import modulecheck.parsing.source.asKotlinDeclaredName
import modulecheck.parsing.source.asKotlinReference
import modulecheck.testing.FancyShould
import modulecheck.testing.trimmedShouldBe
import modulecheck.utils.lazy.LazyDeferred
import modulecheck.utils.lazy.LazySet
import modulecheck.utils.trace.Trace

interface McNameTest : FancyShould {

  class JvmFileBuilder {

    val referenceNames = mutableListOf<ReferenceName>()
    val apiReferenceNames = mutableListOf<ReferenceName>()
    val declarations = mutableListOf<DeclaredName>()

    fun references(builder: NormalReferenceBuilder.() -> Unit) {
      NormalReferenceBuilder().builder()
    }

    fun apiReferences(builder: ApiReferenceBuilder.() -> Unit) {
      ApiReferenceBuilder().builder()
    }

    fun declarations(builder: DeclarationsBuilder.() -> Unit) {
      DeclarationsBuilder().builder()
    }

    open class ReferenceBuilder(
      private val target: MutableList<ReferenceName>
    ) {

      fun androidR(name: String) = AndroidRReferenceName(name)
        .also { target.add(it) }

      fun androidDataBinding(name: String) =
        AndroidDataBindingReferenceName(name)
          .also { target.add(it) }

      fun qualifiedAndroidResource(name: String) =
        QualifiedAndroidResourceReferenceName(name)
          .also { target.add(it) }

      fun unqualifiedAndroidResource(name: String) =
        UnqualifiedAndroidResourceReferenceName(name)
          .also { target.add(it) }

      fun explicitKotlin(name: String) = name.asKotlinReference()
        .also { target.add(it) }

      fun interpretedKotlin(name: String) = name.asKotlinReference()
        .also { target.add(it) }

      fun explicitJava(name: String) = name.asJavaReference()
        .also { target.add(it) }

      fun interpretedJava(name: String) = name.asJavaReference()
        .also { target.add(it) }
    }

    inner class NormalReferenceBuilder : ReferenceBuilder(referenceNames)

    inner class ApiReferenceBuilder : ReferenceBuilder(apiReferenceNames)

    inner class DeclarationsBuilder {
      fun kotlin(name: String, packageName: String = "com.subject") =
        name.asKotlinDeclaredName(PackageName(packageName))
          .also { declarations.add(it) }

      fun java(name: String, packageName: String = "com.subject") =
        name.asJavaDeclaredName(PackageName(packageName))
          .also { declarations.add(it) }

      fun agnostic(name: String, packageName: String = "com.subject") =
        name.asDeclaredName(PackageName(packageName))
          .also { declarations.add(it) }
    }
  }

  infix fun JvmFile.shouldBe(config: JvmFileBuilder.() -> Unit) {

    val other = JvmFileBuilder().also { it.config() }

    references shouldBe other.referenceNames
    apiReferences shouldBe other.apiReferenceNames
    declarations shouldBe other.declarations
  }

  infix fun Collection<DeclaredName>.shouldBe(other: Collection<DeclaredName>) {
    prettyPrint().trimmedShouldBe(other.prettyPrint(), McNameTest::class)
  }

  infix fun LazySet<McElement>.shouldBe(other: List<McElement>) {
    runBlocking(Trace.start(NamedSymbolTest::class)) {
      toList().trimmedShouldBe(other.toList())
    }
  }

  infix fun LazySet<ReferenceName>.shouldBe(other: Collection<ReferenceName>) {
    runBlocking(Trace.start(McNameTest::class)) {
      toList()
        .distinct()
        .prettyPrint().trimmedShouldBe(other.prettyPrint(), McNameTest::class)
    }
  }

  infix fun LazyDeferred<Set<ReferenceName>>.shouldBe(other: Collection<ReferenceName>) {
    runBlocking(Trace.start(McNameTest::class)) {
      await()
        .distinct()
        .prettyPrint().trimmedShouldBe(other.prettyPrint(), McNameTest::class)
    }
  }

  infix fun List<LazySet.DataSource<ReferenceName>>.shouldBe(other: Collection<ReferenceName>) {
    runBlocking(Trace.start(McNameTest::class)) {
      flatMap { it.get() }
        .distinct()
        .prettyPrint()
        .trimmedShouldBe(other.prettyPrint(), McNameTest::class)
    }
  }

  fun kotlin(name: String, packageName: String = "com.subject") =
    name.asKotlinDeclaredName(PackageName(packageName))

  fun java(name: String, packageName: String = "com.subject") =
    name.asJavaDeclaredName(PackageName(packageName))

  fun agnostic(name: String, packageName: String = "com.subject") =
    name.asDeclaredName(PackageName(packageName))

  fun androidR(name: String) = AndroidRReferenceName(name)
  fun androidDataBinding(name: String) = AndroidDataBindingReferenceName(name)
  fun qualifiedAndroidResource(name: String) = QualifiedAndroidResourceReferenceName(name)
  fun unqualifiedAndroidResource(name: String) = UnqualifiedAndroidResourceReferenceName(name)
}

fun Collection<McName>.prettyPrint() = groupBy { it::class }
  .toList()
  .sortedBy { it.first.qualifiedName }
  .joinToString("\n") { (_, names) ->
    val typeName = when (val mcName = names.first()) {
      // declarations
      is JavaReferenceNameImpl -> "java"
      is KotlinReferenceNameImpl -> "kotlin"
      is XmlReferenceNameImpl -> "xml"
      is UnqualifiedAndroidResourceReferenceName -> "unqualifiedAndroidResource"
      is AndroidRReferenceName -> "androidR"
      is QualifiedAndroidResourceReferenceName -> "qualifiedAndroidResource"
      is AndroidDataBindingReferenceName -> "androidDataBinding"
      // references
      is AgnosticDeclaredName -> "agnostic"
      is AndroidRDeclaredName -> "androidR"
      is JavaSpecificDeclaredName -> "java"
      is KotlinSpecificDeclaredName -> "kotlin"
      is UnqualifiedAndroidResourceDeclaredName -> mcName.prefix
      is GeneratedAndroidResourceDeclaredName -> "qualifiedAndroidResource"
      is AndroidDataBindingDeclaredName -> "androidDataBinding"
      // package
      is PackageName -> "packageName"
    }
    names
      .sortedBy { it.name }
      .joinToString("\n", "$typeName {\n", "\n}") { "\t${it.name}" }
  }
