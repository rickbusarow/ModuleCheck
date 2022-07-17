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

package modulecheck.parsing.element.kotlin

import kotlinx.coroutines.flow.filterIsInstance
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.flow.toSet
import modulecheck.api.context.jvmFiles
import modulecheck.parsing.element.McProperty.McKtProperty.KtConstructorProperty
import modulecheck.parsing.element.McProperty.McKtProperty.KtMemberProperty
import modulecheck.parsing.element.McType.McConcreteType.McKtConcreteType
import modulecheck.parsing.element.resolve.ConcatenatingParsingInterceptor2
import modulecheck.parsing.element.resolve.ImportAliasUnwrappingParsingInterceptor2
import modulecheck.parsing.element.resolve.ParsingChain2
import modulecheck.parsing.element.resolve.ParsingContext
import modulecheck.parsing.gradle.model.ConfigurationName
import modulecheck.parsing.gradle.model.SourceSetName
import modulecheck.parsing.psi.RealKotlinFile
import modulecheck.parsing.psi.internal.PsiElementResolver
import modulecheck.parsing.psi.internal.file
import modulecheck.parsing.psi.kotlinStdLibNameOrNull
import modulecheck.parsing.source.DeclaredName
import modulecheck.parsing.source.McName.CompatibleLanguage.KOTLIN
import modulecheck.parsing.source.PackageName.Companion.asPackageName
import modulecheck.parsing.source.QualifiedDeclaredName
import modulecheck.parsing.source.ReferenceName
import modulecheck.parsing.source.SimpleName.Companion.asSimpleName
import modulecheck.parsing.test.McNameTest
import modulecheck.parsing.wiring.RealAndroidDataBindingNameProvider
import modulecheck.parsing.wiring.RealAndroidRNameProvider
import modulecheck.parsing.wiring.RealDeclarationsProvider
import modulecheck.project.McProject
import modulecheck.project.test.ProjectTest
import org.intellij.lang.annotations.Language
import org.jetbrains.kotlin.resolve.calls.callUtil.getType
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test

internal class RealMcKtFileTest : ProjectTest(), McNameTest {

  override val defaultLanguage = KOTLIN

  val lib1 by resets {
    kotlinProject(":lib1") {
      addKotlinSource(
        """
        package com.lib1

        class Lib1Class
        """
      )
    }
  }

  val project by resets {
    kotlinProject(":subject") {
      addDependency(ConfigurationName.api, lib1)
    }
  }

  val McNameTest.JvmFileBuilder.ReferenceBuilder.lib1Class
    get() = kotlin("com.lib1.Lib1Class")

  suspend fun RealMcKtFile.subjectClass() = declaredTypesAndInnerTypes.toList()
    .single { it.simpleNames.last().name == "SubjectClass" }

  suspend fun McKtConcreteType.property(
    name: String
  ) = properties.first { it.simpleNames.last().name == name }

  @Test
  fun `experiments`() = test {
    val file = project.createFile(
      """
      package com.subject

      import com.lib1.Lib1Class
      import javax.inject.Inject

      @Inject(Unit::class, "things")
      class SubjectClass(
        val lib1Class: Lib1Class
      ) {
        class SubjectInnerClass
      }
      interface SubjectInterface {
        val lib1Class: Lib1Class
      }
      object SubjectObject {
        val lib1Class: Lib1Class
      }
      class CompanionHolderClass {
        companion object SubjectCompanion {
          val lib1Class: Lib1Class
        }
      }
      """
    )

    val subjectClass = file.subjectClass()

    subjectClass.properties.toList()
      .filterIsInstance<KtConstructorProperty>()
      .forEach { println(it.typeReferenceName.await()) }

    subjectClass.annotations.toSet()
      .map { it.referenceName.await() }
      .also(::println)

    file.declaredTypesAndInnerTypes
      .map { it.declaredName }
      .toList() shouldBe listOf(
      DeclaredName.agnostic("com.subject", "SubjectClass"),
      DeclaredName.agnostic("com.subject", "SubjectClass", "SubjectInnerClass"),
      DeclaredName.agnostic("com.subject", "SubjectInterface"),
      DeclaredName.agnostic("com.subject", "SubjectObject"),
      DeclaredName.agnostic("com.subject", "CompanionHolderClass"),
      DeclaredName.agnostic("com.subject", "CompanionHolderClass", "SubjectCompanion")
    )

    file.imports shouldBe listOf(
      "com.lib1.Lib1Class".asReferenceName(),
      "javax.inject.Inject".asReferenceName()
    )
  }

  @Nested
  inner class `constructor property type resolution` {

    @Test
    fun `constructor property with no default value should have resolved type`() =
      test {
        val file = project.createFile(
          """
          package com.subject

          import com.lib1.Lib1Class
          import kotlin.properties.Delegates

          class SubjectClass(var lib1Class : Lib1Class)
          """
        )

        val lib1Class = file.subjectClass().property("lib1Class")

        lib1Class.typeReferenceName.await() shouldBe "com.lib1.Lib1Class".asReferenceName()
      }
  }

  @Nested
  inner class `member property type resolution` {

    @Test
    fun `member property with explicit type should have resolved type`() =
      test {
        val file = project.createFile(
          """
          package com.subject

          import com.lib1.Lib1Class
          import kotlin.properties.Delegates

          class SubjectClass{
            lateinit var lib1Class : Lib1Class
          }
          """
        )

        val lib1Class = file.subjectClass().property("lib1Class")

        lib1Class.typeReferenceName.await() shouldBe "com.lib1.Lib1Class".asReferenceName()
      }

    @Test
    fun `member property with explicit stdlib type should resolve`() = test {
      val file = project.createFile(
        """
          package com.subject

          import com.lib1.Lib1Class

          class SubjectClass{
            val name: String = "foo"
          }
          """
      )

      val name = file.subjectClass().property("name")

      name.typeReferenceName.await() shouldBe "kotlin.String".asReferenceName()
    }

    @Test
    fun `member property with explicit single-param stdlib generic type should resolve`() = test {
      val file = project.createFile(
        """
          package com.subject

          import com.lib1.Lib1Class

          class SubjectClass{
            lateinit var classes : List<Lib1Class>
          }
          """
      )

      val classes = file.subjectClass().property("classes")

      classes.typeReferenceName.await() shouldBe "kotlin.collections.List".asReferenceName()
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
            var lib1Class by notNull<Lib1Class>()
          }
          """
        )

        val ke = file.parsingContext.kotlinEnvironmentDeferred.await()

        val bindingContext = ke.bindingContextDeferred.await()

        val psiProperties = file.subjectClass().properties.toList()
          .filterIsInstance<KtMemberProperty>()
          .map { it.psi }
          .map { it.getType(bindingContext) }
          .also(::println)

        val lib1Class = file.subjectClass().property("lib1Class")

        lib1Class.typeReferenceName.await() shouldBe "com.lib1.Lib1Class".asReferenceName()
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
            var lib1Class by Delegates.notNull<Lib1Class>()
          }
          """
        )

        val lib1Class = file.subjectClass().property("lib1Class")

        lib1Class.typeReferenceName.await() shouldBe "com.lib1.Lib1Class".asReferenceName()
      }
  }

  suspend fun McProject.createFile(
    @Language("kotlin")
    content: String,
    sourceSetName: SourceSetName = SourceSetName.MAIN
  ): RealMcKtFile {

    val updatedProject = editSimple {
      addKotlinSource(content, sourceSetName)
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

    val context = ParsingContext(
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
        RealMcKtFile(
          parsingContext = context,
          file = it.psi.file(),
          psi = it.psi
        )
      }
  }

  fun DeclaredName.Companion.agnostic(
    packageName: String,
    vararg simpleNames: String
  ): QualifiedDeclaredName {
    return agnostic(packageName.asPackageName(), simpleNames.map { it.asSimpleName() })
  }

  fun DeclaredName.Companion.kotlin(
    packageName: String,
    vararg simpleNames: String
  ): QualifiedDeclaredName {
    return kotlin(packageName.asPackageName(), simpleNames.map { it.asSimpleName() })
  }
}
