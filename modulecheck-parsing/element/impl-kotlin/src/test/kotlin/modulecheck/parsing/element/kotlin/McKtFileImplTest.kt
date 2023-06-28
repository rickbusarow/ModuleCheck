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

package modulecheck.parsing.element.kotlin

import kotlinx.coroutines.flow.filterIsInstance
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.toList
import modulecheck.api.context.jvmFiles
import modulecheck.model.dependency.ConfigurationName
import modulecheck.model.dependency.ProjectPath
import modulecheck.model.sourceset.SourceSetName
import modulecheck.name.ClassName
import modulecheck.parsing.element.McProperty.McKtProperty
import modulecheck.parsing.element.McType.McConcreteType.McKtConcreteType
import modulecheck.parsing.element.kotlin.McKtFileImplTest.SubjectBuilder.SubjectParams
import modulecheck.parsing.element.resolve.ConcatenatingParsingInterceptor2
import modulecheck.parsing.element.resolve.ImportAliasUnwrappingParsingInterceptor2
import modulecheck.parsing.element.resolve.McElementContext
import modulecheck.parsing.element.resolve.ParsingChain2
import modulecheck.parsing.psi.RealKotlinFile
import modulecheck.parsing.psi.internal.PsiElementResolver
import modulecheck.parsing.psi.internal.file
import modulecheck.parsing.psi.kotlinStdLibNameOrNull
import modulecheck.parsing.source.McName.CompatibleLanguage.KOTLIN
import modulecheck.parsing.source.ReferenceName
import modulecheck.parsing.test.McNameTest
import modulecheck.parsing.wiring.RealAndroidDataBindingNameProvider
import modulecheck.parsing.wiring.RealAndroidRNameProvider
import modulecheck.parsing.wiring.RealDeclarationsProvider
import modulecheck.project.McProject
import modulecheck.project.generation.ProjectCollector
import modulecheck.project.test.ProjectTest
import modulecheck.project.test.ProjectTestEnvironment
import modulecheck.testing.HasTestEnvironment
import modulecheck.testing.SkipInStackTrace
import modulecheck.testing.asTests
import modulecheck.testing.testFactory
import modulecheck.utils.capitalize
import modulecheck.utils.check
import modulecheck.utils.joinToStringConcat
import modulecheck.utils.justifyToFirstLine
import modulecheck.utils.lazy.unsafeLazy
import modulecheck.utils.mapToSet
import modulecheck.utils.remove
import modulecheck.utils.replaceRegex
import modulecheck.utils.singletonList
import modulecheck.utils.splitAndMap
import modulecheck.utils.splitAndTrim
import modulecheck.utils.toStringPretty
import org.intellij.lang.annotations.Language
import org.junit.jupiter.api.DynamicNode
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestFactory
import java.util.stream.Stream

@Suppress("RemoveRedundantQualifierName")
internal class McKtFileImplTest : ProjectTest<ProjectTestEnvironment>(), McNameTest {

  override val defaultLanguage = KOTLIN

  val ProjectTestEnvironment.lib1: McProject
    get() = projectCache.getOrPut(ProjectPath.from(":lib1")) {
      kotlinProject(":lib1") {
        addKotlinSource(
          """
        package com.lib1

        class Lib1Class
        """
        )
      }
    }
  val ProjectTestEnvironment.project: McProject
    get() = projectCache.getOrPut(ProjectPath.from(":subject")) {
      kotlinProject(":subject") {
        addDependency(ConfigurationName.api, lib1)
      }
    }

  val McNameTest.JvmFileBuilder.ReferenceBuilder.lib1Class
    get() = kotlin("com.lib1.Lib1Class")

  private val constructorProperty = SubjectBuilder("constructor property") { params ->
    //language=kotlin
    """
    package com.subject

    ${params.imports.joinToString("\n") { "import $it" }}

    class SubjectClass(
      val subjectProp ${params.afterProperty}
    )

    ${params.additionalTypes.justifyToFirstLine()}
    """
  }

  private val classBodyProperty = SubjectBuilder("class body property") { params ->
    //language=kotlin
    """
    package com.subject

    ${params.imports.joinToString("\n") { "import $it" }}

    class SubjectClass {
      val subjectProp ${params.afterProperty}
    }

    ${params.additionalTypes.justifyToFirstLine()}
    """
  }

  private val nestedClassBodyProperty = SubjectBuilder("nested class body property") { params ->
    //language=kotlin
    """
    package com.subject

    ${params.imports.joinToString("\n") { "import $it" }}

    class Outer {
      class SubjectClass {
        val subjectProp ${params.afterProperty}
      }
    }

    ${params.additionalTypes.justifyToFirstLine()}
    """
  }

  private val explicitTypes = mapOf(
    "no-import stdlib types" to sequenceOf(
      SubjectParams(
        afterProperty = ": String",
        typeAsString = "kotlin.String",
        imports = listOf()
      ),
      SubjectParams(
        afterProperty = ": String = \"Hello World\"",
        typeAsString = "kotlin.String",
        imports = listOf()
      ),
      SubjectParams(
        afterProperty = ": Int",
        typeAsString = "kotlin.Int",
        imports = listOf()
      ),
      SubjectParams(
        afterProperty = ": Int = 3",
        typeAsString = "kotlin.Int",
        imports = listOf()
      ),
      SubjectParams(
        afterProperty = ": List<String>",
        typeAsString = "kotlin.collections.List<kotlin.String>",
        imports = listOf()
      ),
      SubjectParams(
        afterProperty = ": List<String> = emptyList()",
        typeAsString = "kotlin.collections.List<kotlin.String>",
        imports = listOf()
      )
    )
      .checkUnique()
      .importAliases(),
    "other types" to sequenceOf(
      SubjectParams(
        afterProperty = ": kotlin.String",
        typeAsString = "kotlin.String",
        imports = listOf()
      ),
      SubjectParams(
        afterProperty = ": kotlin.String = \"Hello World\"",
        typeAsString = "kotlin.String",
        imports = listOf()
      ),
      SubjectParams(
        afterProperty = ": String",
        typeAsString = "kotlin.String",
        imports = listOf("kotlin.String")
      ),
      SubjectParams(
        afterProperty = ": String = \"Hello World\"",
        typeAsString = "kotlin.String",
        imports = listOf("kotlin.String")
      ),
      SubjectParams(
        afterProperty = ": KotlinString",
        typeAsString = "kotlin.String",
        imports = listOf("kotlin.String as KotlinString")
      ),
      SubjectParams(
        afterProperty = ": KotlinString = \"Hello World\"",
        typeAsString = "kotlin.String",
        imports = listOf("kotlin.String as KotlinString")
      ),
      SubjectParams(
        afterProperty = ": KotlinString",
        typeAsString = "com.subject.KotlinString",
        imports = listOf(),
        additionalTypes = "\n      typealias KotlinString = kotlin.String\n      "
      ),
      SubjectParams(
        afterProperty = ": KotlinString = \"Hello World\"",
        typeAsString = "com.subject.KotlinString",
        imports = listOf(),
        additionalTypes = "\n      typealias KotlinString = kotlin.String\n      "
      ),
      SubjectParams(
        afterProperty = ": kotlin.Int",
        typeAsString = "kotlin.Int",
        imports = listOf()
      ),
      SubjectParams(
        afterProperty = ": kotlin.Int = 3",
        typeAsString = "kotlin.Int",
        imports = listOf()
      ),
      SubjectParams(
        afterProperty = ": Int",
        typeAsString = "kotlin.Int",
        imports = listOf("kotlin.Int")
      ),
      SubjectParams(
        afterProperty = ": Int = 3",
        typeAsString = "kotlin.Int",
        imports = listOf("kotlin.Int")
      ),
      SubjectParams(
        afterProperty = ": Lib1Class",
        typeAsString = "com.lib1.Lib1Class",
        imports = listOf("com.lib1.Lib1Class")
      ),
      SubjectParams(
        afterProperty = ": Lib1Class = Lib1Class()",
        typeAsString = "com.lib1.Lib1Class",
        imports = listOf("com.lib1.Lib1Class")
      ),
      SubjectParams(
        afterProperty = ": List<Lib1Class>",
        typeAsString = "kotlin.collections.List<com.lib1.Lib1Class>",
        imports = listOf("com.lib1.Lib1Class")
      ),
      SubjectParams(
        afterProperty = ": List<Lib1Class> = emptyList()",
        typeAsString = "kotlin.collections.List<com.lib1.Lib1Class>",
        imports = listOf("com.lib1.Lib1Class")
      ),
      SubjectParams(
        afterProperty = ": List<Lib1Class>",
        typeAsString = "kotlin.collections.List<com.lib1.Lib1Class>",
        imports = listOf("com.lib1.Lib1Class")
      )
    )
      .checkUnique()
      .importAliases()
  )

  private val inferredTypes = sequenceOf(
    SubjectParams(
      afterProperty = "= \"hello world\"",
      typeAsString = "kotlin.String",
      imports = listOf()
    ),
    SubjectParams(
      afterProperty = "get() = \"hello world\"",
      typeAsString = "kotlin.String",
      imports = listOf()
    ),
    SubjectParams(
      afterProperty = "= \"hello world\"",
      typeAsString = "kotlin.String",
      imports = listOf("kotlin.String")
    ),
    SubjectParams(
      afterProperty = "get() = \"hello world\"",
      typeAsString = "kotlin.String",
      imports = listOf("kotlin.String")
    ),
    SubjectParams(
      afterProperty = "= 3",
      typeAsString = "kotlin.Int",
      imports = listOf()
    ),
    SubjectParams(
      afterProperty = "get() = 3",
      typeAsString = "kotlin.Int",
      imports = listOf()
    ),
    SubjectParams(
      afterProperty = "= 3_00",
      typeAsString = "kotlin.Int",
      imports = listOf()
    ),
    SubjectParams(
      afterProperty = "get() = 3_00",
      typeAsString = "kotlin.Int",
      imports = listOf()
    ),
    SubjectParams(
      afterProperty = "= 3",
      typeAsString = "kotlin.Int",
      imports = listOf("kotlin.Int")
    ),
    SubjectParams(
      afterProperty = "get() = 3",
      typeAsString = "kotlin.Int",
      imports = listOf("kotlin.Int")
    ),
    SubjectParams(
      afterProperty = "= 3L",
      typeAsString = "kotlin.Long",
      imports = listOf()
    ),
    SubjectParams(
      afterProperty = "get() = 3L",
      typeAsString = "kotlin.Long",
      imports = listOf()
    ),
    SubjectParams(
      afterProperty = "= 3_00L",
      typeAsString = "kotlin.Long",
      imports = listOf()
    ),
    SubjectParams(
      afterProperty = "get() = 3_00L",
      typeAsString = "kotlin.Long",
      imports = listOf()
    ),
    SubjectParams(
      afterProperty = "= 3L",
      typeAsString = "kotlin.Long",
      imports = listOf("kotlin.Long")
    ),
    SubjectParams(
      afterProperty = "= Lib1Class()",
      typeAsString = "com.lib1.Lib1Class",
      imports = listOf("com.lib1.Lib1Class")
    ),
    SubjectParams(
      afterProperty = "get() = Lib1Class()",
      typeAsString = "com.lib1.Lib1Class",
      imports = listOf("com.lib1.Lib1Class")
    )
  )
    .checkUnique()
    .importAliases()

  private val delegateTypes = mapOf(
    "no-import stdlib types" to sequenceOf(
      SubjectParams(
        afterProperty = """by lazy<String> { "" }""",
        typeAsString = "kotlin.String",
        imports = listOf()
      ),
      SubjectParams(
        afterProperty = """by lazy<Int> { 1 }""",
        typeAsString = "kotlin.Int",
        imports = listOf()
      ),
      SubjectParams(
        afterProperty = """by lazy<List<String>> { listOf("") }""",
        typeAsString = "kotlin.collections.List<kotlin.String>",
        imports = listOf()
      ),
      SubjectParams(
        afterProperty = ": String get() = \"Hello World\"",
        typeAsString = "kotlin.String",
        imports = listOf()
      ),
      SubjectParams(
        afterProperty = ": Int get() = 3",
        typeAsString = "kotlin.Int",
        imports = listOf()
      ),
      SubjectParams(
        afterProperty = ": List<kotlin.String> = emptyList()",
        typeAsString = "kotlin.collections.List<kotlin.String>",
        imports = listOf()
      ),
      SubjectParams(
        afterProperty = "get() = emptyList<String>()",
        typeAsString = "kotlin.collections.List<kotlin.String>",
        imports = listOf()
      ),
      SubjectParams(
        afterProperty = ": List<String> get() = emptyList()",
        typeAsString = "kotlin.collections.List<kotlin.String>",
        imports = listOf()
      )
    )
      .checkUnique()
      .importAliases(),
    "other types" to sequenceOf(
      SubjectParams(
        afterProperty = """by lazy { "" }""",
        typeAsString = "kotlin.String",
        imports = listOf()
      ),
      SubjectParams(
        afterProperty = """by lazy { "" }""",
        typeAsString = "kotlin.String",
        imports = listOf("kotlin.String")
      ),
      SubjectParams(
        afterProperty = """by lazy { 3 }""",
        typeAsString = "kotlin.Int",
        imports = listOf()
      ),
      SubjectParams(
        afterProperty = """by lazy { 3_000 }""",
        typeAsString = "kotlin.Int",
        imports = listOf()
      ),
      SubjectParams(
        afterProperty = """by lazy { 3 }""",
        typeAsString = "kotlin.Int",
        imports = listOf("kotlin.Int")
      ),
      SubjectParams(
        afterProperty = """by lazy { 3L }""",
        typeAsString = "kotlin.Long",
        imports = listOf()
      ),
      SubjectParams(
        afterProperty = """by lazy { 3_000L }""",
        typeAsString = "kotlin.Long",
        imports = listOf()
      ),
      SubjectParams(
        afterProperty = """by lazy { 3L }""",
        typeAsString = "kotlin.Long",
        imports = listOf("kotlin.Long")
      ),
      SubjectParams(
        afterProperty = """by lazy { Lib1Class() }""",
        typeAsString = "com.lib1.Lib1Class",
        imports = listOf("com.lib1.Lib1Class")
      ),
      SubjectParams(
        afterProperty = ": Lib1Class get() = Lib1Class()",
        typeAsString = "com.lib1.Lib1Class",
        imports = listOf("com.lib1.Lib1Class")
      ),
      SubjectParams(
        afterProperty = ": List<Lib1Class> get() = emptyList()",
        typeAsString = "kotlin.collections.List<com.lib1.Lib1Class>",
        imports = listOf("com.lib1.Lib1Class")
      ),
      SubjectParams(
        afterProperty = ": List<Lib1Class> get() = emptyList()",
        typeAsString = "kotlin.collections.List<com.lib1.Lib1Class>",
        imports = listOf("com.lib1.Lib1Class")
      )
    )
      .checkUnique()
      .importAliases()
  )

  suspend fun McKtFileImpl.subjectClass() = declaredTypesAndInnerTypes.toList()
    .single { it.simpleNames.last().name == "SubjectClass" }

  suspend fun McKtConcreteType.property(name: String): McKtProperty {
    return properties.first { it.simplestName.name == name }
  }

  suspend fun McKtConcreteType.subjectProp(): McKtProperty {
    return property("subjectProp")
  }

  suspend fun McKtProperty.typeNameStringWithParams(): String {
    return (typeReferenceName.await() as ClassName).asStringWithTypeParameters
  }

  @Nested
  inner class `property type resolution` {

    @TestFactory
    fun `constructor property explicit types`() = explicitTypes.asTests { params ->

      val file = project.createFile(constructorProperty.build(params))

      val subjectProp = file.subjectClass().subjectProp()

      subjectProp.typeNameStringWithParams() shouldBe params.typeAsString
    }

    @TestFactory
    fun `class body explicit types`() = explicitTypes.asTests { params ->

      val file = project.createFile(classBodyProperty.build(params))

      val subjectProp = file.subjectClass().subjectProp()

      subjectProp.typeNameStringWithParams() shouldBe params.typeAsString
    }

    @TestFactory
    fun `class body inferred delegate types`() = delegateTypes.asTests { params ->

      val file = project.createFile(classBodyProperty.build(params))

      val subjectProp = file.subjectClass().subjectProp()

      subjectProp.typeNameStringWithParams() shouldBe params.typeAsString
    }

    @TestFactory
    fun `nested class body explicit types`() = explicitTypes.asTests { params ->

      val file = project.createFile(nestedClassBodyProperty.build(params))

      val subjectProp = file.subjectClass().subjectProp()

      subjectProp.typeNameStringWithParams() shouldBe params.typeAsString
    }

    @TestFactory
    fun `class body inferred types`() = inferredTypes.asTests { params ->

      val file = project.createFile(classBodyProperty.build(params))

      val subjectProp = file.subjectClass().subjectProp()

      subjectProp.typeNameStringWithParams() shouldBe params.typeAsString
    }

    @Nested
    inner class `member property type resolution` {

      @Test
      fun `member property with explicit type should have resolved type`() = test {
        val file = project.createFile(
          """
          package com.subject

          import com.lib1.Lib1Class
          import kotlin.properties.Delegates

          class SubjectClass {
            lateinit var subjectProp : Lib1Class
          }
          """
        )

        val subjectProp = file.subjectClass().subjectProp()

        subjectProp.typeReferenceName.await() shouldBe ClassName("com.lib1", "Lib1Class")
      }

      @Test
      fun `member property with explicit stdlib type should resolve`() = test {
        val file = project.createFile(
          """
          package com.subject

          import com.lib1.Lib1Class

          class SubjectClass {
            val subjectProp: String = "foo"
          }
          """
        )

        val subjectProp = file.subjectClass().subjectProp()

        subjectProp.typeReferenceName.await() shouldBe ClassName("kotlin", "String")
      }

      @Test
      fun `member property with explicit single-param stdlib generic type should resolve`() = test {
        val file = project.createFile(
          """
          package com.subject

          class SubjectClass {
            lateinit var subjectProp: List<String>
          }
          """
        )

        val subjectProp = file.subjectClass().subjectProp()

        subjectProp.typeReferenceName.await().toStringPretty() shouldBe ClassName(
          "kotlin.collections",
          "List"
        )
          .parameterizedBy(ClassName("kotlin", "String")).toStringPretty()
      }

      @Test
      fun `member property with inferred type from fully imported generic delegate should have resolved type`() =
        test {
          val file = project.createFile(
            """
            package com.subject

            import com.lib1.Lib1Class
            import kotlin.properties.Delegates.notNull

            class SubjectClass {
              var subjectProp by notNull<Lib1Class>()
            }
            """
          )

          val subjectProp = file.subjectClass().subjectProp()

          subjectProp.typeReferenceName.await() shouldBe ClassName("com.lib1", "Lib1Class")
        }

      @Test
      fun `member property with inferred type from qualified generic delegate should have resolved type`() =
        test {

          val file = project.createFile(
            """
            package com.subject

            import com.lib1.Lib1Class
            import kotlin.properties.Delegates

            class SubjectClass {
              var subjectProp by Delegates.notNull<Lib1Class>()
            }
            """
          )

          val subjectProp = file.subjectClass().subjectProp()

          subjectProp.typeReferenceName.await() shouldBe ClassName("com.lib1", "Lib1Class")
        }

      @Test
      fun `member property with nested type should have resolved type`() = test {
        val file = project.createFile(
          """
          package com.subject

          import com.lib1.Lib1Class

          class SubjectClass {
            class NestedClass
            val nested: NestedClass = NestedClass()
          }
          """
        )

        val nested = file.subjectClass().property("nested")

        nested.typeReferenceName.await() shouldBe ClassName(
          "com.subject",
          "SubjectClass",
          "NestedClass"
        )
      }

      @Test
      fun `member property with generic type with multiple type parameters should resolve`() =
        test {
          val file = project.createFile(
            """
            package com.subject

            class SubjectClass {
              val map: Map<String, Int> = mapOf()
            }
            """
          )

          val map = file.subjectClass().property("map")

          map.typeReferenceName.await() shouldBe ClassName("kotlin.collections", "Map")
            .parameterizedBy(
              ClassName("kotlin", "String"),
              ClassName("kotlin", "Int")
            )
        }

      @Test
      fun `member property with typealias type should have resolved alias type`() = test {
        val file = project.createFile(
          """
          package com.subject

          typealias StringList = List<String>

          class SubjectClass {
            val subjectProp: StringList = listOf()
          }
          """
        )

        val subjectProp = file.subjectClass().subjectProp()

        subjectProp.typeReferenceName.await() shouldBe ClassName("com.subject", "StringList")
      }

      @Test
      fun `member property with a generic typealias type should have resolved alias type`() = test {
        val file = project.createFile(
          """
          package com.subject

          typealias StringList<T: CharSequence> = List<T>

          class SubjectClass {
            val subjectProp: StringList<String> = listOf()
          }
          """
        )

        val subjectProp = file.subjectClass().subjectProp()

        subjectProp.typeReferenceName.await() shouldBe ClassName("com.subject", "StringList")
          .parameterizedBy(ClassName("kotlin", "String"))
      }

      @Test
      fun `member property with import alias type should have the original imported type`() = test {
        val file = project.createFile(
          """
          package com.subject

          import kotlin.Unit as KUnit

          class SubjectClass {
            lateinit var kunit: KUnit
          }
          """
        )

        val subjectProp = file.subjectClass().property("kunit")

        subjectProp.typeReferenceName.await() shouldBe ClassName("kotlin", "Unit")
      }

      @Test
      fun `member property with nullable type should have resolved nullable type`() = test {
        val file = project.createFile(
          """
          package com.subject

          class SubjectClass {
            val subjectProp: String? = null
          }
          """
        )

        val subjectProp = file.subjectClass().subjectProp()

        subjectProp.typeReferenceName.await() shouldBe ClassName(
          "kotlin",
          "String",
          nullable = true
        )
      }

      @Test
      fun `member property with custom generic type should have resolved type`() = test {
        val file = project.createFile(
          """
          package com.subject

          class MyGeneric<T>

          class SubjectClass {
            val myGeneric: MyGeneric<String> = MyGeneric()
          }
          """
        )

        val myGeneric = file.subjectClass().property("myGeneric")

        myGeneric.typeReferenceName.await() shouldBe ClassName("com.subject", "MyGeneric")
          .parameterizedBy(ClassName("kotlin", "String"))
      }

      @Test
      fun `member property with type from different module should have resolved type`() = test {
        val file = project.createFile(
          """
          package com.subject

          import com.lib1.Lib1Class

          class SubjectClass {
            val subjectProp: Lib1Class = Lib1Class()
          }
          """
        )

        val subjectProp = file.subjectClass().subjectProp()

        subjectProp.typeReferenceName.await() shouldBe ClassName("com.lib1", "Lib1Class")
      }

      @Test
      fun `member property with complex nested generic type should have resolved type`() = test {
        val file = project.createFile(
          """
          package com.subject

          class MyGeneric<T>

          class SubjectClass {
            val myGeneric: MyGeneric<List<String>> = MyGeneric()
          }
          """
        )

        val myGeneric = file.subjectClass().property("myGeneric")

        myGeneric.typeReferenceName.await() shouldBe ClassName("com.subject", "MyGeneric")
          .parameterizedBy(
            ClassName("kotlin.collections", "List")
              .parameterizedBy(ClassName("kotlin", "String"))
          )
      }
    }
  }

  context (ProjectCollector)
  suspend fun McProject.createFile(
    @Language("kotlin")
    content: String,
    fileName: String = "Source.kt",
    sourceSetName: SourceSetName = SourceSetName.MAIN
  ): McKtFileImpl {

    val updatedProject = editSimple {

      addKotlinSource(
        kotlin = content,
        sourceSetName = sourceSetName,
        fileName = fileName
      )
    }

    val androidDataBinding = RealAndroidDataBindingNameProvider(updatedProject, sourceSetName)
    val androidRNameProvider = RealAndroidRNameProvider(updatedProject, sourceSetName)
    val declarationsInPackage = RealDeclarationsProvider(updatedProject)

    val nameParser = ParsingChain2.Factory(
      listOf(
        ImportAliasUnwrappingParsingInterceptor2(),
        ConcatenatingParsingInterceptor2(
          androidRNameProvider = androidRNameProvider,
          dataBindingNameProvider = androidDataBinding,
          declarationsProvider = declarationsInPackage,
          sourceSetName = sourceSetName
        )
      )
    )

    val sourceSet = updatedProject.sourceSets.getValue(sourceSetName)

    val kotlinEnvironment = sourceSet.kotlinEnvironmentDeferred

    val context = McElementContext(
      nameParser = nameParser,
      symbolResolver = PsiElementResolver(updatedProject, sourceSetName),
      language = KOTLIN,
      kotlinEnvironmentDeferred = kotlinEnvironment,
      stdLibNameOrNull = ReferenceName::kotlinStdLibNameOrNull
    )

    return updatedProject.jvmFiles()
      .get(sourceSetName)
      .filterIsInstance<RealKotlinFile>()
      .first { it.psi.text == content.trimIndent() }
      .let {
        McKtFileImpl(
          context = context,
          file = it.psi.file(),
          psi = it.psi
        )
      }
  }

  data class SubjectBuilder(
    val name: String,
    private val builder: (params: SubjectParams) -> String
  ) {

    fun build(params: SubjectParams): String = builder(params)
      .justifyToFirstLine()
      .replace("""\n{3,}""".toRegex(), "\n\n")
      .trim()
      .plus("\n\n")

    data class SubjectParams(
      val afterProperty: String,
      val typeAsString: String,
      val imports: List<String>,
      @Language("kotlin")
      val additionalTypes: String = ""
    ) {
      val allTypeRefs by unsafeLazy {
        typeAsString
          .splitAndTrim("<", ">", ",")
          .filter { it.isNotBlank() }
      }

      override fun toString(): String {
        return "$afterProperty | imports: $imports".remove("\n")
      }
    }
  }

  fun Sequence<SubjectParams>.checkUnique(): Sequence<SubjectParams> =
    check({ it.toList().minus(it.toSet()).isEmpty() }) {
      val extra = it.toList().minus(it.toSet())
      "Subject parameters must be unique.  The extras are:\n${extra.joinToString("\n")}"
    }

  fun Sequence<SubjectParams>.importAliases() = flatMap { params ->

    if (params.imports.any { it.matches("""\S+ as \S+""".toRegex()) }) {
      return@flatMap params.singletonList()
    }

    // keep the original params (without import aliases) in the list, before the aliases
    params.singletonList() + params.allTypeRefs
      .map { original ->

        val aliasName = original.splitAndMap('.') { it.trim().capitalize() }
          .joinToStringConcat()
          .plus("ImportAlias")

        Triple(original, aliasName, "$original as $aliasName")
      }
      .combinations()
      .map { aliases ->

        params.copy(
          afterProperty = aliases
            .fold(params.afterProperty) { acc, triple ->
              val originalSimple = triple.first.splitAndTrim('.').last()

              if (acc.contains(triple.first)) {
                acc.replace("""\b${triple.first}""", triple.second)
              } else {
                acc.replaceRegex("""\b$originalSimple""", triple.second)
              }
            },
          imports = params.imports
            .minus(aliases.mapToSet { it.first })
            .plus(aliases.map { it.third })
        )
      }
  }
    .distinctBy { it.toString() }

  context(HasTestEnvironment<ProjectTestEnvironment>)
  @SkipInStackTrace
  inline fun Sequence<SubjectParams>.asTests(
    crossinline testName: (SubjectParams) -> String = { it.toString() },
    crossinline action: suspend ProjectTestEnvironment.(SubjectParams) -> Unit
  ): Stream<out DynamicNode> = testFactory {

    groupBy { it.afterProperty }
      .toList()
      .asContainers({ it.first }) { (_, params) ->
        params
          .distinctBy { it.toString() }
          .asTests(testName, action)
      }
  }
}

fun <T> List<T>.combinations(): List<Set<T>> {
  return generateSequence(listOf(emptySet<T>())) { prev ->

    prev
      .flatMap { set ->
        filter { it !in set }
          .map { element -> set + element }
      }
      .takeIf { it.isNotEmpty() }
  }
    .flatten()
    .drop(1)
    .toList()
    .distinct()
}
