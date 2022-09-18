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

import io.kotest.assertions.asClue
import io.kotest.assertions.assertSoftly
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.runBlocking
import modulecheck.parsing.element.McElement
import modulecheck.parsing.source.AndroidDataBindingDeclaredName
import modulecheck.parsing.source.AndroidDataBindingReferenceName
import modulecheck.parsing.source.AndroidRDeclaredName
import modulecheck.parsing.source.AndroidRReferenceName
import modulecheck.parsing.source.DeclaredName
import modulecheck.parsing.source.JvmFile
import modulecheck.parsing.source.McName
import modulecheck.parsing.source.McName.CompatibleLanguage
import modulecheck.parsing.source.McName.CompatibleLanguage.JAVA
import modulecheck.parsing.source.McName.CompatibleLanguage.KOTLIN
import modulecheck.parsing.source.McName.CompatibleLanguage.XML
import modulecheck.parsing.source.PackageName
import modulecheck.parsing.source.QualifiedAndroidResourceDeclaredName
import modulecheck.parsing.source.QualifiedAndroidResourceReferenceName
import modulecheck.parsing.source.QualifiedDeclaredName
import modulecheck.parsing.source.ReferenceName
import modulecheck.parsing.source.ReferenceName.Companion.asReferenceName
import modulecheck.parsing.source.SimpleName.Companion.stripPackageNameFromFqName
import modulecheck.parsing.source.UnqualifiedAndroidResource
import modulecheck.parsing.source.UnqualifiedAndroidResourceReferenceName
import modulecheck.parsing.source.asDeclaredName
import modulecheck.testing.FancyShould
import modulecheck.testing.trimmedShouldBe
import modulecheck.utils.lazy.LazyDeferred
import modulecheck.utils.lazy.LazySet
import modulecheck.utils.trace.Trace

interface McNameTest : FancyShould {

  val defaultLanguage: CompatibleLanguage

  class JvmFileBuilder {

    val referenceNames = mutableListOf<ReferenceName>()
    val apiReferenceNames = mutableListOf<ReferenceName>()
    val declarations = mutableListOf<QualifiedDeclaredName>()

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

      fun androidR(packageName: PackageName = PackageName("com.test")) =
        AndroidRReferenceName(packageName, XML)
          .also { target.add(it) }

      fun androidDataBinding(name: String) =
        AndroidDataBindingReferenceName(name, XML)
          .also { target.add(it) }

      fun qualifiedAndroidResource(name: String) =
        QualifiedAndroidResourceReferenceName(name, XML)
          .also { target.add(it) }

      fun unqualifiedAndroidResource(name: String) =
        UnqualifiedAndroidResourceReferenceName(name, XML)
          .also { target.add(it) }

      fun kotlin(name: String) = ReferenceName(name, KOTLIN)
        .also { target.add(it) }

      fun java(name: String) = ReferenceName(name, JAVA)
        .also { target.add(it) }
    }

    inner class NormalReferenceBuilder : ReferenceBuilder(referenceNames)

    inner class ApiReferenceBuilder : ReferenceBuilder(apiReferenceNames)

    inner class DeclarationsBuilder {
      fun kotlin(name: String, packageName: PackageName = PackageName("com.subject")) =
        DeclaredName.kotlin(
          packageName, name.stripPackageNameFromFqName(packageName)
        )
          .also { declarations.add(it) }

      fun java(name: String, packageName: PackageName = PackageName("com.subject")) =
        DeclaredName.java(
          packageName, name.stripPackageNameFromFqName(packageName)
        )
          .also { declarations.add(it) }

      fun agnostic(name: String, packageName: PackageName = PackageName("com.subject")) =
        name.stripPackageNameFromFqName(packageName)
          .asDeclaredName(packageName)
          .also { declarations.add(it) }
    }
  }

  infix fun JvmFile.shouldBe(config: JvmFileBuilder.() -> Unit) {

    val other = JvmFileBuilder().also { it.config() }

    assertSoftly {
      "references".asClue {
        references shouldBe other.referenceNames
      }
      "api references".asClue {
        apiReferences shouldBe other.apiReferenceNames
      }
      "declarations".asClue {
        declarations shouldBe other.declarations
      }
    }
  }

  infix fun Collection<QualifiedDeclaredName>.shouldBe(other: Collection<QualifiedDeclaredName>) {
    prettyPrint().trimmedShouldBe(other.prettyPrint(), McNameTest::class)
  }

  infix fun LazySet<McElement>.shouldBe(other: List<McElement>) {
    runBlocking(Trace.start(McNameTest::class)) {
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

  fun kotlin(name: String, packageName: PackageName = PackageName("com.test")) =
    DeclaredName.kotlin(packageName, name.stripPackageNameFromFqName(packageName))

  fun java(name: String, packageName: PackageName = PackageName("com.test")) =
    DeclaredName.java(packageName, name.stripPackageNameFromFqName(packageName))

  fun agnostic(name: String, packageName: PackageName = PackageName("com.test")) =
    name.stripPackageNameFromFqName(packageName).asDeclaredName(packageName)

  fun String.asReferenceName() = this@asReferenceName.asReferenceName(defaultLanguage)

  fun androidR(packageName: PackageName = PackageName("com.test")) =
    AndroidRReferenceName(packageName, defaultLanguage)

  fun androidDataBinding(name: String) =
    AndroidDataBindingReferenceName(name, defaultLanguage)

  fun qualifiedAndroidResource(name: String) =
    QualifiedAndroidResourceReferenceName(name, defaultLanguage)

  fun unqualifiedAndroidResource(name: String) =
    UnqualifiedAndroidResourceReferenceName(name, defaultLanguage)
}

fun Collection<McName>.prettyPrint() = map { mcName ->
  val typeName = when (mcName) {
    // references
    is UnqualifiedAndroidResourceReferenceName -> "unqualifiedAndroidResource"
    is AndroidRReferenceName -> "androidR"
    is QualifiedAndroidResourceReferenceName -> "qualifiedAndroidResource"
    is AndroidDataBindingReferenceName -> "androidDataBinding"
    is ReferenceName -> when {
      mcName.isJava() -> "java"
      mcName.isKotlin() -> "kotlin"
      mcName.isXml() -> "xml"
      else -> throw IllegalArgumentException("???")
    }

    is AndroidRDeclaredName -> "androidR"
    is UnqualifiedAndroidResource -> mcName.prefix.name
    is QualifiedAndroidResourceDeclaredName -> "qualifiedAndroidResource"
    is AndroidDataBindingDeclaredName -> "androidDataBinding"

    // declarations
    is QualifiedDeclaredName -> {
      when {
        mcName.languages.containsAll(setOf(KOTLIN, JAVA)) -> "agnostic"
        mcName.languages.contains(KOTLIN) -> "kotlin"
        mcName.languages.contains(JAVA) -> "java"
        mcName.languages.contains(XML) -> "xml"
        else -> throw IllegalArgumentException("???")
      }
    }
    // package
    is PackageName -> "packageName"
  }
  typeName to mcName
}
  .groupBy { it.first }
  .toList()
  .sortedBy { it.first }
  .joinToString("\n") { (typeName, pairs) ->

    pairs.map { it.second }
      .sortedBy { it.name }
      .joinToString("\n", "$typeName {\n", "\n}") { "\t${it.name}" }
  }
