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

package modulecheck.parsing.kotlin.compiler.impl

import io.kotest.assertions.assertSoftly
import io.kotest.matchers.types.shouldBeTypeOf
import modulecheck.model.dependency.ConfigurationName
import modulecheck.model.sourceset.SourceSetName
import modulecheck.parsing.kotlin.compiler.internal.isKtFile
import modulecheck.parsing.psi.internal.getChildrenOfTypeRecursive
import modulecheck.project.test.ProjectTest
import org.jetbrains.kotlin.incremental.isJavaFile
import org.jetbrains.kotlin.js.descriptorUtils.getKotlinTypeFqName
import org.jetbrains.kotlin.psi.KtProperty
import org.jetbrains.kotlin.resolve.BindingContext
import org.jetbrains.kotlin.types.error.ErrorType
import org.jetbrains.kotlin.types.isNullable
import org.junit.jupiter.api.Test

@Suppress("UnusedPrivateMember")
class RealKotlinEnvironmentTest : ProjectTest() {

  @Test
  fun `resolution should work for sources from a dependency module`() = test {

    val lib1 = kotlinProject(":lib1") {

      addKotlinSource(
        """
        package com.modulecheck.lib1

        class Lib1Class(val name: String)
        """,
        fileName = "Lib1Class.kt"
      )
    }
    val subject = kotlinProject(":subject") {
      addDependency(ConfigurationName.api, lib1)

      addKotlinSource(
        """
        package com.modulecheck.subject

        import com.modulecheck.lib1.Lib1Class

        class SubjectClass {
          val lib1 = Lib1Class("foo")
        }
        """
      )
    }

    val sourceSet = subject.sourceSets[SourceSetName.MAIN]!!
    val kotlinEnvironment = sourceSet.kotlinEnvironmentDeferred.await() as RealKotlinEnvironment
    val bindingContext = kotlinEnvironment.bindingContextDeferred.await()

    val ktFile = kotlinEnvironment.ktFile(sourceSet.jvmFiles.single())

    val property = ktFile.getChildrenOfTypeRecursive<KtProperty>().single()
    val propertyDescriptor = bindingContext[BindingContext.VARIABLE, property]!!

    val propertyType = propertyDescriptor.returnType!!

    assertSoftly {
      propertyType.getKotlinTypeFqName(true) shouldBe "com.modulecheck.lib1.Lib1Class"
      propertyType.isNullable() shouldBe false
    }
  }

  @Test
  fun `resolution should work for sources from an upstream source set in the same module`() = test {

    val subject = kotlinProject(":subject") {

      addKotlinSource(
        """
        package com.modulecheck.subject

        class DepClass
        """
      )

      addKotlinSource(
        """
        package com.modulecheck.subject

        class SubjectClass {
          val dep = DepClass()
        }
        """,
        sourceSetName = SourceSetName.TEST
      )
    }

    val sourceSet = subject.sourceSets[SourceSetName.TEST]!!
    val kotlinEnvironment = sourceSet.kotlinEnvironmentDeferred.await()
    val bindingContext = kotlinEnvironment.bindingContextDeferred.await()

    val ktFile = kotlinEnvironment.ktFile(sourceSet.jvmFiles.single())

    val property = ktFile.getChildrenOfTypeRecursive<KtProperty>().first()

    val propertyDescriptor = bindingContext[BindingContext.VARIABLE, property]!!

    val propertyType = propertyDescriptor.returnType!!

    assertSoftly {
      propertyType.getKotlinTypeFqName(true) shouldBe "com.modulecheck.subject.DepClass"
      propertyType.isNullable() shouldBe false
    }
  }

  @Test
  fun `resolution should work for java source from an upstream source set in the same module`() =
    test {

      val subject = kotlinProject(":subject") {

        addJavaSource(
          """
        package com.modulecheck.subject;

        class DepClass {}
        """
        )

        addKotlinSource(
          """
        package com.modulecheck.subject

        class SubjectClass {
          val dep = DepClass()
        }
        """,
          sourceSetName = SourceSetName.TEST
        )
      }

      val sourceSet = subject.sourceSets[SourceSetName.TEST]!!
      val kotlinEnvironment = sourceSet.kotlinEnvironmentDeferred.await()
      val bindingContext = kotlinEnvironment.bindingContextDeferred.await()

      val ktFile = kotlinEnvironment.ktFile(sourceSet.jvmFiles.single())

      val property = ktFile.getChildrenOfTypeRecursive<KtProperty>().single()
      val propertyDescriptor = bindingContext[BindingContext.VARIABLE, property]!!

      val propertyType = propertyDescriptor.returnType!!

      assertSoftly {
        propertyType.getKotlinTypeFqName(true) shouldBe "com.modulecheck.subject.DepClass"
        propertyType.isNullable() shouldBe false
      }
    }

  @Test
  fun `resolution should work for java dependency source in the same source set`() = test {

    val subject = kotlinProject(":subject") {

      addJavaSource(
        """
        package com.modulecheck.subject;

        class DepClass {}
        """
      )

      addKotlinSource(
        """
        package com.modulecheck.subject

        class SubjectClass {
          val dep = DepClass()
        }
        """
      )
    }

    val sourceSet = subject.sourceSets[SourceSetName.MAIN]!!
    val kotlinEnvironment = sourceSet.kotlinEnvironmentDeferred.await()
    val bindingContext = kotlinEnvironment.bindingContextDeferred.await()

    val ktFile = kotlinEnvironment.ktFile(sourceSet.jvmFiles.single { it.isKtFile() })

    val property = ktFile.getChildrenOfTypeRecursive<KtProperty>().single()
    val propertyDescriptor = bindingContext[BindingContext.VARIABLE, property]!!

    val propertyType = propertyDescriptor.returnType!!

    assertSoftly {
      propertyType.getKotlinTypeFqName(true) shouldBe "com.modulecheck.subject.DepClass"
      propertyType.isNullable() shouldBe false
    }
  }

  @Test
  fun `resolution should work for java source in the same source set`() = test {

    val subject = kotlinProject(":subject") {

      addKotlinSource(
        """
        package com.modulecheck.subject

        @Deprecated("no")
        class DepClass
        """
      )

      addJavaSource(
        """
        package com.modulecheck.subject;

        class SubjectClass {
          DepClass dep = new DepClass();
        }
        """
      )
    }

    val sourceSet = subject.sourceSets[SourceSetName.MAIN]!!
    val kotlinEnvironment = sourceSet.kotlinEnvironmentDeferred.await()

    val javaFile = kotlinEnvironment.javaPsiFile(sourceSet.jvmFiles.single { it.isJavaFile() })

    val property = javaFile.classes.single().fields.single()

    val propertyType = property.type

    // type resolution for Java Psi files assumes that analysis has already been run
    kotlinEnvironment.analysisResultDeferred.await()

    propertyType.canonicalText shouldBe "com.modulecheck.subject.DepClass"
  }

  @Test
  fun `resolution does not work for references which arent real`() = test {

    val subject = kotlinProject(":subject") {

      addKotlinSource(
        """
        package com.modulecheck.subject

        class SubjectClass {

          val property : com.fake.Fake<String>? = null
        }
        """
      )
    }

    val sourceSet = subject.sourceSets[SourceSetName.MAIN]!!
    val kotlinEnvironment = sourceSet.kotlinEnvironmentDeferred.await()
    val bindingContext = kotlinEnvironment.bindingContextDeferred.await()

    val ktFile = kotlinEnvironment.ktFile(sourceSet.jvmFiles.single())

    val property = ktFile.getChildrenOfTypeRecursive<KtProperty>().single()
    val propertyDescriptor = bindingContext[BindingContext.VARIABLE, property]!!

    val propertyType = propertyDescriptor.returnType!!

    assertSoftly {
      propertyType.shouldBeTypeOf<ErrorType>()
      propertyType.getKotlinTypeFqName(false) shouldBe ""
      propertyType.debugMessage shouldBe "Unresolved type for com.fake.Fake<String>"
      propertyType.isNullable() shouldBe true

      propertyType.arguments.single()
        .type
        .getKotlinTypeFqName(false) shouldBe String::class.qualifiedName
    }
  }
}
