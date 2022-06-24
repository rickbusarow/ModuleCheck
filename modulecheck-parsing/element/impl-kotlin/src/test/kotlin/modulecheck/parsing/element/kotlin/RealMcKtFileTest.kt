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
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.flow.toSet
import kotlinx.coroutines.runBlocking
import modulecheck.api.context.jvmFiles
import modulecheck.parsing.element.McElement
import modulecheck.parsing.element.McProperty.McKtProperty.KtConstructorProperty
import modulecheck.parsing.element.McType.McConcreteType.McKtConcreteType
import modulecheck.parsing.element.resolve.ParsingContext
import modulecheck.parsing.gradle.model.ConfigurationName
import modulecheck.parsing.gradle.model.SourceSetName
import modulecheck.parsing.psi.ConcatenatingParsingInterceptor
import modulecheck.parsing.psi.RealKotlinFile
import modulecheck.parsing.psi.internal.PsiElementResolver
import modulecheck.parsing.psi.internal.file
import modulecheck.parsing.source.asExplicitKotlinReference
import modulecheck.parsing.source.internal.InterpretingInterceptor
import modulecheck.parsing.source.internal.ParsingChain
import modulecheck.parsing.test.NamedSymbolTest
import modulecheck.parsing.wiring.RealDeclarationsInPackageProvider
import modulecheck.project.McProject
import modulecheck.project.test.ProjectTest
import modulecheck.utils.trace.Trace
import org.intellij.lang.annotations.Language
import org.junit.jupiter.api.Disabled
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test

internal class RealMcKtFileTest : ProjectTest(), NamedSymbolTest {

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

  val NamedSymbolTest.JvmFileBuilder.ReferenceBuilder.lib1Class
    get() = explicitKotlin("com.lib1.Lib1Class")

  suspend fun RealMcKtFile.subjectClass() = declaredTypesAndInnerTypes.toList()
    .single { it.simpleName == "SubjectClass" }

  suspend fun McKtConcreteType.property(
    name: String
  ) = properties.first { it.simpleName == name }

  @Test
  fun `experiments`() = test {
    val file = project.createFile(
      """
      package com.subject

      import com.lib1.Lib1Class
      import javax.inject.Inject

      @Inject(AppScope::class, "things")
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

    val subjectClass = file.declaredTypesAndInnerTypes.toList()
      .single { it.simpleName == "SubjectClass" }

    subjectClass.properties.toList()
      .filterIsInstance<KtConstructorProperty>()
      .forEach { println(it.typeReferenceName.await()) }

    subjectClass.annotations.toSet()
      .map { it.referenceName.await() }
      .also(::println)

    file.declaredTypesAndInnerTypes shouldBe listOf<McElement>()

    file.simpleName shouldBe "SourceKt"
    file.imports shouldBe listOf("com.lib1.Lib1Class".asExplicitKotlinReference())
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

        lib1Class.typeReferenceName.await() shouldBe "com.lib1.Lib1Class".asExplicitKotlinReference()
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

        lib1Class.typeReferenceName.await() shouldBe "com.lib1.Lib1Class".asExplicitKotlinReference()
      }

    @Test
    fun `member property with inferred type from generic delegate should have resolved type`() =
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

        lib1Class.typeReferenceName.await() shouldBe "com.lib1.Lib1Class".asExplicitKotlinReference()
      }
  }

  fun McProject.createFile(
    @Language("kotlin")
    content: String,
    sourceSetName: SourceSetName = SourceSetName.MAIN
  ): RealMcKtFile {
    return runBlocking(Trace.start(RealMcKtFileTest::class)) {

      val declarationsInPackage = RealDeclarationsInPackageProvider(this@createFile)

      val nameParser = ParsingChain.Factory(
        listOf(
          ConcatenatingParsingInterceptor(declarationsInPackage, sourceSetName),
          InterpretingInterceptor()
        )
      )

      val context = ParsingContext(
        nameParser = nameParser,
        symbolResolver = PsiElementResolver(this@createFile, sourceSetName)
      )

      editSimple {
        addKotlinSource(content, sourceSetName)
      }.jvmFiles()
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
  }
}
