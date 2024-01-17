/*
 * Copyright (C) 2021-2024 Rick Busarow
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

package modulecheck.name.testing

import io.kotest.assertions.asClue
import io.kotest.assertions.assertSoftly
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.runBlocking
import modulecheck.name.AndroidDataBindingName
import modulecheck.name.AndroidRName
import modulecheck.name.AndroidResourceNameWithRName
import modulecheck.name.Name
import modulecheck.name.NameWithPackageName
import modulecheck.name.PackageName
import modulecheck.name.SimpleName
import modulecheck.name.SimpleName.Companion.asSimpleName
import modulecheck.name.SimpleName.Companion.stripPackageNameFromFqName
import modulecheck.name.UnqualifiedAndroidResourceName
import modulecheck.name.asNameWithPackageName
import modulecheck.parsing.source.JvmFile
import modulecheck.testing.assertions.TrimmedAsserts
import modulecheck.utils.lazy.LazyDeferred
import modulecheck.utils.lazy.LazySet
import modulecheck.utils.trace.Trace

interface BaseNameTest : TrimmedAsserts {

  class JvmFileBuilder {

    val names: MutableList<Name> = mutableListOf()
    val apiNames: MutableList<Name> = mutableListOf()
    val declarations: MutableList<NameWithPackageName> = mutableListOf()

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
      private val target: MutableList<Name>
    ) {

      fun androidR(packageName: PackageName = PackageName("com.test")): AndroidRName =
        AndroidRName(packageName)
          .also { target.add(it) }

      fun androidDataBinding(name: String): AndroidDataBindingName =
        AndroidDataBindingName(TODO(), TODO())
          .also { target.add(it) }

      fun qualifiedAndroidResource(name: String): AndroidResourceNameWithRName =
        AndroidResourceNameWithRName(TODO(), TODO())
          .also { target.add(it) }

      fun unqualifiedAndroidResource(name: String): UnqualifiedAndroidResourceName =
        UnqualifiedAndroidResourceName.layout(name.asSimpleName())
          .also { target.add(it) }

      fun kotlin(name: String): Name = name.asSimpleName().also { target.add(it) }

      fun java(name: String): Name = name.asSimpleName().also { target.add(it) }
    }

    inner class NormalReferenceBuilder : ReferenceBuilder(names)

    inner class ApiReferenceBuilder : ReferenceBuilder(apiNames)

    inner class DeclarationsBuilder {
      fun kotlin(
        name: String,
        packageName: PackageName = PackageName("com.subject")
      ): NameWithPackageName = NameWithPackageName(
        packageName,
        name.stripPackageNameFromFqName(packageName)
      )
        .also { declarations.add(it) }

      fun java(
        name: String,
        packageName: PackageName = PackageName("com.subject")
      ): NameWithPackageName = NameWithPackageName(
        packageName,
        name.stripPackageNameFromFqName(packageName)
      )
        .also { declarations.add(it) }

      fun agnostic(
        name: String,
        packageName: PackageName = PackageName("com.subject")
      ): NameWithPackageName = name.stripPackageNameFromFqName(packageName)
        .asNameWithPackageName(packageName)
        .also { declarations.add(it) }
    }
  }

  infix fun JvmFile.shouldBeJvmFile(config: JvmFileBuilder.() -> Unit) {

    val other = JvmFileBuilder().also { it.config() }

    assertSoftly {
      "references".asClue {
        references shouldBe other.names
      }
      "api references".asClue {
        apiReferences shouldBe other.apiNames
      }
      "declarations".asClue {
        declarations shouldBe other.declarations
      }
    }
  }

  infix fun Collection<NameWithPackageName>.shouldBe(other: Collection<NameWithPackageName>) {
    prettyPrint().trimmedShouldBe(other.prettyPrint(), BaseNameTest::class)
  }

  infix fun LazySet<Name>.shouldBe(other: Collection<Name>) {
    runBlocking(Trace.start(BaseNameTest::class)) {
      toList()
        .distinct()
        .prettyPrint().trimmedShouldBe(other.prettyPrint(), BaseNameTest::class)
    }
  }

  infix fun LazyDeferred<Set<Name>>.shouldBe(other: Collection<Name>) {
    runBlocking(Trace.start(BaseNameTest::class)) {
      await()
        .distinct()
        .prettyPrint().trimmedShouldBe(other.prettyPrint(), BaseNameTest::class)
    }
  }

  infix fun List<LazySet.DataSource<Name>>.shouldBe(other: Collection<Name>) {
    runBlocking(Trace.start(BaseNameTest::class)) {
      flatMap { it.get() }
        .distinct()
        .prettyPrint()
        .trimmedShouldBe(other.prettyPrint(), BaseNameTest::class)
    }
  }

  fun kotlin(
    name: String,
    packageName: PackageName = PackageName("com.subject")
  ): NameWithPackageName =
    name.stripPackageNameFromFqName(packageName).asNameWithPackageName(packageName)

  fun java(
    name: String,
    packageName: PackageName = PackageName("com.subject")
  ): NameWithPackageName =
    name.stripPackageNameFromFqName(packageName).asNameWithPackageName(packageName)

  fun agnostic(
    name: String,
    packageName: PackageName = PackageName("com.subject")
  ): NameWithPackageName =
    name.stripPackageNameFromFqName(packageName).asNameWithPackageName(packageName)

  fun androidR(packageName: PackageName = PackageName("com.test")): AndroidRName =
    AndroidRName(packageName)
}

fun Collection<Name>.prettyPrint(): String = asSequence()
  .map { name ->
    val typeName = when (name) {
      // references
      is UnqualifiedAndroidResourceName -> "unqualifiedAndroidResource"
      is AndroidRName -> "androidR"
      is AndroidResourceNameWithRName -> "qualifiedAndroidResource"
      is AndroidDataBindingName -> "androidDataBinding"
      // is Name -> when {
      //   Name.isJava() -> "java"
      //   Name.isKotlin() -> "kotlin"
      //   Name.isXml() -> "xml"
      //   else -> throw IllegalArgumentException("???")
      // }

      // is AndroidRDeclaredName -> "androidR"
      // is UnqualifiedAndroidResource -> Name.prefix.name
      // is QualifiedAndroidResourceDeclaredName -> "qualifiedAndroidResource"
      // is AndroidDataBindingDeclaredName -> "androidDataBinding"

      // declarations
      is NameWithPackageName -> {
        when {
          // Name.languages.containsAll(setOf(KOTLIN, JAVA)) -> "agnostic"
          // Name.languages.contains(KOTLIN) -> "kotlin"
          // Name.languages.contains(JAVA) -> "java"
          // Name.languages.contains(XML) -> "xml"
          else -> "throw IllegalArgumentException(???)"
        }
      }
      // package
      is PackageName -> "packageName"
      is SimpleName -> "simpleName"
    }
    typeName to name
  }
  .groupBy { it.first }
  .toList()
  .sortedBy { it.first }
  .joinToString("\n") { (typeName, pairs) ->

    pairs.map { it.second }
      .sortedBy { it.asString }
      .joinToString("\n", "$typeName {\n", "\n}") { "\t${it.asString}" }
  }
