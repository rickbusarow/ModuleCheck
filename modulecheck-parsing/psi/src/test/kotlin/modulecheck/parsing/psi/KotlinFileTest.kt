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

import kotlinx.coroutines.flow.filterIsInstance
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.single
import kotlinx.coroutines.runBlocking
import modulecheck.api.context.jvmFiles
import modulecheck.parsing.gradle.model.ConfigurationName
import modulecheck.parsing.gradle.model.SourceSetName
import modulecheck.parsing.test.NamedSymbolTest
import modulecheck.project.McProject
import modulecheck.project.test.ProjectTest
import org.intellij.lang.annotations.Language
import org.junit.jupiter.api.Disabled
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test

internal class KotlinFileTest : ProjectTest(), NamedSymbolTest {

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

  @Test
  fun `fully qualified annotated primary constructor arguments should be injected`() = test {
    val file = project.createFile(
      """
      package com.subject

      import com.lib1.Lib1Class

      class SubjectClass @javax.inject.Inject constructor(
        val lib1Class: Lib1Class
      )
      """
    )

    file shouldBe {
      references {

        lib1Class

        interpretedKotlin("Inject")
        interpretedKotlin("com.subject.Inject")
        interpretedKotlin("com.subject.inject")
        interpretedKotlin("com.subject.javax")
        interpretedKotlin("com.subject.javax.inject.Inject")
        interpretedKotlin("inject")
        interpretedKotlin("javax")
        interpretedKotlin("javax.inject.Inject")
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
    val file = project.createFile(
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

    file shouldBe {
      references {

        lib1Class

        interpretedKotlin("Inject")
        interpretedKotlin("com.subject.Inject")
        interpretedKotlin("com.subject.inject")
        interpretedKotlin("com.subject.javax")
        interpretedKotlin("com.subject.javax.inject.Inject")
        interpretedKotlin("com.subject.lib1Class")
        interpretedKotlin("com.subject.this")
        interpretedKotlin("com.subject.this.lib1Class")
        interpretedKotlin("inject")
        interpretedKotlin("javax")
        interpretedKotlin("javax.inject.Inject")
        interpretedKotlin("lib1Class")
        interpretedKotlin("this")
        interpretedKotlin("this.lib1Class")
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
    val file = project.createFile(
      """
      package com.subject

      import com.lib1.Lib1Class
      import javax.inject.Inject

      class SubjectClass @Inject constructor(
        val lib1Class: Lib1Class
      )
      """
    )

    file shouldBe {
      references {

        lib1Class
        explicitKotlin("javax.inject.Inject")
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
    val file = project.createFile(
      """
      package com.subject

      import com.lib1.*
      import javax.inject.Inject

      class SubjectClass @Inject constructor(
        val lib1Class: Lib1Class
      )
      """
    )

    file shouldBe {
      references {

        explicitKotlin("javax.inject.Inject")

        interpretedKotlin("Lib1Class")
        interpretedKotlin("com.lib1.Lib1Class")
        interpretedKotlin("com.subject.Lib1Class")
      }
      apiReferences {

        interpretedKotlin("Lib1Class")
        interpretedKotlin("com.lib1.Lib1Class")
        interpretedKotlin("com.subject.Lib1Class")
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
    val file = project.createFile(
      """
      package com.subject

      import javax.inject.Inject

      class SubjectClass @Inject constructor(
        val lib1Class: com.lib1.Lib1Class
      )
      """
    )

    file shouldBe {
      references {

        explicitKotlin("javax.inject.Inject")

        interpretedKotlin("Lib1Class")
        interpretedKotlin("com")
        interpretedKotlin("com.lib1.Lib1Class")
        interpretedKotlin("com.subject.Lib1Class")
        interpretedKotlin("com.subject.com")
        interpretedKotlin("com.subject.com.lib1.Lib1Class")
        interpretedKotlin("com.subject.lib1")
        interpretedKotlin("lib1")
      }
      apiReferences {

        interpretedKotlin("Lib1Class")
        interpretedKotlin("com")
        interpretedKotlin("com.lib1.Lib1Class")
        interpretedKotlin("com.subject.Lib1Class")
        interpretedKotlin("com.subject.com")
        interpretedKotlin("com.subject.com.lib1.Lib1Class")
        interpretedKotlin("com.subject.lib1")
        interpretedKotlin("lib1")
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
    val file = project.createFile(
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

    file shouldBe {
      references {

        lib1Class
        explicitKotlin("javax.inject.Inject")

        interpretedKotlin("com.subject.lib1Class")
        interpretedKotlin("com.subject.this")
        interpretedKotlin("com.subject.this.lib1Class")
        interpretedKotlin("lib1Class")
        interpretedKotlin("this")
        interpretedKotlin("this.lib1Class")
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
    val file = project.createFile(
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

    file shouldBe {
      references {

        lib1Class
        explicitKotlin("org.jetbrains.kotlin.name.FqName")
        explicitKotlin("javax.inject.Inject")

        interpretedKotlin("com.subject.lib1Class")
        interpretedKotlin("lib1Class")
      }
      apiReferences {

        lib1Class
        explicitKotlin("org.jetbrains.kotlin.name.FqName")
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

      val file = project.createFile(
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

      file shouldBe {
        references {
          androidR("com.subject.R")

          explicitKotlin("androidx.lifecycle.ViewModel")
          explicitKotlin("com.modulecheck.ResourceProvider")

          interpretedKotlin("com.subject.resourceProvider.getString")
          interpretedKotlin("com.subject.viewEffect")
          interpretedKotlin("resourceProvider.getString")
          interpretedKotlin("viewEffect")

          qualifiedAndroidResource("com.subject.R.string.google_places_api_key")
          unqualifiedAndroidResource("R.string.google_places_api_key")
        }
        apiReferences {

          explicitKotlin("androidx.lifecycle.ViewModel")
          explicitKotlin("com.modulecheck.ResourceProvider")
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

    val file = project.createFile(
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

    file shouldBe {
      references {
        androidR("com.subject.R")

        explicitKotlin("com.lib.Config")

        interpretedKotlin("ConfigImpl")
        interpretedKotlin("com.subject.ConfigImpl")
        interpretedKotlin("com.subject.getString")
        interpretedKotlin("com.subject.googleApiKey")
        interpretedKotlin("getString")
        interpretedKotlin("googleApiKey")

        qualifiedAndroidResource("com.subject.R.string.google_places_api_key")
        unqualifiedAndroidResource("R.string.google_places_api_key")
      }
      apiReferences {

        explicitKotlin("com.lib.Config")
      }

      declarations {
        agnostic("com.subject.SubjectClass")
        kotlin("com.subject.SubjectClass.config")
        java("com.subject.SubjectClass.getConfig")
      }
    }
  }

  @Test
  fun `explicit fully qualified type of public property in public class should be api reference`() =
    test {

      val project = androidLibrary(":subject", "com.subject")

      val file = project.createFile(
        """
        package com.subject

        class SubjectClass {

          val config : com.lib.Config = ConfigImpl(
            googleApiKey = getString(R.string.google_places_api_key),
          )
        }
        """
      )

      file shouldBe {
        references {
          androidR("com.subject.R")

          interpretedKotlin("Config")
          interpretedKotlin("ConfigImpl")
          interpretedKotlin("com")
          interpretedKotlin("com.lib.Config")
          interpretedKotlin("com.subject.Config")
          interpretedKotlin("com.subject.ConfigImpl")
          interpretedKotlin("com.subject.com")
          interpretedKotlin("com.subject.com.lib.Config")
          interpretedKotlin("com.subject.getString")
          interpretedKotlin("com.subject.googleApiKey")
          interpretedKotlin("com.subject.lib")
          interpretedKotlin("getString")
          interpretedKotlin("googleApiKey")
          interpretedKotlin("lib")

          qualifiedAndroidResource("com.subject.R.string.google_places_api_key")
          unqualifiedAndroidResource("R.string.google_places_api_key")
        }
        apiReferences {

          interpretedKotlin("com.lib.Config")
          interpretedKotlin("com.subject.com.lib.Config")
        }

        declarations {
          agnostic("com.subject.SubjectClass")
          kotlin("com.subject.SubjectClass.config")
          java("com.subject.SubjectClass.getConfig")
        }
      }
    }

  @Test
  fun `explicit type of public property in internal class should not be api reference`() =
    test {
      val file = project.createFile(
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

      file shouldBe {
        references {
          explicitKotlin("com.lib.Config")

          interpretedKotlin("ConfigImpl")
          interpretedKotlin("com.subject.ConfigImpl")
          interpretedKotlin("com.subject.getString")
          interpretedKotlin("com.subject.googleApiKey")
          interpretedKotlin("getString")
          interpretedKotlin("googleApiKey")

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
  fun `implicit type of public property in public class should be api reference`() =
    test {
      val file = project.createFile(
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

      file shouldBe {
        references {
          explicitKotlin("com.lib.Config")

          interpretedKotlin("com.subject.getString")
          interpretedKotlin("com.subject.googleApiKey")
          interpretedKotlin("getString")
          interpretedKotlin("googleApiKey")

          unqualifiedAndroidResource("R.string.google_places_api_key")
        }
        apiReferences {
          explicitKotlin("com.lib.Config")
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
    val file = project.createFile(
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

    file shouldBe {
      references {
        explicitKotlin("kotlin.Unit")
        explicitKotlin("kotlin.jvm.JvmName")
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
      val file = project.createFile(
        """
      @file:JvmName("SubjectFile")
      package com.subject

      class SubjectClass
        """
      )

      file shouldBe {
        references {
          explicitKotlin("kotlin.jvm.JvmName")
        }
        declarations {
          agnostic(name = "com.subject.SubjectClass")
        }
      }
    }

  @Test
  fun `file without JvmName should have alternate names for top-level functions`() = test {
    val file = project.createFile(
      """
      package com.subject

      fun someFunction() = Unit

      val someProperty = ""
      """
    )

    file shouldBe {
      references {
        explicitKotlin("kotlin.Unit")
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
    val file = project.createFile(
      """
      package com.subject

      val isAProperty = true
      """
    )

    file shouldBe {
      declarations {
        kotlin("com.subject.isAProperty")
        java("com.subject.SourceKt.isAProperty")
      }
    }
  }

  @Test
  fun `var property with is- prefix should have set- prefix and no is- for java method`() = test {
    val file = project.createFile(
      """
      package com.subject

      var isAProperty = true
      """
    )

    file shouldBe {
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
      val file = project.createFile(
        """
      package com.subject

      var isAProperty = true
        public get
        public set
        """
      )

      file shouldBe {
        declarations {
          kotlin("com.subject.isAProperty")
          java("com.subject.SourceKt.isAProperty")
          java("com.subject.SourceKt.setAProperty")
        }
      }
    }

  @Test
  fun `is- prefix should not be removed if the following character is a lowercase letter`() =
    test {
      val file = project.createFile(
        """
        package com.subject

        var isaProperty = true
        """
      )

      file shouldBe {
        declarations {
          kotlin("com.subject.isaProperty")
          java("com.subject.SourceKt.getIsaProperty")
          java("com.subject.SourceKt.setIsaProperty")
        }
      }
    }

  @Test
  fun `is- should not be removed if it's not at the start of the name`() =
    test {
      val file = project.createFile(
        """
        package com.subject

        var _isAProperty = true
        """
      )

      file shouldBe {
        declarations {
          kotlin("com.subject._isAProperty")
          java("com.subject.SourceKt.get_isAProperty")
          java("com.subject.SourceKt.set_isAProperty")
        }
      }
    }

  @Test
  fun `file without JvmName should not have alternate names for type declarations`() = test {
    val file = project.createFile(
      """
      package com.subject

      class SubjectClass
      """
    )

    file shouldBe {
      declarations {
        agnostic("com.subject.SubjectClass")
      }
    }
  }

  @Test
  fun `object should have alternate name with INSTANCE`() = test {
    val file = project.createFile(
      """
      package com.subject

      object Utils
      """
    )

    file shouldBe {
      declarations {
        agnostic("com.subject.Utils")
        java("com.subject.Utils.INSTANCE")
      }
    }
  }

  @Test
  fun `companion object should have alternate name for both with Companion`() = test {
    val file = project.createFile(
      """
      package com.subject

      class SomeClass {
        companion object
      }
      """
    )

    file shouldBe {
      declarations {
        agnostic("com.subject.SomeClass")
        agnostic("com.subject.SomeClass.Companion")
      }
    }
  }

  @Test
  fun `top-level extension property`() = test {
    val file = project.createFile(
      """
      package com.subject

      val String.vowels get() = replace("[^aeiou]".toRegex(),"")
      """
    )

    file shouldBe {
      references {
        explicitKotlin("kotlin.String")
        explicitKotlin("kotlin.text.replace")

        // TODO this is all definitely wrong
        interpretedKotlin("\"[^aeiou]\".toRegex")
        interpretedKotlin("com.subject.\"[^aeiou]\".toRegex")
      }

      apiReferences {
        explicitKotlin("kotlin.String")
      }

      declarations {
        kotlin("com.subject.vowels")
        java("com.subject.SourceKt.getVowels")
      }
    }
  }

  @Test
  fun `top-level extension function`() = test {
    val file = project.createFile(
      """
      package com.subject

      fun String.vowels() = replace("[^aeiou]".toRegex(),"")
      """
    )

    file shouldBe {
      references {
        explicitKotlin("kotlin.String")
        explicitKotlin("kotlin.text.replace")

        // TODO this is all definitely wrong
        interpretedKotlin("\"[^aeiou]\".toRegex")
        interpretedKotlin("com.subject.\"[^aeiou]\".toRegex")
      }

      apiReferences {
        explicitKotlin("kotlin.String")
      }

      declarations {
        kotlin("com.subject.vowels")
        java("com.subject.SourceKt.vowels")
      }
    }
  }

  @Test
  fun `top-level function with JvmName annotation should have alternate name`() = test {
    val file = project.createFile(
      """
      package com.subject

      @JvmName("alternate")
      fun someFunction() = Unit
      """
    )

    file shouldBe {
      references {
        explicitKotlin("kotlin.Unit")
        explicitKotlin("kotlin.jvm.JvmName")
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
    val file = project.createFile(
      """
      package com.subject

      import com.lib1.Lib1Class

      fun someFunction() = Lib1Class()
      """
    )

    file shouldBe {
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
      val file = project.createFile(
        """
      package com.subject

      import com.modulecheck.lib1.R as Lib1R

      val appName = Lib1R.string.app_name
        """
      )

      file shouldBe {
        references {
          explicitKotlin("com.modulecheck.lib1.R")
          explicitKotlin("com.modulecheck.lib1.R.string.app_name")
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

      val file = project.createFile(
        """
      package com.subject

      import com.modulecheck.lib1.foo as lib1Foo

      val property = lib1Foo()
        """
      )

      file shouldBe {
        references {
          explicitKotlin("com.modulecheck.lib1.foo")
        }

        apiReferences {
          // TODO this is wrong
          explicitKotlin("com.modulecheck.lib1.foo")
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
      val file = project.createFile(
        """
        package com.subject

        class SomeClass {
          companion object {
            fun someFunction() = Unit
          }
        }
        """
      )

      file shouldBe {
        references {
          explicitKotlin("kotlin.Unit")
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
      val file = project.createFile(
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

      file shouldBe {
        references {
          explicitKotlin("kotlin.Unit")
          explicitKotlin("kotlin.jvm.JvmStatic")
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
      val file = project.createFile(
        """
          package com.subject

          object Utils {

            var property = Unit
          }
        """
      )

      file shouldBe {
        references {
          explicitKotlin("kotlin.Unit")
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
      val file = project.createFile(
        """
          package com.subject

          object Utils {

            @JvmStatic var property = Unit
          }
        """
      )

      file shouldBe {
        references {
          explicitKotlin("kotlin.Unit")
          explicitKotlin("kotlin.jvm.JvmStatic")
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
      val file = project.createFile(
        """
          package com.subject

          object Utils {

            var property = Unit
              @JvmName("alternateGetter") get
              @JvmName("alternateSetter") set
          }
        """
      )

      file shouldBe {
        references {
          explicitKotlin("kotlin.Unit")
          explicitKotlin("kotlin.jvm.JvmName")
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
      val file = project.createFile(
        """
          package com.subject

          object Utils {

            @JvmStatic
            var property = Unit
          }
        """
      )

      file shouldBe {
        references {
          explicitKotlin("kotlin.Unit")
          explicitKotlin("kotlin.jvm.JvmStatic")
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
      val file = project.createFile(
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

      file shouldBe {
        references {
          explicitKotlin("kotlin.Unit")
          explicitKotlin("kotlin.jvm.JvmName")
          explicitKotlin("kotlin.jvm.JvmStatic")
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
      val file = project.createFile(
        """
          package com.subject

          object Utils {

            fun someFunction() = Unit
          }
        """
      )

      file shouldBe {
        references {
          explicitKotlin("kotlin.Unit")
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
      val file = project.createFile(
        """
          package com.subject

          object Utils {
            @JvmStatic
            fun someFunction() = Unit
          }
        """
      )

      file shouldBe {
        references {
          explicitKotlin("kotlin.Unit")
          explicitKotlin("kotlin.jvm.JvmStatic")
        }

        apiReferences {
        }

        declarations {
          agnostic("com.subject.Utils")
          agnostic("com.subject.Utils.someFunction")
          java("com.subject.Utils.INSTANCE")
          java("com.subject.Utils.INSTANCE.someFunction")
        }
      }
    }

    @Test
    fun `object JvmStatic function with JvmName`() = test {
      val file = project.createFile(
        """
          package com.subject

          object Utils {
            @JvmStatic
            @JvmName("alternate")
            fun someFunction() = Unit
          }
        """
      )

      file shouldBe {
        references {
          explicitKotlin("kotlin.Unit")
          explicitKotlin("kotlin.jvm.JvmName")
          explicitKotlin("kotlin.jvm.JvmStatic")
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
      val file = project.createFile(
        """
          package com.subject

          object Utils {
            @JvmName("alternate")
            fun someFunction() = Unit
          }
        """
      )

      file shouldBe {
        references {
          explicitKotlin("kotlin.Unit")
          explicitKotlin("kotlin.jvm.JvmName")
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

      val file = androidProject.createFile(
        """
        package com.subject

        val someString = R.string.app_name
        """
      )

      file shouldBe {
        references {
          unqualifiedAndroidResource("R.string.app_name")
          androidR("com.subject.R")
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

      file shouldBe {
        references {
          androidR("com.modulecheck.other.R")
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

      file shouldBe {
        references {
          androidR("com.modulecheck.other.R")
          explicitKotlin("com.modulecheck.other.R.string")
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

      file shouldBe {
        references {

          androidR("com.subject.R")
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

      file shouldBe {
        references {

          androidR("com.modulecheck.other.R")
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

      val file = project.createFile(
        """
          package com.subject.internal

          import com.modulecheck.other.R.*

          val someString = string.app_name
        """
      )

      file shouldBe {
        references {

          androidR("com.modulecheck.other.R")
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

      val file = project.createFile(
        """
          package com.subject

          import com.modulecheck.other.R.string

          val someString = string.app_name
        """
      )

      file shouldBe {
        references {
          androidR("com.modulecheck.other.R")
          explicitKotlin("com.modulecheck.other.R.string")
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

      val file = project.createFile(
        """
          package com.subject

          import com.modulecheck.other.databinding.FragmentOtherBinding

          val binding = FragmentOtherBinding.inflate()
        """
      )

      file shouldBe {
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

      val file = project.createFile(
        """
          package com.subject

          val binding = com.modulecheck.other.databinding.FragmentOtherBinding.inflate()
        """
      )

      file shouldBe {
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

      val file = project.createFile(
        """
          package com.subject

          import com.modulecheck.other.databinding.*

          val binding = FragmentOtherBinding.inflate()
        """
      )

      file shouldBe {
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

  fun McProject.createFile(
    @Language("kotlin")
    content: String,
    sourceSetName: SourceSetName = SourceSetName.MAIN
  ): RealKotlinFile = runBlocking {
    editSimple {
      addKotlinSource(content, sourceSetName)
    }.jvmFiles()
      .get(sourceSetName)
      .filterIsInstance<RealKotlinFile>()
      .first { it.ktFile.text == content.trimIndent() }
  }
}
