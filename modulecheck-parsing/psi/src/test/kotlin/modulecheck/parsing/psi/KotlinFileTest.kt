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

package modulecheck.parsing.psi

import io.kotest.matchers.collections.shouldContainExactlyInAnyOrder
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.runBlocking
import modulecheck.parsing.gradle.SourceSetName
import modulecheck.parsing.psi.internal.PsiElementResolver
import modulecheck.parsing.psi.internal.psiFileFactory
import modulecheck.parsing.source.AgnosticDeclarationName
import modulecheck.parsing.source.DeclarationName
import modulecheck.parsing.source.JavaSpecificDeclaration
import modulecheck.parsing.source.KotlinSpecificDeclaration
import modulecheck.parsing.source.asExplicitKotlinReference
import modulecheck.parsing.source.asInterpretedKotlinReference
import modulecheck.project.McProject
import modulecheck.project.test.ProjectTest
import org.intellij.lang.annotations.Language
import org.jetbrains.kotlin.idea.KotlinLanguage
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.psi.KtFile
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.fail
import modulecheck.parsing.source.asDeclarationName as neutralExtension
import modulecheck.parsing.source.asJavaDeclarationName as javaExtension
import modulecheck.parsing.source.asKotlinDeclarationName as kotlinExtension

internal class KotlinFileTest : ProjectTest() {

  @Test
  fun `fully qualified annotated primary constructor arguments should be injected`() = test {

    val file = createFile(
      """
      package com.test

      import com.lib1.Lib1Class

      class InjectClass @javax.inject.Inject constructor(
        val lib1Class: Lib1Class
      )
      """
    )

    file.constructorInjectedParams.await() shouldBe listOf(
      FqName("com.lib1.Lib1Class")
    )
  }

  @Test
  fun `fully qualified annotated secondary constructor arguments should be injected`() = test {

    val file = createFile(
      """
      package com.test

      import com.lib1.Lib1Class

      class InjectClass {
        val lib1Class: Lib1Class

        @javax.inject.Inject
        constructor(lib1Class: Lib1Class) {
          this.lib1Class = lib1Class
        }
      }
      """
    )

    file.constructorInjectedParams.await() shouldBe listOf(
      FqName("com.lib1.Lib1Class")
    )
  }

  @Test
  fun `imported annotated primary constructor arguments should be injected`() = test {

    val file = createFile(
      """
      package com.test

      import com.lib1.Lib1Class
      import javax.inject.Inject

      class InjectClass @Inject constructor(
        val lib1Class: Lib1Class
      )
      """
    )

    file.constructorInjectedParams.await() shouldBe listOf(
      FqName("com.lib1.Lib1Class")
    )
  }

  @Test
  fun `wildcard-imported annotated primary constructor arguments should be injected`() = test {

    val file = createFile(
      """
      package com.test

      import com.lib1.*
      import javax.inject.Inject

      class InjectClass @Inject constructor(
        val lib1Class: Lib1Class
      )
      """
    )

    file.constructorInjectedParams.await() shouldContainExactlyInAnyOrder listOf(
      FqName("com.lib1.Lib1Class")
    )
  }

  @Test
  fun `fully qualified arguments in annotated primary constructor should be injected`() = test {

    val file = createFile(
      """
      package com.test

      import javax.inject.Inject

      class InjectClass @Inject constructor(
        val lib1Class: com.lib1.Lib1Class
      )
      """
    )

    file.constructorInjectedParams.await() shouldContainExactlyInAnyOrder listOf(
      FqName("com.lib1.Lib1Class")
    )
  }

  @Test
  fun `imported annotated secondary constructor arguments should be injected`() = test {

    val file = createFile(
      """
      package com.test

      import com.lib1.Lib1Class
      import javax.inject.Inject

      class InjectClass {
        val lib1Class: Lib1Class

        @Inject
        constructor(lib1Class: Lib1Class) {
          this.lib1Class = lib1Class
        }
      }
      """
    )

    file.constructorInjectedParams.await() shouldBe listOf(
      FqName("com.lib1.Lib1Class")
    )
  }

  @Test
  fun `arguments from overloaded constructor without annotation should not be injected`() = test {

    val file = createFile(
      """
      package com.test

      import com.lib1.Lib1Class
      import org.jetbrains.kotlin.name.FqName
      import javax.inject.Inject

      class InjectClass @Inject constructor(
        val lib1Class: Lib1Class
      ) {

        constructor(lib1Class: Lib1Class, other: FqName) : this(lib1Class)
      }
      """
    )

    file.constructorInjectedParams.await() shouldBe listOf(
      FqName("com.lib1.Lib1Class")
    )
  }

  @Test
  fun `api references should not include concatenated matches if the reference is already imported`() =
    test {

      val file = createFile(
        """
      package com.test

      import androidx.lifecycle.ViewModel

      class MyViewModel : ViewModel() {

        fun someFunction() {
          viewEffect(resourceProvider.getString(R.string.playstore_url))
        }
      }
        """
      )

      file.apiReferences.await() shouldBe listOf("androidx.lifecycle.ViewModel")
    }

  @Test
  fun `explicit type of public property in public class should be api reference`() =
    test {

      val file = createFile(
        """
      package com.test

      import com.lib.Config

      class MyClass {

        val config : Config = ConfigImpl(
          googleApiKey = getString(R.string.google_places_api_key),
        )
      }
        """
      )

      file.apiReferences.await() shouldBe listOf("com.lib.Config")
    }

  @Test
  fun `explicit fully qualified type of public property in public class should be api reference`() =
    test {

      val file = createFile(
        """
      package com.test

      class MyClass {

        val config : com.lib.Config = ConfigImpl(
          googleApiKey = getString(R.string.google_places_api_key),
        )
      }
        """
      )

      file.apiReferences.await() shouldBe listOf("com.lib.Config", "com.test.com.lib.Config")
    }

  @Test
  fun `explicit type of public property in internal class should not be api reference`() =
    test {

      val file = createFile(
        """
      package com.test

      import com.lib.Config

      internal class MyClass {

        val config : Config = ConfigImpl(
          googleApiKey = getString(R.string.google_places_api_key),
        )
      }
        """
      )

      file.apiReferences.await() shouldBe setOf()
    }

  @Test
  fun `implicit type of public property in public class should be api reference`() =
    test {

      val file = createFile(
        """
      package com.test

      import com.lib.Config

      class MyClass {

        val config = Config(
          googleApiKey = getString(R.string.google_places_api_key),
        )
      }
        """
      )

      file.apiReferences.await() shouldBe listOf("com.lib.Config")
    }

  @Test
  fun `file with JvmName annotation should count as declaration`() = test {

    val file = createFile(
      """
      @file:JvmName("TheFile")
      package com.test

      fun someFunction() = Unit

      val someProperty = ""
      val someProperty2
        @JvmName("alternateGetter") get() = ""
      val someProperty3
        get() = ""
      """
    )

    file.declarations shouldBe listOf(
      kotlin("com.test.someFunction"),
      java("com.test.TheFile.someFunction"),
      kotlin("com.test.someProperty"),
      java("com.test.TheFile.getSomeProperty"),
      kotlin("com.test.someProperty2"),
      java("com.test.TheFile.alternateGetter"),
      kotlin("com.test.someProperty3"),
      java("com.test.TheFile.getSomeProperty3")
    )
  }

  @Test
  fun `file with JvmName annotation should not have alternate names for type declarations`() =
    test {

      val file = createFile(
        """
      @file:JvmName("TheFile")
      package com.test

      class TheClass
        """
      )

      file.declarations shouldBe listOf(
        AgnosticDeclarationName(fqName = "com.test.TheClass")
      )
    }

  @Test
  fun `file without JvmName should have alternate names for top-level functions`() = test {

    val file = createFile(
      """
      package com.test

      fun someFunction() = Unit

      val someProperty = ""
      """
    )

    file.declarations shouldBe listOf(
      kotlin("com.test.someFunction"),
      java("com.test.SourceKt.someFunction"),
      kotlin("com.test.someProperty"),
      java("com.test.SourceKt.getSomeProperty")
    )
  }

  @Test
  fun `val property with is- prefix should not have get-prefix for java method`() = test {

    val file = createFile(
      """
      package com.test

      val isAProperty = true
      """
    )

    file.declarations shouldBe listOf(
      kotlin("com.test.isAProperty"),
      java("com.test.SourceKt.isAProperty")
    )
  }

  @Test
  fun `var property with is- prefix should have set- prefix and no is- for java method`() = test {

    val file = createFile(
      """
      package com.test

      var isAProperty = true
      """
    )

    file.declarations shouldBe listOf(
      kotlin("com.test.isAProperty"),
      java("com.test.SourceKt.isAProperty"),
      java("com.test.SourceKt.setAProperty")
    )
  }

  @Test
  fun `var property with explicit accessors and is- prefix should have set- prefix and no is- for java method`() =
    test {

      val file = createFile(
        """
      package com.test

      var isAProperty = true
        public get
        public set
        """
      )

      file.declarations shouldBe listOf(
        kotlin("com.test.isAProperty"),
        java("com.test.SourceKt.isAProperty"),
        java("com.test.SourceKt.setAProperty")
      )
    }

  @Test
  fun `is- prefix should not be removed if the following character is a lowercase letter`() =
    test {

      val file = createFile(
        """
        package com.test

        var isaProperty = true
        """
      )

      file.declarations shouldBe listOf(
        kotlin("com.test.isaProperty"),
        java("com.test.SourceKt.getIsaProperty"),
        java("com.test.SourceKt.setIsaProperty")
      )
    }

  @Test
  fun `is- should not be removed if it's not at the start of the name`() =
    test {

      val file = createFile(
        """
        package com.test

        var _isAProperty = true
        """
      )

      file.declarations shouldBe listOf(
        kotlin("com.test._isAProperty"),
        java("com.test.SourceKt.get_isAProperty"),
        java("com.test.SourceKt.set_isAProperty")
      )
    }

  @Test
  fun `file without JvmName should not have alternate names for type declarations`() = test {

    val file = createFile(
      """
      package com.test

      class TheClass
      """
    )

    file.declarations shouldBe listOf(
      both("com.test.TheClass")
    )
  }

  @Test
  fun `object should have alternate name with INSTANCE`() = test {

    val file = createFile(
      """
      package com.test

      object Utils
      """
    )

    file.declarations shouldBe listOf(
      both("com.test.Utils"),
      java("com.test.Utils.INSTANCE")
    )
  }

  @Test
  fun `companion object should have alternate name for both with Companion`() = test {

    val file = createFile(
      """
      package com.test

      class SomeClass {
        companion object
      }
      """
    )

    file.declarations shouldBe listOf(
      both("com.test.SomeClass"),
      both("com.test.SomeClass.Companion")
    )
  }

  @Test
  fun `top-level function with JvmName annotation should have alternate name`() = test {

    val file = createFile(
      """
      package com.test

      @JvmName("alternate")
      fun someFunction() = Unit
      """
    )

    file.declarations shouldBe listOf(
      kotlin("com.test.someFunction"),
      java("com.test.SourceKt.alternate")
    )
  }

  @Test
  fun `import alias reference should be inlined to the normal fully qualified reference`() = test {

    val file = createFile(
      """
      package com.test

      import com.modulecheck.lib1.R as Lib1R

      val appName = Lib1R.string.app_name
      """
    )

    file.importsLazy.value shouldBe setOf(
      "com.modulecheck.lib1.R".asExplicitKotlinReference()
    )

    file.interpretedReferencesLazy.value shouldBe setOf(
      "com.test.Lib1R".asInterpretedKotlinReference(),
      "Lib1R".asInterpretedKotlinReference(),
      "com.test.string".asInterpretedKotlinReference(),
      "string".asInterpretedKotlinReference(),
      "com.test.app_name".asInterpretedKotlinReference(),
      "app_name".asInterpretedKotlinReference(),
      "com.modulecheck.lib1.R.string".asExplicitKotlinReference(),
      "com.modulecheck.lib1.R.string.app_name".asExplicitKotlinReference(),
    )
  }

  @Nested
  inner class `inside companion object -- function` {

    @Test
    fun `companion object function`() = test {

      val file = createFile(
        """
        package com.test

        class SomeClass {
          companion object {
            fun someFunction() = Unit
          }
        }
        """
      )

      file.declarations shouldBe listOf(
        both("com.test.SomeClass"),
        both("com.test.SomeClass.Companion"),
        both("com.test.SomeClass.Companion.someFunction"),
        kotlin("com.test.SomeClass.someFunction")
      )
    }

    @Test
    fun `companion object with JvmStatic should have alternate name`() = test {

      val file = createFile(
        """
        package com.test

        class SomeClass {
          companion object {
            @JvmStatic
            fun someFunction() = Unit
          }
        }
        """
      )

      file.declarations shouldBe listOf(
        both("com.test.SomeClass"),
        both("com.test.SomeClass.Companion"),
        both("com.test.SomeClass.Companion.someFunction"),
        both("com.test.SomeClass.someFunction")
      )
    }
  }

  @Nested
  inner class `inside object -- property` {

    @Test
    fun `object property with default setter and getter`() = test {

      val file = createFile(
        """
          package com.test

          object Utils {

            var property = Unit
          }
        """
      )

      file.declarations shouldBe listOf(
        both("com.test.Utils"),
        java("com.test.Utils.INSTANCE"),
        kotlin("com.test.Utils.property"),
        java("com.test.Utils.INSTANCE.getProperty"),
        java("com.test.Utils.INSTANCE.setProperty")
      )
    }

    @Test
    fun `object property with JvmName setter and getter`() = test {

      val file = createFile(
        """
          package com.test

          object Utils {

            var property = Unit
              @JvmName("alternateGetter") get
              @JvmName("alternateSetter") set
          }
        """
      )

      file.declarations shouldBe listOf(
        both("com.test.Utils"),
        java("com.test.Utils.INSTANCE"),
        kotlin("com.test.Utils.property"),
        java("com.test.Utils.INSTANCE.alternateGetter"),
        java("com.test.Utils.INSTANCE.alternateSetter")
      )
    }

    @Test
    fun `object JvmStatic property with default setter and getter`() = test {

      val file = createFile(
        """
          package com.test

          object Utils {

            @JvmStatic
            var property = Unit
          }
        """
      )

      file.declarations shouldBe listOf(
        both("com.test.Utils"),
        java("com.test.Utils.INSTANCE"),
        kotlin("com.test.Utils.property"),
        java("com.test.Utils.getProperty"),
        java("com.test.Utils.INSTANCE.getProperty"),
        java("com.test.Utils.setProperty"),
        java("com.test.Utils.INSTANCE.setProperty")
      )
    }

    @Test
    fun `object JvmStatic property with JvmName setter and getter`() = test {

      val file = createFile(
        """
          package com.test

          object Utils {

            @JvmStatic
            var property = Unit
              @JvmName("alternateGetter") get
              @JvmName("alternateSetter") set
          }
        """
      )

      file.declarations shouldBe listOf(
        both("com.test.Utils"),
        java("com.test.Utils.INSTANCE"),
        kotlin("com.test.Utils.property"),
        java("com.test.Utils.alternateGetter"),
        java("com.test.Utils.INSTANCE.alternateGetter"),
        java("com.test.Utils.alternateSetter"),
        java("com.test.Utils.INSTANCE.alternateSetter")
      )
    }
  }

  @Nested
  inner class `inside object -- function` {

    @Test
    fun `object function`() = test {

      val file = createFile(
        """
          package com.test

          object Utils {

            fun someFunction() = Unit
          }
        """
      )

      file.declarations shouldBe listOf(
        both("com.test.Utils"),
        java("com.test.Utils.INSTANCE"),
        kotlin("com.test.Utils.someFunction"),
        java("com.test.Utils.INSTANCE.someFunction")
      )
    }

    @Test
    fun `object JvmStatic function`() = test {

      val file = createFile(
        """
          package com.test

          object Utils {
            @JvmStatic
            fun someFunction() = Unit
          }
        """
      )

      file.declarations shouldBe listOf(
        both("com.test.Utils"),
        both("com.test.Utils.someFunction"),
        java("com.test.Utils.INSTANCE"),
        java("com.test.Utils.INSTANCE.someFunction")
      )
    }

    @Test
    fun `object JvmStatic function with JvmName`() = test {

      val file = createFile(
        """
          package com.test

          object Utils {
            @JvmStatic
            @JvmName("alternate")
            fun someFunction() = Unit
          }
        """
      )

      file.declarations shouldBe listOf(
        both("com.test.Utils"),
        java("com.test.Utils.INSTANCE"),
        java("com.test.Utils.alternate"),
        java("com.test.Utils.INSTANCE.alternate"),
        kotlin("com.test.Utils.someFunction")
      )
    }

    @Test
    fun `object function with JvmName`() = test {

      val file = createFile(
        """
          package com.test

          object Utils {
            @JvmName("alternate")
            fun someFunction() = Unit
          }
        """
      )

      file.declarations shouldBe listOf(
        both("com.test.Utils"),
        java("com.test.Utils.INSTANCE"),
        kotlin("com.test.Utils.someFunction"),
        java("com.test.Utils.INSTANCE.alternate")
      )
    }
  }

  fun kotlin(name: String) = name.kotlinExtension()

  fun java(name: String) = name.javaExtension()

  fun both(name: String) = name.neutralExtension()

  fun createFile(
    @Language("kotlin")
    content: String,
    project: McProject = simpleProject(),
    sourceSetName: SourceSetName = SourceSetName.MAIN
  ): RealKotlinFile {

    val kt = KtFile(content)

    return RealKotlinFile(kt, PsiElementResolver(project, sourceSetName))
  }

  fun test(action: suspend CoroutineScope.() -> Unit) = runBlocking(block = action)

  fun Collection<DeclarationName>.prettyPrint() = groupBy { it::class }
    .toList()
    .sortedBy { it.first.qualifiedName }
    .joinToString("\n") { (type, names) ->
      val name = when (type) {
        AgnosticDeclarationName::class -> "both"
        JavaSpecificDeclaration::class -> "java"
        KotlinSpecificDeclaration::class -> "kotlin"
        else -> fail { "unrecognized declaration type -- ${type::qualifiedName}" }
      }
      names
        .sortedBy { it.fqName }
        .joinToString("\n", "$name {\n", "\n}") { "\t${it.fqName}" }
    }

  infix fun Collection<DeclarationName>.shouldBe(other: Collection<DeclarationName>) {

    prettyPrint() shouldBe other.prettyPrint()
  }

  fun KtFile(
    @Language("kotlin")
    content: String
  ): KtFile = KtFile(name = "Source.kt", content = content)

  fun KtFile(
    name: String,
    @Language("kotlin")
    content: String
  ): KtFile = psiFileFactory.createFileFromText(
    name,
    KotlinLanguage.INSTANCE,
    content.trimIndent()
  ) as KtFile
}
