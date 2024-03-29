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

package modulecheck.parsing.psi

import kotlinx.coroutines.flow.single
import modulecheck.api.context.jvmFiles
import modulecheck.model.dependency.ConfigurationName
import modulecheck.model.sourceset.SourceSetName
import modulecheck.parsing.source.McName.CompatibleLanguage
import modulecheck.parsing.source.McName.CompatibleLanguage.KOTLIN
import modulecheck.parsing.source.PackageName
import modulecheck.parsing.source.PackageName.Companion.asPackageName
import modulecheck.parsing.test.McNameTest
import modulecheck.project.McProject
import modulecheck.project.test.ProjectTest
import modulecheck.project.test.ProjectTestEnvironment
import org.junit.jupiter.api.Disabled
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test

internal class KotlinFileTest : ProjectTest(), McNameTest {

  override val defaultLanguage: CompatibleLanguage
    get() = KOTLIN

  val ProjectTestEnvironment.lib1: McProject
    get() = kotlinProject(":lib1") {
      addKotlinSource(
        """
        package com.lib1

        class Lib1Class
        """
      )
    }

  val ProjectTestEnvironment.project: McProject
    get() = kotlinProject(":subject") {
      addDependency(ConfigurationName.api, lib1)
    }

  val McNameTest.JvmFileBuilder.ReferenceBuilder.lib1Class
    get() = kotlin("com.lib1.Lib1Class")

  @Test
  fun `fully qualified annotated primary constructor arguments should be injected`() = test {
    val file = project.createKotlinFile(
      """
      package com.subject

      import com.lib1.Lib1Class

      class SubjectClass @javax.inject.Inject constructor(
        val lib1Class: Lib1Class
      )
      """
    )

    file shouldBeJvmFile {
      references {

        lib1Class

        kotlin("Inject")
        kotlin("com.subject.Inject")
        kotlin("com.subject.inject")
        kotlin("com.subject.javax")
        kotlin("com.subject.javax.inject.Inject")
        kotlin("inject")
        kotlin("javax")
        kotlin("javax.inject.Inject")
      }
      apiReferences {

        lib1Class
      }

      declarations {
        agnostic("com.subject.SubjectClass")
        kotlin("com.subject.SubjectClass.lib1Class")
        java("com.subject.SubjectClass.getLib1Class")
      }
    }
  }

  @Test
  fun `fully qualified annotated secondary constructor arguments should be injected`() = test {
    val file = project.createKotlinFile(
      """
      package com.subject

      import com.lib1.Lib1Class

      class SubjectClass {
        val lib1Class: Lib1Class

        @javax.inject.Inject
        constructor(lib1Class: Lib1Class) {
          this.lib1Class = lib1Class
        }
      }
      """
    )

    file shouldBeJvmFile {
      references {

        lib1Class
        kotlin("com.subject.SubjectClass.lib1Class")

        kotlin("Inject")
        kotlin("com.subject.Inject")
        kotlin("com.subject.inject")
        kotlin("com.subject.javax")
        kotlin("com.subject.javax.inject.Inject")
        kotlin("com.subject.this")
        kotlin("com.subject.this.lib1Class")
        kotlin("inject")
        kotlin("javax")
        kotlin("javax.inject.Inject")
        kotlin("this")
        kotlin("this.lib1Class")
      }
      apiReferences {

        lib1Class
      }

      declarations {
        agnostic("com.subject.SubjectClass")
        kotlin("com.subject.SubjectClass.lib1Class")
        java("com.subject.SubjectClass.getLib1Class")
      }
    }
  }

  @Test
  fun `imported annotated primary constructor arguments should be injected`() = test {
    val file = project.createKotlinFile(
      """
      package com.subject

      import com.lib1.Lib1Class
      import javax.inject.Inject

      class SubjectClass @Inject constructor(
        val lib1Class: Lib1Class
      )
      """
    )

    file shouldBeJvmFile {
      references {

        lib1Class
        kotlin("javax.inject.Inject")
      }
      apiReferences {

        lib1Class
      }

      declarations {
        agnostic("com.subject.SubjectClass")
        kotlin("com.subject.SubjectClass.lib1Class")
        java("com.subject.SubjectClass.getLib1Class")
      }
    }
  }

  @Test
  fun `wildcard-imported annotated primary constructor arguments should be injected`() = test {
    val file = project.createKotlinFile(
      """
      package com.subject

      import com.lib1.*
      import javax.inject.Inject

      class SubjectClass @Inject constructor(
        val lib1Class: Lib1Class
      )
      """
    )

    file shouldBeJvmFile {
      references {

        kotlin("com.lib1.Lib1Class")
        kotlin("javax.inject.Inject")
      }
      apiReferences {

        kotlin("com.lib1.Lib1Class")
      }
      declarations {
        agnostic("com.subject.SubjectClass")
        kotlin("com.subject.SubjectClass.lib1Class")
        java("com.subject.SubjectClass.getLib1Class")
      }
    }
  }

  @Test
  fun `fully qualified arguments in annotated primary constructor should be injected`() = test {
    val file = project.createKotlinFile(
      """
      package com.subject

      import javax.inject.Inject

      class SubjectClass @Inject constructor(
        val lib1Class: com.lib1.Lib1Class
      )
      """
    )

    file shouldBeJvmFile {
      references {

        kotlin("com")
        kotlin("com.lib1.Lib1Class")
        kotlin("com.subject.com")
        kotlin("com.subject.lib1")
        kotlin("javax.inject.Inject")
        kotlin("lib1")
      }
      apiReferences {

        kotlin("com")
        kotlin("com.lib1.Lib1Class")
        kotlin("com.subject.com")
        kotlin("com.subject.lib1")
        kotlin("lib1")
      }

      declarations {
        agnostic("com.subject.SubjectClass")
        kotlin("com.subject.SubjectClass.lib1Class")
        java("com.subject.SubjectClass.getLib1Class")
      }
    }
  }

  @Test
  fun `imported annotated secondary constructor arguments should be injected`() = test {
    val file = project.createKotlinFile(
      """
      package com.subject

      import com.lib1.Lib1Class
      import javax.inject.Inject

      class SubjectClass {
        val lib1Class: Lib1Class

        @Inject
        constructor(lib1Class: Lib1Class) {
          this.lib1Class = lib1Class
        }
      }
      """
    )

    file shouldBeJvmFile {
      references {

        lib1Class
        kotlin("javax.inject.Inject")
        kotlin("com.subject.SubjectClass.lib1Class")

        kotlin("com.subject.this")
        kotlin("com.subject.this.lib1Class")
        kotlin("this")
        kotlin("this.lib1Class")
      }
      apiReferences {

        lib1Class
      }

      declarations {
        agnostic("com.subject.SubjectClass")
        kotlin("com.subject.SubjectClass.lib1Class")
        java("com.subject.SubjectClass.getLib1Class")
      }
    }
  }

  @Test
  fun `arguments from overloaded constructor without annotation should not be injected`() = test {
    val file = project.createKotlinFile(
      """
      package com.subject

      import com.lib1.Lib1Class
      import org.jetbrains.kotlin.name.FqName
      import javax.inject.Inject

      class SubjectClass @Inject constructor(
        val lib1Class: Lib1Class
      ) {

        constructor(lib1Class: Lib1Class, other: FqName) : this(lib1Class)
      }
      """
    )

    file shouldBeJvmFile {
      references {

        lib1Class
        kotlin("org.jetbrains.kotlin.name.FqName")
        kotlin("javax.inject.Inject")
        kotlin("com.subject.SubjectClass.lib1Class")
      }
      apiReferences {

        lib1Class
        kotlin("org.jetbrains.kotlin.name.FqName")
      }

      declarations {
        agnostic("com.subject.SubjectClass")
        kotlin("com.subject.SubjectClass.lib1Class")
        java("com.subject.SubjectClass.getLib1Class")
      }
    }
  }

  @Test
  fun `api references should not include concatenated matches if the reference is already imported`() =
    test {
      val project = androidLibrary(":subject", "com.subject")

      val file = project.createKotlinFile(
        """
        package com.subject

        import androidx.lifecycle.ViewModel
        import com.modulecheck.ResourceProvider

        class MyViewModel(
          private val resourceProvider: ResourceProvider
        ) : ViewModel() {

          fun someFunction() {
            viewEffect(resourceProvider.getString(R.string.google_places_api_key))
          }
        }
        """
      )

      file shouldBeJvmFile {
        references {
          androidR("com.subject".asPackageName())

          kotlin("androidx.lifecycle.ViewModel")
          kotlin("com.modulecheck.ResourceProvider")

          kotlin("com.subject.resourceProvider.getString")
          kotlin("com.subject.viewEffect")
          kotlin("resourceProvider.getString")
          kotlin("viewEffect")

          qualifiedAndroidResource("com.subject.R.string.google_places_api_key")
          unqualifiedAndroidResource("R.string.google_places_api_key")
        }
        apiReferences {

          kotlin("androidx.lifecycle.ViewModel")
          kotlin("com.modulecheck.ResourceProvider")
        }

        declarations {
          agnostic("com.subject.MyViewModel")
          agnostic("com.subject.MyViewModel.someFunction")
        }
      }
    }

  @Test
  fun `explicit type of public property in public class should be api reference`() = test {

    val project = androidLibrary(":subject", "com.subject")

    val file = project.createKotlinFile(
      """
      package com.subject

      import com.lib.Config

      class SubjectClass {

        val config : Config = ConfigImpl(
          googleApiKey = getString(R.string.google_places_api_key),
        )
      }
      """
    )

    file shouldBeJvmFile {
      references {
        androidR("com.subject".asPackageName())

        kotlin("com.lib.Config")

        kotlin("ConfigImpl")
        kotlin("com.subject.ConfigImpl")
        kotlin("com.subject.getString")
        kotlin("com.subject.googleApiKey")
        kotlin("getString")
        kotlin("googleApiKey")

        qualifiedAndroidResource("com.subject.R.string.google_places_api_key")
        unqualifiedAndroidResource("R.string.google_places_api_key")
      }
      apiReferences {

        kotlin("com.lib.Config")
      }

      declarations {
        agnostic("com.subject.SubjectClass")
        kotlin("com.subject.SubjectClass.config")
        java("com.subject.SubjectClass.getConfig")
      }
    }
  }

  @Test
  fun `named companion object and function should also have declarations using original class name`() =
    test {

      val project = kotlinProject(":subject")

      val file = project.createKotlinFile(
        """
      package com.subject

      class SubjectClass {

        companion object Factory {
          fun create() = SubjectClass()
        }
      }
      """
      )

      file shouldBeJvmFile {
        references {
          kotlin("com.subject.SubjectClass")
        }
        declarations {
          agnostic("com.subject.SubjectClass")
          agnostic("com.subject.SubjectClass.Factory")
          agnostic("com.subject.SubjectClass.Factory.create")
          kotlin("com.subject.SubjectClass.create")
        }
      }
    }

  @Test
  fun `explicit fully qualified type of public property in public class should be api reference`() =
    test {

      val project = androidLibrary(":subject", "com.subject")

      val file = project.createKotlinFile(
        """
        package com.subject

        class SubjectClass {

          val config : com.lib.Config = ConfigImpl(
            googleApiKey = getString(R.string.google_places_api_key),
          )
        }
        """
      )

      file shouldBeJvmFile {
        references {
          androidR("com.subject".asPackageName())

          kotlin("Config")
          kotlin("ConfigImpl")
          kotlin("com")
          kotlin("com.lib.Config")
          kotlin("com.subject.Config")
          kotlin("com.subject.ConfigImpl")
          kotlin("com.subject.com")
          kotlin("com.subject.com.lib.Config")
          kotlin("com.subject.getString")
          kotlin("com.subject.googleApiKey")
          kotlin("com.subject.lib")
          kotlin("getString")
          kotlin("googleApiKey")
          kotlin("lib")

          qualifiedAndroidResource("com.subject.R.string.google_places_api_key")
          unqualifiedAndroidResource("R.string.google_places_api_key")
        }
        apiReferences {

          kotlin("com.lib.Config")
          kotlin("com.subject.com.lib.Config")
        }

        declarations {
          agnostic("com.subject.SubjectClass")
          kotlin("com.subject.SubjectClass.config")
          java("com.subject.SubjectClass.getConfig")
        }
      }
    }

  @Test
  fun `explicit type of public property in internal class should not be api reference`() = test {
    val file = project.createKotlinFile(
      """
      package com.subject

      import com.lib.Config

      internal class SubjectClass {

        val config : Config = ConfigImpl(
          googleApiKey = getString(R.string.google_places_api_key),
        )
      }
        """
    )

    file shouldBeJvmFile {
      references {
        kotlin("com.lib.Config")

        kotlin("ConfigImpl")
        kotlin("com.subject.ConfigImpl")
        kotlin("com.subject.getString")
        kotlin("com.subject.googleApiKey")
        kotlin("getString")
        kotlin("googleApiKey")

        unqualifiedAndroidResource("R.string.google_places_api_key")
      }
      apiReferences {
      }

      declarations {
        // TODO These should be declared, but with `internal` visibility somehow
        //  https://github.com/RBusarow/ModuleCheck/issues/531
        // agnostic("com.subject.SubjectClass")
        // agnostic("com.subject.SubjectClass.config")
      }
    }
  }

  @Test
  fun `implicit type of public property in public class should be api reference`() = test {
    val file = project.createKotlinFile(
      """
      package com.subject

      import com.lib.Config

      class SubjectClass {

        val config = Config(
          googleApiKey = getString(R.string.google_places_api_key),
        )
      }
        """
    )

    file shouldBeJvmFile {
      references {
        kotlin("com.lib.Config")

        kotlin("com.subject.getString")
        kotlin("com.subject.googleApiKey")
        kotlin("getString")
        kotlin("googleApiKey")

        unqualifiedAndroidResource("R.string.google_places_api_key")
      }
      apiReferences {
        kotlin("com.lib.Config")
      }

      declarations {
        agnostic("com.subject.SubjectClass")
        kotlin("com.subject.SubjectClass.config")
        java("com.subject.SubjectClass.getConfig")
      }
    }
  }

  @Test
  fun `file with JvmName annotation should count as declaration`() = test {
    val file = project.createKotlinFile(
      """
      @file:JvmName("SubjectFile")
      package com.subject

      fun someFunction() = Unit

      val someProperty = ""
      val someProperty2
        @JvmName("alternateGetter") get() = ""
      val someProperty3
        get() = ""
      """
    )

    file shouldBeJvmFile {
      references {
        kotlin("kotlin.Unit")
        kotlin("kotlin.jvm.JvmName")
      }

      declarations {
        kotlin("com.subject.someFunction")
        java("com.subject.SubjectFile.someFunction")
        kotlin("com.subject.someProperty")
        java("com.subject.SubjectFile.getSomeProperty")
        kotlin("com.subject.someProperty2")
        java("com.subject.SubjectFile.alternateGetter")
        kotlin("com.subject.someProperty3")
        java("com.subject.SubjectFile.getSomeProperty3")
      }
    }
  }

  @Test
  fun `file with JvmName annotation should not have alternate names for type declarations`() =
    test {
      val file = project.createKotlinFile(
        """
      @file:JvmName("SubjectFile")
      package com.subject

      class SubjectClass
        """
      )

      file shouldBeJvmFile {
        references {
          kotlin("kotlin.jvm.JvmName")
        }
        declarations {
          agnostic(name = "com.subject.SubjectClass")
        }
      }
    }

  @Test
  fun `file without JvmName should have alternate names for top-level functions`() = test {
    val file = project.createKotlinFile(
      """
      package com.subject

      fun someFunction() = Unit

      val someProperty = ""
      """
    )

    file shouldBeJvmFile {
      references {
        kotlin("kotlin.Unit")
      }
      declarations {
        kotlin("com.subject.someFunction")
        java("com.subject.SourceKt.someFunction")
        kotlin("com.subject.someProperty")
        java("com.subject.SourceKt.getSomeProperty")
      }
    }
  }

  @Test
  fun `val property with is- prefix should not have get-prefix for java method`() = test {
    val file = project.createKotlinFile(
      """
      package com.subject

      val isAProperty = true
      """
    )

    file shouldBeJvmFile {
      declarations {
        kotlin("com.subject.isAProperty")
        java("com.subject.SourceKt.isAProperty")
      }
    }
  }

  @Test
  fun `var property with is- prefix should have set- prefix and no is- for java method`() = test {
    val file = project.createKotlinFile(
      """
      package com.subject

      var isAProperty = true
      """
    )

    file shouldBeJvmFile {
      declarations {
        kotlin("com.subject.isAProperty")
        java("com.subject.SourceKt.isAProperty")
        java("com.subject.SourceKt.setAProperty")
      }
    }
  }

  @Test
  fun `var property with explicit accessors and is- prefix should have set- prefix and no is- for java method`() =
    test {
      val file = project.createKotlinFile(
        """
      package com.subject

      var isAProperty = true
        public get
        public set
        """
      )

      file shouldBeJvmFile {
        declarations {
          kotlin("com.subject.isAProperty")
          java("com.subject.SourceKt.isAProperty")
          java("com.subject.SourceKt.setAProperty")
        }
      }
    }

  @Test
  fun `is- prefix should not be removed if the following character is a lowercase letter`() = test {
    val file = project.createKotlinFile(
      """
        package com.subject

        var isaProperty = true
        """
    )

    file shouldBeJvmFile {
      declarations {
        kotlin("com.subject.isaProperty")
        java("com.subject.SourceKt.getIsaProperty")
        java("com.subject.SourceKt.setIsaProperty")
      }
    }
  }

  @Test
  fun `is- should not be removed if it's not at the start of the name`() = test {
    val file = project.createKotlinFile(
      """
        package com.subject

        var _isAProperty = true
        """
    )

    file shouldBeJvmFile {
      declarations {
        kotlin("com.subject._isAProperty")
        java("com.subject.SourceKt.get_isAProperty")
        java("com.subject.SourceKt.set_isAProperty")
      }
    }
  }

  @Test
  fun `file without JvmName should not have alternate names for type declarations`() = test {
    val file = project.createKotlinFile(
      """
      package com.subject

      class SubjectClass
      """
    )

    file shouldBeJvmFile {
      declarations {
        agnostic("com.subject.SubjectClass")
      }
    }
  }

  @Test
  fun `object should have alternate name with INSTANCE`() = test {
    val file = project.createKotlinFile(
      """
      package com.subject

      object Utils
      """
    )

    file shouldBeJvmFile {
      declarations {
        agnostic("com.subject.Utils")
        java("com.subject.Utils.INSTANCE")
      }
    }
  }

  @Test
  fun `companion object should have alternate name for both with Companion`() = test {
    val file = project.createKotlinFile(
      """
      package com.subject

      class SomeClass {
        companion object
      }
      """
    )

    file shouldBeJvmFile {
      declarations {
        agnostic("com.subject.SomeClass")
        agnostic("com.subject.SomeClass.Companion")
      }
    }
  }

  @Nested
  inner class `extensions` {

    @Test
    fun `top-level extension property declaration`() = test {
      val file = project.createKotlinFile(
        """
      package com.subject

      val String.vowels get() = replace("[^aeiou]".toRegex(),"")
      """
      )

      file shouldBeJvmFile {
        references {
          kotlin("kotlin.String")
          kotlin("kotlin.text.replace")

          // TODO this is all definitely wrong
          kotlin("\"[^aeiou]\".toRegex")
          kotlin("com.subject.\"[^aeiou]\".toRegex")
        }

        apiReferences {
          kotlin("kotlin.String")
        }

        declarations {
          kotlin("com.subject.vowels")
          java("com.subject.SourceKt.getVowels")
        }
      }
    }

    @Disabled
    @Test
    fun `top-level extension property reference from String literal`() = test {
      val file = project.createKotlinFile(
        """
      package com.subject

      val String.vowels get() = replace("[^aeiou]".toRegex(),"")

      val someVowels = "some string".vowels
      """
      )

      file shouldBeJvmFile {
        references {
          kotlin("kotlin.String")
          kotlin("kotlin.text.replace")

          // TODO this is all definitely wrong
          kotlin("\"[^aeiou]\".toRegex")
          kotlin("com.subject.\"[^aeiou]\".toRegex")
        }

        apiReferences {
          kotlin("kotlin.String")
        }

        declarations {
          kotlin("com.subject.foo")
          kotlin("com.subject.vowels")
          java("com.subject.SourceKt.foo")
          java("com.subject.SourceKt.getVowels")
        }
      }
    }

    @Disabled
    @Test
    fun `top-level extension function`() = test {
      val file = project.createKotlinFile(
        """
      package com.subject

      fun String.vowels() = replace("[^aeiou]".toRegex(),"")
      """
      )

      file shouldBeJvmFile {
        references {
          kotlin("kotlin.String")
          kotlin("kotlin.text.replace")

          // TODO this is all definitely wrong
          kotlin("\"[^aeiou]\".toRegex")
          kotlin("com.subject.\"[^aeiou]\".toRegex")
        }

        apiReferences {
          kotlin("kotlin.String")
        }

        declarations {
          java("com.subject.SourceKt.getVowels")

          kotlin("com.subject.vowels")
        }
      }
    }

    @Disabled
    @Test
    fun `extension property reference from variable`() = test {
      val file = project.createKotlinFile(
        """
      package com.subject

      val String.vowels get() = replace("[^aeiou]".toRegex(),"")

      fun foo(someString: String) {
        val someVowels = someString.vowels
      }
      """
      )

      file shouldBeJvmFile {
        references {
          kotlin("kotlin.String")
          kotlin("kotlin.text.replace")

          // TODO this is all definitely wrong
          // interpretedKotlin("\"[^aeiou]\".toRegex")
          // interpretedKotlin("com.subject.\"[^aeiou]\".toRegex")
          // interpretedKotlin("com.subject.someString.vowels")
          // interpretedKotlin("someString.vowels")
        }

        apiReferences {
          kotlin("kotlin.String")
        }

        declarations {
          kotlin("com.subject.vowels")
          java("com.subject.SourceKt.vowels")
        }
      }
    }
  }

  @Test
  fun `class with name wrapped in backticks is allowed`() = test {

    val file = project.createKotlinFile(
      """
        package com.subject

        class `outer in backticks` {
          inner class `inner in backticks` {
            fun `function in backticks`() { }
          }
        }
        """
    )

    file shouldBeJvmFile {
      declarations {
        kotlin(
          "`outer in backticks`.`inner in backticks`.`function in backticks`",
          packageName = "com.subject".asPackageName()
        )
        kotlin("`outer in backticks`", packageName = "com.subject".asPackageName())
        kotlin(
          "`outer in backticks`.`inner in backticks`",
          packageName = "com.subject".asPackageName()
        )
      }
    }
  }

  @Test
  fun `function with name wrapped in backticks is allowed`() = test {

    val file = project.createKotlinFile(
      """
        import com.lib1.Lib1Class

        class Subject {
          fun `name in backticks`() = Unit
        }
        """
    )

    file shouldBeJvmFile {
      references {
        kotlin("com.lib1.Lib1Class")
        kotlin("kotlin.Unit")
      }

      declarations {
        agnostic("Subject", packageName = PackageName(null))
        kotlin("Subject.`name in backticks`", packageName = PackageName(null))
      }
    }
  }

  @Test
  fun `file without package should put declarations at the root`() = test {

    val file = project.createKotlinFile(
      """
        import com.lib1.Lib1Class

        class Subject {
          private val lib1Class = Lib1Class()
        }
        """
    )

    file shouldBeJvmFile {
      references {
        kotlin("com.lib1.Lib1Class")
      }

      declarations {
        agnostic("Subject", packageName = PackageName(null))
      }
    }
  }

  @Test
  fun `top-level function with JvmName annotation should have alternate name`() = test {
    val file = project.createKotlinFile(
      """
      package com.subject

      @JvmName("alternate")
      fun someFunction() = Unit
      """
    )

    file shouldBeJvmFile {
      references {
        kotlin("kotlin.Unit")
        kotlin("kotlin.jvm.JvmName")
      }

      apiReferences {
      }

      declarations {
        kotlin("com.subject.someFunction")
        java("com.subject.SourceKt.alternate")
      }
    }
  }

  @Disabled
  @Test
  fun `top-level expression inferred function return type should be api reference`() = test {
    val file = project.createKotlinFile(
      """
      package com.subject

      import com.lib1.Lib1Class

      fun someFunction() = Lib1Class()
      """
    )

    file shouldBeJvmFile {
      references {
        lib1Class
      }

      apiReferences {
        lib1Class
      }

      declarations {
        kotlin("com.subject.someFunction")
        java("com.subject.SourceKt.someFunction")
      }
    }
  }

  @Test
  fun `import alias reference which is continued should be inlined to the normal fully qualified reference`() =
    test {
      val file = project.createKotlinFile(
        """
      package com.subject

      import com.modulecheck.lib1.R as Lib1R

      val appName = Lib1R.string.app_name
        """
      )

      file shouldBeJvmFile {
        references {
          kotlin("com.modulecheck.lib1.R")
          kotlin("com.modulecheck.lib1.R.string.app_name")
        }

        apiReferences {
        }

        declarations {
          kotlin("com.subject.appName")
          java("com.subject.SourceKt.getAppName")
        }
      }
    }

  @Test
  fun `import alias reference without further selectors should be inlined to the normal fully qualified reference`() =
    test {

      val file = project.createKotlinFile(
        """
      package com.subject

      import com.modulecheck.lib1.foo as lib1Foo

      val property = lib1Foo()
        """
      )

      file shouldBeJvmFile {
        references {
          kotlin("com.modulecheck.lib1.foo")
        }

        apiReferences {
          // TODO this is wrong
          kotlin("com.modulecheck.lib1.foo")
        }

        declarations {
          kotlin("com.subject.property")
          java("com.subject.SourceKt.getProperty")
        }
      }
    }

  @Nested
  inner class `inside companion object -- function` {

    @Test
    fun `companion object function`() = test {
      val file = project.createKotlinFile(
        """
        package com.subject

        class SomeClass {
          companion object {
            fun someFunction() = Unit
          }
        }
        """
      )

      file shouldBeJvmFile {
        references {
          kotlin("kotlin.Unit")
        }

        apiReferences {
        }

        declarations {
          agnostic("com.subject.SomeClass")
          agnostic("com.subject.SomeClass.Companion")
          agnostic("com.subject.SomeClass.Companion.someFunction")
          kotlin("com.subject.SomeClass.someFunction")
        }
      }
    }

    @Test
    fun `companion object with JvmStatic should have alternate name`() = test {
      val file = project.createKotlinFile(
        """
        package com.subject

        class SomeClass {
          companion object {
            @JvmStatic
            fun someFunction() = Unit
          }
        }
        """
      )

      file shouldBeJvmFile {
        references {
          kotlin("kotlin.Unit")
          kotlin("kotlin.jvm.JvmStatic")
        }

        apiReferences {
        }

        declarations {
          agnostic("com.subject.SomeClass")
          agnostic("com.subject.SomeClass.Companion")
          agnostic("com.subject.SomeClass.Companion.someFunction")
          agnostic("com.subject.SomeClass.someFunction")
        }
      }
    }
  }

  @Nested
  inner class `inside object -- property` {

    @Test
    fun `object property with default setter and getter`() = test {
      val file = project.createKotlinFile(
        """
          package com.subject

          object Utils {

            var property = Unit
          }
        """
      )

      file shouldBeJvmFile {
        references {
          kotlin("kotlin.Unit")
        }

        apiReferences {
        }

        declarations {
          agnostic("com.subject.Utils")
          java("com.subject.Utils.INSTANCE")
          kotlin("com.subject.Utils.property")
          java("com.subject.Utils.INSTANCE.getProperty")
          java("com.subject.Utils.INSTANCE.setProperty")
        }
      }
    }

    @Test
    fun `object property with JvmStatic and default setter and getter`() = test {
      val file = project.createKotlinFile(
        """
          package com.subject

          object Utils {

            @JvmStatic var property = Unit
          }
        """
      )

      file shouldBeJvmFile {
        references {
          kotlin("kotlin.Unit")
          kotlin("kotlin.jvm.JvmStatic")
        }

        apiReferences {
        }

        declarations {
          agnostic("com.subject.Utils")
          java("com.subject.Utils.INSTANCE")
          kotlin("com.subject.Utils.property")
          java("com.subject.Utils.getProperty")
          java("com.subject.Utils.setProperty")
          java("com.subject.Utils.INSTANCE.getProperty")
          java("com.subject.Utils.INSTANCE.setProperty")
        }
      }
    }

    @Test
    fun `object property with JvmName setter and getter`() = test {
      val file = project.createKotlinFile(
        """
          package com.subject

          object Utils {

            var property = Unit
              @JvmName("alternateGetter") get
              @JvmName("alternateSetter") set
          }
        """
      )

      file shouldBeJvmFile {
        references {
          kotlin("kotlin.Unit")
          kotlin("kotlin.jvm.JvmName")
        }

        apiReferences {
        }

        declarations {
          agnostic("com.subject.Utils")
          java("com.subject.Utils.INSTANCE")
          kotlin("com.subject.Utils.property")
          java("com.subject.Utils.INSTANCE.alternateGetter")
          java("com.subject.Utils.INSTANCE.alternateSetter")
        }
      }
    }

    @Test
    fun `object JvmStatic property with default setter and getter`() = test {
      val file = project.createKotlinFile(
        """
          package com.subject

          object Utils {

            @JvmStatic
            var property = Unit
          }
        """
      )

      file shouldBeJvmFile {
        references {
          kotlin("kotlin.Unit")
          kotlin("kotlin.jvm.JvmStatic")
        }

        apiReferences {
        }

        declarations {
          agnostic("com.subject.Utils")
          java("com.subject.Utils.INSTANCE")
          kotlin("com.subject.Utils.property")
          java("com.subject.Utils.getProperty")
          java("com.subject.Utils.INSTANCE.getProperty")
          java("com.subject.Utils.setProperty")
          java("com.subject.Utils.INSTANCE.setProperty")
        }
      }
    }

    @Test
    fun `object JvmStatic property with JvmName setter and getter`() = test {
      val file = project.createKotlinFile(
        """
          package com.subject

          object Utils {

            @JvmStatic
            var property = Unit
              @JvmName("alternateGetter") get
              @JvmName("alternateSetter") set
          }
        """
      )

      file shouldBeJvmFile {
        references {
          kotlin("kotlin.Unit")
          kotlin("kotlin.jvm.JvmName")
          kotlin("kotlin.jvm.JvmStatic")
        }

        apiReferences {
        }

        declarations {
          agnostic("com.subject.Utils")
          java("com.subject.Utils.INSTANCE")
          kotlin("com.subject.Utils.property")
          java("com.subject.Utils.alternateGetter")
          java("com.subject.Utils.INSTANCE.alternateGetter")
          java("com.subject.Utils.alternateSetter")
          java("com.subject.Utils.INSTANCE.alternateSetter")
        }
      }
    }
  }

  @Nested
  inner class `inside object -- function` {

    @Test
    fun `object function`() = test {
      val file = project.createKotlinFile(
        """
          package com.subject

          object Utils {

            fun someFunction() = Unit
          }
        """
      )

      file shouldBeJvmFile {
        references {
          kotlin("kotlin.Unit")
        }

        apiReferences {
        }

        declarations {
          agnostic("com.subject.Utils")
          java("com.subject.Utils.INSTANCE")
          kotlin("com.subject.Utils.someFunction")
          java("com.subject.Utils.INSTANCE.someFunction")
        }
      }
    }

    @Test
    fun `object JvmStatic function`() = test {
      val file = project.createKotlinFile(
        """
          package com.subject

          object Utils {
            @JvmStatic
            fun someFunction() = Unit
          }
        """
      )

      file shouldBeJvmFile {
        references {
          kotlin("kotlin.Unit")
          kotlin("kotlin.jvm.JvmStatic")
        }

        apiReferences {
        }

        declarations {
          agnostic("com.subject.Utils")
          agnostic("com.subject.Utils.someFunction")
          java("com.subject.Utils.INSTANCE")
          java("com.subject.Utils.INSTANCE.someFunction")
          java("com.subject.Utils.someFunction")
        }
      }
    }

    @Test
    fun `object JvmStatic function with JvmName`() = test {
      val file = project.createKotlinFile(
        """
          package com.subject

          object Utils {
            @JvmStatic
            @JvmName("alternate")
            fun someFunction() = Unit
          }
        """
      )

      file shouldBeJvmFile {
        references {
          kotlin("kotlin.Unit")
          kotlin("kotlin.jvm.JvmName")
          kotlin("kotlin.jvm.JvmStatic")
        }

        apiReferences {
        }

        declarations {
          agnostic("com.subject.Utils")
          java("com.subject.Utils.INSTANCE")
          java("com.subject.Utils.alternate")
          java("com.subject.Utils.INSTANCE.alternate")
          kotlin("com.subject.Utils.someFunction")
        }
      }
    }

    @Test
    fun `object function with JvmName`() = test {
      val file = project.createKotlinFile(
        """
          package com.subject

          object Utils {
            @JvmName("alternate")
            fun someFunction() = Unit
          }
        """
      )

      file shouldBeJvmFile {
        references {
          kotlin("kotlin.Unit")
          kotlin("kotlin.jvm.JvmName")
        }

        apiReferences {
        }

        declarations {
          agnostic("com.subject.Utils")
          java("com.subject.Utils.INSTANCE")
          kotlin("com.subject.Utils.someFunction")
          java("com.subject.Utils.INSTANCE.alternate")
        }
      }
    }
  }

  @Nested
  inner class `Android resource references` {

    @Test
    fun `unqualified android resource reference in base package`() = test {

      val androidProject = androidLibrary(":subject", "com.subject")

      val file = androidProject.createKotlinFile(
        """
        package com.subject

        val someString = R.string.app_name
        """
      )

      file shouldBeJvmFile {
        references {
          unqualifiedAndroidResource("R.string.app_name")
          androidR("com.subject".asPackageName())
          qualifiedAndroidResource("com.subject.R.string.app_name")
        }

        apiReferences {
        }

        declarations {
          kotlin("com.subject.someString")
          java("com.subject.SourceKt.getSomeString")
        }
      }
    }

    @Test
    fun `unqualified android resource reference with R import`() = test {

      val otherLib = androidLibrary(":other", "com.modulecheck.other")

      val project = androidLibrary(":subject", "com.subject") {
        addDependency(ConfigurationName.implementation, otherLib)

        addKotlinSource(
          """
          package com.subject

          import com.modulecheck.other.R

          val someString = R.string.app_name
          """
        )
      }

      val file = project.jvmFiles().get(SourceSetName.MAIN).single()

      file shouldBeJvmFile {
        references {
          androidR("com.modulecheck.other".asPackageName())
          qualifiedAndroidResource("com.modulecheck.other.R.string.app_name")
          unqualifiedAndroidResource("R.string.app_name")
        }

        apiReferences {
        }

        declarations {
          kotlin("com.subject.someString")
          java("com.subject.SourceKt.getSomeString")
        }
      }
    }

    @Test
    fun `android resource reference with R string import`() = test {

      val otherLib = androidLibrary(":other", "com.modulecheck.other")

      val project = androidLibrary(":subject", "com.subject") {
        addDependency(ConfigurationName.implementation, otherLib)

        addKotlinSource(
          """
          package com.subject

          import com.modulecheck.other.R.string

          val someString = string.app_name
          """
        )
      }

      val file = project.jvmFiles().get(SourceSetName.MAIN).single()

      file shouldBeJvmFile {
        references {
          androidR("com.modulecheck.other".asPackageName())
          kotlin("com.modulecheck.other.R.string")
          qualifiedAndroidResource("com.modulecheck.other.R.string.app_name")
          unqualifiedAndroidResource("R.string.app_name")
        }

        apiReferences {
        }

        declarations {
          kotlin("com.subject.someString")
          java("com.subject.SourceKt.getSomeString")
        }
      }
    }

    @Test
    fun `android resource reference with wildcard R import in base package`() = test {

      val otherLib = androidLibrary(":other", "com.modulecheck.other")

      val project = androidLibrary(":subject", "com.subject") {
        addDependency(ConfigurationName.implementation, otherLib)

        addKotlinSource(
          """
          package com.subject

          import com.modulecheck.other.*

          val someString = R.string.app_name
          """
        )
      }

      val file = project.jvmFiles().get(SourceSetName.MAIN).single()

      file shouldBeJvmFile {
        references {

          androidR("com.subject".asPackageName())
          qualifiedAndroidResource("com.subject.R.string.app_name")
          unqualifiedAndroidResource("R.string.app_name")
        }

        apiReferences {
        }

        declarations {
          kotlin("com.subject.someString")
          java("com.subject.SourceKt.getSomeString")
        }
      }
    }

    @Test
    fun `android resource reference with wildcard R import not in base package`() = test {

      val otherLib = androidLibrary(":other", "com.modulecheck.other")

      val project = androidLibrary(":subject", "com.subject") {
        addDependency(ConfigurationName.implementation, otherLib)

        addKotlinSource(
          """
          package com.subject.internal

          import com.modulecheck.other.*

          val someString = R.string.app_name
          """
        )
      }

      val file = project.jvmFiles().get(SourceSetName.MAIN).single()

      file shouldBeJvmFile {
        references {

          androidR("com.modulecheck.other".asPackageName())
          qualifiedAndroidResource("com.modulecheck.other.R.string.app_name")
          unqualifiedAndroidResource("R.string.app_name")
        }

        apiReferences {
        }

        declarations {
          kotlin("com.subject.internal.someString")
          java("com.subject.internal.SourceKt.getSomeString")
        }
      }
    }

    @Test
    fun `android resource reference with wildcard R member import`() = test {

      val otherLib = androidLibrary(":other", "com.modulecheck.other")

      val project = androidLibrary(":subject", "com.subject") {
        addDependency(ConfigurationName.implementation, otherLib)
      }

      val file = project.createKotlinFile(
        """
          package com.subject.internal

          import com.modulecheck.other.R.*

          val someString = string.app_name
        """
      )

      file shouldBeJvmFile {
        references {

          androidR("com.modulecheck.other".asPackageName())
          qualifiedAndroidResource("com.modulecheck.other.R.string.app_name")
          unqualifiedAndroidResource("R.string.app_name")
        }

        apiReferences {
        }

        declarations {
          kotlin("com.subject.internal.someString")
          java("com.subject.internal.SourceKt.getSomeString")
        }
      }
    }

    @Test
    fun `android resource reference with explicit R string import`() = test {

      val otherLib = androidLibrary(":other", "com.modulecheck.other")

      val project = androidLibrary(":subject", "com.subject") {
        addDependency(ConfigurationName.implementation, otherLib)
      }

      val file = project.createKotlinFile(
        """
          package com.subject

          import com.modulecheck.other.R.string

          val someString = string.app_name
        """
      )

      file shouldBeJvmFile {
        references {
          androidR("com.modulecheck.other".asPackageName())
          kotlin("com.modulecheck.other.R.string")
          qualifiedAndroidResource("com.modulecheck.other.R.string.app_name")
          unqualifiedAndroidResource("R.string.app_name")
        }

        apiReferences {
        }

        declarations {
          kotlin("com.subject.someString")
          java("com.subject.SourceKt.getSomeString")
        }
      }
    }

    @Test
    fun `android data-binding reference from dependency with explicit import`() = test {

      val otherLib = androidLibrary(":other", "com.modulecheck.other") {
        addLayoutFile(
          "fragment_other.xml",
          """<?xml version="1.0" encoding="utf-8"?>
          <layout/>
          """
        )
      }

      val project = androidLibrary(":subject", "com.subject") {
        addDependency(ConfigurationName.implementation, otherLib)
      }

      val file = project.createKotlinFile(
        """
          package com.subject

          import com.modulecheck.other.databinding.FragmentOtherBinding

          val binding = FragmentOtherBinding.inflate()
        """
      )

      file shouldBeJvmFile {
        references {

          androidDataBinding("com.modulecheck.other.databinding.FragmentOtherBinding")
          androidDataBinding("com.modulecheck.other.databinding.FragmentOtherBinding.inflate")
        }

        apiReferences {
        }

        declarations {
          kotlin("com.subject.binding")
          java("com.subject.SourceKt.getBinding")
        }
      }
    }

    @Test
    fun `android data-binding reference from dependency with fully qualified reference`() = test {

      val otherLib = androidLibrary(":other", "com.modulecheck.other") {
        addLayoutFile(
          "fragment_other.xml",
          """<?xml version="1.0" encoding="utf-8"?>
          <layout/>
          """
        )
      }

      val project = androidLibrary(":subject", "com.subject") {
        addDependency(ConfigurationName.implementation, otherLib)
      }

      val file = project.createKotlinFile(
        """
          package com.subject

          val binding = com.modulecheck.other.databinding.FragmentOtherBinding.inflate()
        """
      )

      file shouldBeJvmFile {
        references {

          androidDataBinding("com.modulecheck.other.databinding.FragmentOtherBinding")
          androidDataBinding("com.modulecheck.other.databinding.FragmentOtherBinding.inflate")
        }

        apiReferences {
        }

        declarations {
          kotlin("com.subject.binding")
          java("com.subject.SourceKt.getBinding")
        }
      }
    }

    @Test
    fun `android data-binding reference from dependency with wildcard import`() = test {

      val otherLib = androidLibrary(":other", "com.modulecheck.other") {
        addLayoutFile(
          "fragment_other.xml",
          """<?xml version="1.0" encoding="utf-8"?>
          <layout/>
          """
        )
      }

      val project = androidLibrary(":subject", "com.subject") {
        addDependency(ConfigurationName.implementation, otherLib)
      }

      val file = project.createKotlinFile(
        """
          package com.subject

          import com.modulecheck.other.databinding.*

          val binding = FragmentOtherBinding.inflate()
        """
      )

      file shouldBeJvmFile {
        references {

          androidDataBinding("com.modulecheck.other.databinding.FragmentOtherBinding")
          androidDataBinding("com.modulecheck.other.databinding.FragmentOtherBinding.inflate")
        }

        apiReferences {
        }

        declarations {
          kotlin("com.subject.binding")
          java("com.subject.SourceKt.getBinding")
        }
      }
    }
  }
}
