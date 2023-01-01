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

import io.kotest.assertions.fail
import kotlinx.coroutines.flow.filterIsInstance
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.last
import kotlinx.coroutines.flow.toList
import modulecheck.api.context.jvmFiles
import modulecheck.model.dependency.ConfigurationName
import modulecheck.model.sourceset.SourceSetName
import modulecheck.parsing.kotlin.compiler.impl.RealMcPsiFileFactory
import modulecheck.parsing.psi.RealKotlinFile
import modulecheck.parsing.psi.internal.getChildrenOfTypeRecursive
import modulecheck.parsing.psi.internal.returnType
import modulecheck.project.test.ProjectTest
import modulecheck.utils.requireNotNull
import org.jetbrains.kotlin.asJava.toLightClassWithBuiltinMapping
import org.jetbrains.kotlin.js.descriptorUtils.getJetTypeFqName
import org.jetbrains.kotlin.psi.KtCallExpression
import org.jetbrains.kotlin.psi.KtClassOrObject
import org.jetbrains.kotlin.psi.KtFunction
import org.jetbrains.kotlin.psi.KtNameReferenceExpression
import org.jetbrains.kotlin.psi.KtProperty
import org.jetbrains.kotlin.resolve.BindingContext
import org.jetbrains.kotlin.resolve.BindingContextUtils
import org.junit.jupiter.api.Test
import java.io.File
import kotlin.properties.Delegates

@Suppress("UnusedPrivateMember")
class BindingContextExperiment : ProjectTest() {

  @Test
  fun `resolution should work for sources from a dependency module`() = test {

    var one: File by Delegates.notNull()
    var two: File by Delegates.notNull()

    val lib1 = kotlinProject(":lib1") {

      one = addKotlinSource(
        """
        package com.modulecheck.lib1

        class Lib1Class(val name: String)
        """,
        fileName = "Lib1Class.kt"
      )
    }
    val lib2 = kotlinProject(":lib2") {
      addDependency(ConfigurationName.api, lib1)

      two = addKotlinSource(
        """
        package com.modulecheck.lib2

        import com.modulecheck.lib1.Lib1Class

        class Lib2Class {

          val someName = someFunction("butt")

          val aString = "a string"

          val name by lazy { "butt" }

          val idsToObjects by lazy { mapOf("butt" to Unit, "thing" to Lib1Class("the thing")) }

          val lib1 = Lib1Class(someName)

          fun Lib2Class.someFunction(name: String) = name

          fun Lib1Class.foo() = println(this)
        }
        """
      )
    }

    val ss = lib2.sourceSets[SourceSetName.MAIN]!!

    val ke = ss.kotlinEnvironmentDeferred.await()
    val bc = ke.bindingContextDeferred.await()

    val twoK = lib2.jvmFiles()
      .get(SourceSetName.MAIN)
      .filterIsInstance<RealKotlinFile>()
      .first()
      .psi

    fun printFunctionThings(textMatcher: String) {

      val function = twoK.getChildrenOfTypeRecursive<KtFunction>()
        .single { it.text.contains(textMatcher) }

      val functionDescriptor = bc[BindingContext.FUNCTION, function]!!

      val receiverTypeRef = function.receiverTypeReference

      val receiverType = bc[BindingContext.TYPE, receiverTypeRef]!!

      println("##############################################")
      println("############  `${function.text}`")
      println("##############################################")

      println(
        "return type -------- ${functionDescriptor.returnType?.getJetTypeFqName(false)}"
      )
      println("receiver type ref -- $receiverTypeRef")
      println("receiver type ------ ${receiverType.getJetTypeFqName(false)}")
      println("__________________\n\n")
    }

    printFunctionThings("fun Lib2Class.someFunction")
    printFunctionThings("fun Lib1Class.foo")

    fun printPropertyThings(textMatcher: String) {

      val property = twoK.getChildrenOfTypeRecursive<KtProperty>()
        .single { it.text.contains(textMatcher) }

      val variableDescriptor = bc[BindingContext.VARIABLE, property]

      val delegateReturnKotlinType = property.delegate?.returnType(bc)

      // TypeUtils.getClassDescriptor(delegateReturnKotlinType!!)!!.containingPackage()

      println("##############################################")
      println("############  `${property.text}`")
      println("##############################################")
      println("delegate KotlinType -------------------- $delegateReturnKotlinType")
      println(
        "delegate KotlinType jetFqName ---------- ${
        delegateReturnKotlinType?.getJetTypeFqName(true)
        }"
      )
      println(
        "delegate memberScope classifier names -- ${delegateReturnKotlinType?.memberScope?.getClassifierNames()}"
      )
      println("descriptor type ------------------------ ${variableDescriptor?.type}")
      println(
        "delegate type -------------------------- ${property.delegate?.let { it::class.simpleName }}"
      )
      println("__________________\n\n")
    }

    printPropertyThings("val someName")
    printPropertyThings("val aString")
    printPropertyThings("val idsToObjects")
    printPropertyThings("val name")
    printPropertyThings("val lib1")

    fail("show me")
  }

  @Test
  fun `resolution probably does not work`() = test {

    var one: File by Delegates.notNull()
    var two: File by Delegates.notNull()
    var three: File by Delegates.notNull()
    var four: File by Delegates.notNull()

    // var (one, two, three) = listOf<File?>(null, null, null)

    val project = kotlinProject(":lib1") {

      one = addKotlinSource(
        """
        package com.test

        class Lib1Class(val name: String)
        """,
        fileName = "Lib1Class.kt"
      )

      two = addKotlinSource(
        """
        package com.test

        class Lib2Class {

          val someName : String = someFunction("butt")

          val lib1 = Lib1Class(someName)
          val lib3 = Lib3Class()

          fun someFunction(name: String) : String = name
        }
        """,
        fileName = "Lib2Class.kt"
      )

      three = addJavaSource(
        """
      package com.test;

      import com.test.Lib1Class;

      public class Lib3Class {

        String someName = "butt";

        Lib1Class lib1 = new Lib1Class(someName);
      }
      """,
        fileName = "Lib3Class.java"
      )

      four = addKotlinSource(
        """
        package com.test

        val String.vowels get() = replace("[^aeiou]".toRegex(),"")

        fun foo(someString: String) {
          val someVowels = someString.vowels
        }
        """,
        fileName = "Lib2Class.kt"
      )
    }

    val ss = project.sourceSets[SourceSetName.MAIN]!!

    val ke = ss.kotlinEnvironmentDeferred.await()
    val bc = ke.bindingContextDeferred.await()

    val jvmFiles = project.jvmFiles().get(SourceSetName.MAIN)
    val kotlinFiles = jvmFiles.filterIsInstance<RealKotlinFile>()

    val oneK = kotlinFiles.first().psi
    val twoK = kotlinFiles.toList()[1].psi
    val fourK = kotlinFiles.last().psi

    println("actual context --> $bc")

    val rmpff = RealMcPsiFileFactory(ke)
    val threeJ = rmpff.createJava(three)

    val oneClass = oneK.getChildrenOfTypeRecursive<KtClassOrObject>()
      .first()
      .toLightClassWithBuiltinMapping()
      .requireNotNull()

    println("oneClass -- ${oneClass.text}")

    val ref = threeJ.findImportReferenceTo(oneClass)

    println("threeJ ref -- ${ref?.text}")

    val callExpressions = twoK.getChildrenOfTypeRecursive<KtCallExpression>()

    val msg = callExpressions
      .joinToString("\n\n")

    println(" -- call expressions\n$msg\n===")

    val vowels = fourK.getChildrenOfTypeRecursive<KtNameReferenceExpression>()
      .joinToString("\n") { it.text }

    val fourKNameRefs = fourK.getChildrenOfTypeRecursive<KtNameReferenceExpression>()

    val someString = fourKNameRefs.single { it.text == "someString" }

    val sst = BindingContextUtils.getTypeNotNull(bc, someString)

    println("someString type -- $sst")

    // fourK.printEverything()

    println(
      """#################################### vowels

      $vowels

      ___________
      """.trimIndent()
    )

    fail("show me")
  }

  @Test
  fun `please be lazy`() = test {

    var one: File by Delegates.notNull()

    val project = kotlinProject(":lib1") {

      repeat(100) { i ->
        addKotlinSource(
          """
          package com.test

          class Lib${i}Class(val name: String)
          """,
          fileName = "Lib${i}Class.kt"
        ).also {
          if (i == 0) {
            one = it
          }
        }
      }
    }

    val ss = project.sourceSets[SourceSetName.MAIN]!!

    val ke = ss.kotlinEnvironmentDeferred.await()
    val bc = ke.bindingContextDeferred.await()

    val oneK = project.jvmFiles()
      .get(SourceSetName.MAIN)
      .filterIsInstance<RealKotlinFile>()
      .first()
      .psi

    fail("show me")
  }
}
