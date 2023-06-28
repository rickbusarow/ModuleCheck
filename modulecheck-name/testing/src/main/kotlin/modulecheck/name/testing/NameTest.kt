/*
 * Copyright (C) 2021-2023 Rick Busarow
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
import modulecheck.name.PackageName.Companion.asPackageName
import modulecheck.name.SimpleName
import modulecheck.name.SimpleName.Companion.asSimpleName
import modulecheck.name.SimpleName.Companion.stripPackageNameFromFqName
import modulecheck.name.TypeParameter
import modulecheck.name.UnqualifiedAndroidResourceName
import modulecheck.name.asNameWithPackageName
import modulecheck.parsing.source.JvmFile
import modulecheck.testing.assert.TrimmedAsserts
import modulecheck.testing.assert.requireNotNullOrFail
import modulecheck.utils.lazy.LazyDeferred
import modulecheck.utils.lazy.LazySet
import modulecheck.utils.trace.Trace

/**
 * Interface for testing [Name] instances. It provides a set of helper
 * methods and builders for creating and comparing [Name] instances.
 */
interface NameTest : TrimmedAsserts {

  /** Builder for creating a [JvmFile] instance for testing. */
  class JvmFileBuilder {

    /** all names in the file */
    val names: MutableList<Name> = mutableListOf()

    /** all names in the file which are part of its public API */
    val apiNames: MutableList<Name> = mutableListOf()

    /** all named declarations in the file */
    val declarations: MutableList<NameWithPackageName> = mutableListOf()

    /**
     * Adds normal references to the [JvmFile] being built.
     *
     * @param builder The builder function for adding references.
     */
    fun references(builder: NormalReferenceBuilder.() -> Unit) {
      NormalReferenceBuilder().builder()
    }

    /**
     * Adds API references to the [JvmFile] being built.
     *
     * @param builder The builder function for adding API references.
     */
    fun apiReferences(builder: ApiReferenceBuilder.() -> Unit) {
      ApiReferenceBuilder().builder()
    }

    /**
     * Adds declarations to the [JvmFile] being built.
     *
     * @param builder The builder function for adding declarations.
     */
    fun declarations(builder: DeclarationsBuilder.() -> Unit) {
      DeclarationsBuilder().builder()
    }

    /**
     * Base class for building references.
     *
     * @property target The target list to add the references to.
     */
    open class ReferenceBuilder(
      private val target: MutableList<Name>
    ) {

      /**
       * Adds an [AndroidRName] to the target list.
       *
       * @param packageName The package name of the Android R class.
       * @return The created [AndroidRName].
       */
      fun androidR(packageName: PackageName = PackageName("com.test")): AndroidRName =
        AndroidRName(packageName)
          .also { target.add(it) }

      /**
       * Adds an [AndroidDataBindingName] to the target list.
       *
       * @param packageName ex: `com.example.library`
       * @param layoutSimpleName ex: `activity_main`
       * @return The created [AndroidDataBindingName].
       */
      fun androidDataBinding(
        packageName: String,
        layoutSimpleName: String
      ): AndroidDataBindingName = AndroidDataBindingName(
        packageName = packageName.asPackageName(),
        sourceLayout = UnqualifiedAndroidResourceName.layout(layoutSimpleName.asSimpleName())
      )
        .apply { target.add(this) }

      /**
       * Adds a qualified [AndroidResourceNameWithRName] to the target list.
       *
       * @param packageName ex: `com.example.library`
       * @param resourceName ex: `layout.activity_main`
       * @return The created [AndroidResourceNameWithRName].
       */
      fun qualifiedAndroidResource(
        packageName: String,
        resourceName: String
      ): AndroidResourceNameWithRName {
        val (type, simple) = resourceName.split('.')
        return AndroidResourceNameWithRName(
          androidRName = AndroidRName(packageName.asPackageName()),
          resourceName = UnqualifiedAndroidResourceName.fromValuePair(type, simple)
            .requireNotNullOrFail { "no resource is declared for $resourceName" }
        )
          .apply { target.add(this) }
      }

      /**
       * Adds an unqualified [UnqualifiedAndroidResourceName] to the target list.
       *
       * @param name The name of the Android resource.
       * @return The created [UnqualifiedAndroidResourceName].
       */
      fun unqualifiedAndroidResource(name: String): UnqualifiedAndroidResourceName =
        UnqualifiedAndroidResourceName.layout(name.asSimpleName())
          .also { target.add(it) }

      /**
       * Adds a Kotlin [Name] to the target list.
       *
       * @param name The name of the Kotlin class.
       * @return The created [Name].
       */
      fun kotlin(name: String): Name = name.asSimpleName().also { target.add(it) }

      /**
       * Adds a Java [Name] to the target list.
       *
       * @param name The name of the Java class.
       * @return The created [Name].
       */
      fun java(name: String): Name = name.asSimpleName().also { target.add(it) }
    }

    /** Builder for normal references. */
    inner class NormalReferenceBuilder : ReferenceBuilder(names)

    /** Builder for API references. */
    inner class ApiReferenceBuilder : ReferenceBuilder(apiNames)

    /** Builder for declarations. */
    inner class DeclarationsBuilder {
      /**
       * Adds a Kotlin [NameWithPackageName] to the declarations list.
       *
       * @param name The name of the Kotlin class.
       * @param packageName The package name of the Kotlin class.
       * @return The created [NameWithPackageName].
       */
      fun kotlin(
        name: String,
        packageName: PackageName = PackageName("com.subject")
      ): NameWithPackageName = NameWithPackageName(
        packageName,
        name.stripPackageNameFromFqName(packageName)
      )
        .also { declarations.add(it) }

      /**
       * Adds a Java [NameWithPackageName] to the declarations list.
       *
       * @param name The name of the Java class.
       * @param packageName The package name of the Java class.
       * @return The created [NameWithPackageName].
       */
      fun java(
        name: String,
        packageName: PackageName = PackageName("com.subject")
      ): NameWithPackageName = NameWithPackageName(
        packageName,
        name.stripPackageNameFromFqName(packageName)
      )
        .also { declarations.add(it) }

      /**
       * Adds an agnostic [NameWithPackageName] to the declarations list.
       *
       * @param name The name of the class.
       * @param packageName The package name of the class.
       * @return The created [NameWithPackageName].
       */
      fun agnostic(
        name: String,
        packageName: PackageName = PackageName("com.subject")
      ): NameWithPackageName = name.stripPackageNameFromFqName(packageName)
        .asNameWithPackageName(packageName)
        .also { declarations.add(it) }
    }
  }

  /**
   * Asserts that the receiver [JvmFile] is equal to
   * the [JvmFile] built by the provided configuration.
   *
   * @param config The configuration for building the [JvmFile] to compare with.
   */
  infix fun JvmFile.shouldBeJvmFile(config: JvmFileBuilder.() -> Unit) {

    val other = JvmFileBuilder().apply(config)

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

  /**
   * Asserts that the receiver collection of [NameWithPackageName] is equal to the other collection.
   *
   * @param other The other collection of [NameWithPackageName] to compare with.
   */
  infix fun Collection<NameWithPackageName>.shouldBe(other: Collection<NameWithPackageName>) {
    prettyPrint().trimmedShouldBe(other.prettyPrint(), NameTest::class)
  }

  /**
   * Asserts that the receiver [LazySet] of [Name] is equal to the other collection.
   *
   * @param other The other collection of [Name] to compare with.
   */
  infix fun LazySet<Name>.shouldBe(other: Collection<Name>) {
    runBlocking(Trace.start(NameTest::class)) {
      toList()
        .distinct()
        .prettyPrint().trimmedShouldBe(other.prettyPrint(), NameTest::class)
    }
  }

  /**
   * Asserts that the receiver [LazyDeferred] of [Set] of [Name] is equal to the other collection.
   *
   * @param other The other collection of [Name] to compare with.
   */
  infix fun LazyDeferred<Set<Name>>.shouldBe(other: Collection<Name>) {
    runBlocking(Trace.start(NameTest::class)) {
      await()
        .distinct()
        .prettyPrint().trimmedShouldBe(other.prettyPrint(), NameTest::class)
    }
  }

  /**
   * Asserts that the receiver list of [LazySet.DataSource]
   * of [Name] is equal to the other collection.
   *
   * @param other The other collection of [Name] to compare with.
   */
  infix fun List<LazySet.DataSource<Name>>.shouldBe(other: Collection<Name>) {
    runBlocking(Trace.start(NameTest::class)) {
      flatMap { it.get() }
        .distinct()
        .prettyPrint()
        .trimmedShouldBe(other.prettyPrint(), NameTest::class)
    }
  }

  /**
   * Creates a Kotlin [NameWithPackageName] with the provided name and package name.
   *
   * @param name The name of the Kotlin class.
   * @param packageName The package name of the Kotlin class.
   * @return The created [NameWithPackageName].
   */
  fun kotlin(
    name: String,
    packageName: PackageName = PackageName("com.subject")
  ): NameWithPackageName =
    name.stripPackageNameFromFqName(packageName).asNameWithPackageName(packageName)

  /**
   * Creates a Java [NameWithPackageName] with the provided name and package name.
   *
   * @param name The name of the Java class.
   * @param packageName The package name of the Java class.
   * @return The created [NameWithPackageName].
   */
  fun java(
    name: String,
    packageName: PackageName = PackageName("com.subject")
  ): NameWithPackageName =
    name.stripPackageNameFromFqName(packageName).asNameWithPackageName(packageName)

  /**
   * Creates an agnostic [NameWithPackageName] with the provided name and package name.
   *
   * @param name The name of the class.
   * @param packageName The package name of the```kotlin
   *   class. @return The created [NameWithPackageName].
   */
  fun agnostic(
    name: String,
    packageName: PackageName = PackageName("com.subject")
  ): NameWithPackageName =
    name.stripPackageNameFromFqName(packageName).asNameWithPackageName(packageName)

  /**
   * Creates an [AndroidRName] with the provided package name.
   *
   * @param packageName The package name of the Android R class.
   * @return The created [AndroidRName].
   */
  fun androidR(packageName: PackageName = PackageName("com.test")): AndroidRName =
    AndroidRName(packageName)
}

/**
 * Pretty prints a collection of [Name] instances.
 *
 * @return The pretty printed string.
 */
fun Collection<Name>.prettyPrint(): String = asSequence()
  .map { name ->
    val typeName = when (name) {
      // references
      is UnqualifiedAndroidResourceName -> "unqualifiedAndroidResource"
      is AndroidRName -> "androidR"
      is AndroidResourceNameWithRName -> "qualifiedAndroidResource"
      is AndroidDataBindingName -> "androidDataBinding"
      is TypeParameter -> "typeParameter"
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
