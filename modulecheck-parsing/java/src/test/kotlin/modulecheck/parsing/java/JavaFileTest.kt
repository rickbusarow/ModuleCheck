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

package modulecheck.parsing.java

import kotlinx.coroutines.flow.filterIsInstance
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.single
import kotlinx.coroutines.runBlocking
import modulecheck.api.context.jvmFiles
import modulecheck.model.dependency.ConfigurationName
import modulecheck.model.sourceset.SourceSetName
import modulecheck.parsing.source.McName.CompatibleLanguage
import modulecheck.parsing.source.McName.CompatibleLanguage.JAVA
import modulecheck.parsing.source.PackageName
import modulecheck.parsing.source.PackageName.Companion.asPackageName
import modulecheck.parsing.source.ReferenceName
import modulecheck.parsing.test.McNameTest
import modulecheck.project.McProject
import modulecheck.project.test.ProjectTest
import modulecheck.utils.trace.Trace
import org.intellij.lang.annotations.Language
import org.jetbrains.kotlin.config.JvmTarget
import org.jetbrains.kotlin.config.JvmTarget.JVM_11
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test

internal class JavaFileTest : ProjectTest(), McNameTest {

  override val defaultLanguage: CompatibleLanguage
    get() = JAVA

  @Nested
  inner class `resolvable declarations` {

    @Test
    fun `enum constants should count as declarations`() {

      val file = createFile(
        """
        package com.subject;

        public enum Color { RED, BLUE }
        """
      )
      file shouldBe {

        apiReferences {}
        references {}
        declarations {
          agnostic("com.subject.Color")
          agnostic("com.subject.Color.RED")
          agnostic("com.subject.Color.BLUE")
        }
      }
    }

    @Test
    fun `nested enum constants should count as declarations`() {

      val file = createFile(
        """
        package com.subject;

        public class Constants {
          public enum Color { RED, BLUE }
        }
        """
      )

      file shouldBe {

        apiReferences {}
        references {}
        declarations {
          agnostic("com.subject.Constants")
          agnostic("com.subject.Constants.Color")
          agnostic("com.subject.Constants.Color.RED")
          agnostic("com.subject.Constants.Color.BLUE")
        }
      }
    }

    @Test
    fun `declared constants should count as declarations`() {

      val file = createFile(
        """
        package com.subject;

        public class Constants {

          public static final int MY_VALUE = 250;
        }
        """
      )

      file shouldBe {

        apiReferences {}
        references {}
        declarations {
          agnostic("com.subject.Constants")
          agnostic("com.subject.Constants.MY_VALUE")
        }
      }
    }

    @Test
    fun `declared nested constants should count as declarations`() {

      val file = createFile(
        """
        package com.subject;

        public class Constants {

          public static class Values {

            public static final int MY_VALUE = 250;
          }
        }
        """
      )

      file shouldBe {

        apiReferences {}
        references {}
        declarations {
          agnostic("com.subject.Constants")
          agnostic("com.subject.Constants.Values")
          agnostic("com.subject.Constants.Values.MY_VALUE")
        }
      }
    }

    @Test
    fun `public static methods should count as declarations`() {

      val file = createFile(
        """
        package com.subject;

        public class ParsedClass {

          public static void foo() {}
        }
        """
      )

      file shouldBe {

        apiReferences {}
        references {}
        declarations {
          agnostic("com.subject.ParsedClass")
          agnostic("com.subject.ParsedClass.foo")
        }
      }
    }

    @Test
    fun `a record should count as a declaration`() {

      val file = createFile(
        //language=text
        """
        package com.subject;

        import com.lib1.Lib1Class;

        public static record MyRecord(Lib1Class lib1Class) {}
        """,
        jvmTarget = JvmTarget.JVM_16
      )

      file shouldBe {

        apiReferences {}
        references {
          java("com.lib1.Lib1Class")
        }
        declarations {
          agnostic("com.subject.MyRecord")
        }
      }
    }

    // reproducer for https://github.com/RBusarow/ModuleCheck/issues/399
    @Test
    fun `file without package should put declarations at the root`() {

      val file = createFile(
        //language=text
        """

        import com.lib1.Lib1Class;

        public static record MyRecord(Lib1Class lib1Class) {}
        """,
        jvmTarget = JvmTarget.JVM_16
      )

      file shouldBe {

        apiReferences {}
        references {
          java("com.lib1.Lib1Class")
        }
        declarations {
          agnostic("MyRecord", PackageName.DEFAULT)
        }
      }
    }

    @Test
    fun `file without imports should still parse`() {

      val file = createFile(
        """
        package com.subject;

        public class MyClass {}
        """
      )

      file shouldBe {

        apiReferences {}
        references {}
        declarations {
          agnostic("com.subject.MyClass")
        }
      }
    }
  }

  @Nested
  inner class `api references` {

    @Test
    fun `public method return type should count as api reference`() {

      val file = createFile(
        """
        package com.subject;

        import com.lib1.Lib1Class;

        public class ParsedClass {

          public Lib1Class foo() { return Lib1Class(); }
        }
        """
      )

      file shouldBe {

        apiReferences {
          java("com.lib1.Lib1Class")
        }
        references {
          java("com.lib1.Lib1Class")
        }
        declarations {
          agnostic("com.subject.ParsedClass")
          agnostic("com.subject.ParsedClass.foo")
        }
      }
    }

    @Test
    fun `private method return type should not count as api reference`() {

      val file = createFile(
        """
        package com.subject;

        import com.lib1.Lib1Class;

        public class ParsedClass {

          private Lib1Class foo() { return Lib1Class(); }
        }
        """
      )

      file shouldBe {

        apiReferences {}
        references {
          java("com.lib1.Lib1Class")
        }
        declarations {
          agnostic("com.subject.ParsedClass")
        }
      }
    }

    @Test
    fun `package-private method return type should not count as api reference`() {

      val file = createFile(
        """
        package com.subject;

        import com.lib1.Lib1Class;

        public class ParsedClass {

          Lib1Class foo() { return Lib1Class(); }
        }
        """
      )

      file shouldBe {

        apiReferences {}
        references {
          java("com.lib1.Lib1Class")
        }
        declarations {
          agnostic("com.subject.ParsedClass")
          agnostic("com.subject.ParsedClass.foo")
        }
      }
    }

    @Test
    fun `public method with wildcard-imported return type should count as api reference`() {

      val file = createFile(
        """
        package com.subject;

        import com.lib1.*;

        public class ParsedClass {

          public Lib1Class foo() { return Lib1Class(); }
        }
        """
      )

      file shouldBe {

        apiReferences {
          java("com.lib1.Lib1Class")
        }
        references {
          java("com.lib1.Lib1Class")
        }
        declarations {
          agnostic("com.subject.ParsedClass")
          agnostic("com.subject.ParsedClass.foo")
        }
      }
    }

    @Test
    fun `public method with fully qualified return type should count as api reference`() {

      val file = createFile(
        """
        package com.subject;

        public class ParsedClass {

          public com.lib1.Lib1Class foo() { return com.lib1.Lib1Class(); }
        }
        """
      )

      file shouldBe {

        apiReferences {
          java("com.lib1.Lib1Class")
          java("com.subject.com.lib1.Lib1Class")
        }
        references {
          java("com.lib1.Lib1Class")
          java("com.subject.com.lib1.Lib1Class")
        }
        declarations {
          agnostic("com.subject.ParsedClass")
          agnostic("com.subject.ParsedClass.foo")
        }
      }
    }

    @Test
    fun `public method parameterized return type should count as api reference`() {

      val file = createFile(
        """
        package com.subject;

        import com.lib1.Lib1Class;
        import java.util.List;

        public class ParsedClass {

          public List<Lib1Class> foo() { return null; }
        }
        """
      )

      file shouldBe {

        apiReferences {
          java("com.lib1.Lib1Class")
          java("java.util.List")
        }
        references {
          java("com.lib1.Lib1Class")
          java("java.util.List")
        }
        declarations {
          agnostic("com.subject.ParsedClass")
          agnostic("com.subject.ParsedClass.foo")
        }
      }
    }

    @Test
    fun `public method generic return type parameter should not count as api reference`() {

      val file = createFile(
        """
        package com.subject;

        import java.util.List;

        public class ParsedClass {

          public <E> List<E> foo() { return null; }
        }
        """
      )

      file shouldBe {

        apiReferences {
          java("java.util.List")
        }
        references {
          java("java.util.List")
        }
        declarations {
          agnostic("com.subject.ParsedClass")
          agnostic("com.subject.ParsedClass.foo")
        }
      }
    }

    @Test
    fun `import should not be an api reference if it isn't actually part of an api reference`() {

      val file = createFile(
        """
        package com.subject;

        import com.lib1.Lib1Class;
        import java.util.List;

        public class ParsedClass {

          public <E> List<E> foo() { return null; }
        }
        """
      )

      file shouldBe {

        apiReferences {
          java("java.util.List")
        }
        references {
          java("com.lib1.Lib1Class")
          java("java.util.List")
        }
        declarations {
          agnostic("com.subject.ParsedClass")
          agnostic("com.subject.ParsedClass.foo")
        }
      }
    }

    @Test
    fun `public method generic return type parameter bound should count as api reference`() {

      val file = createFile(
        """
        package com.subject;

        import java.util.List;

        public class ParsedClass {

          public <E extends CharSequence> List<E> foo() { return null; }
        }
        """
      )

      file shouldBe {

        apiReferences {
          java("java.util.List")
          java("java.lang.CharSequence")
        }
        references {
          java("java.util.List")
          java("java.lang.CharSequence")
        }
        declarations {
          agnostic("com.subject.ParsedClass")
          agnostic("com.subject.ParsedClass.foo")
        }
      }
    }

    @Test
    fun `public method argument should count as api reference`() {

      val file = createFile(
        """
        package com.subject;

        import java.util.List;

        public class ParsedClass {

          public <E> List<E> foo(String name) { return null; }
        }
        """
      )
      file shouldBe {

        apiReferences {
          java("java.util.List")
          java("java.lang.String")
        }
        references {
          java("java.util.List")
          java("java.lang.String")
        }
        declarations {
          agnostic("com.subject.ParsedClass")
          agnostic("com.subject.ParsedClass.foo")
        }
      }
    }

    @Test
    fun `public member property type with wildcard import should count as api reference`() =
      test {

        val file = createFile(
          """
          package com.subject;

          import com.lib1.*;

          public class ParsedClass {

            public Lib1Class lib1Class;
          }
          """
        )

        file shouldBe {

          apiReferences {
            java("com.lib1.Lib1Class")
          }
          references {
            java("com.lib1.Lib1Class")
          }
          declarations {
            agnostic("com.subject.ParsedClass")
            agnostic("com.subject.ParsedClass.lib1Class")
          }
        }
      }

    @Test
    fun `public member property type with import should count as api reference`() =
      test {

        val file = createFile(
          """
          package com.subject;

          import com.lib1.Lib1Class;

          public class ParsedClass {

            public Lib1Class lib1Class;
          }
          """
        )

        file shouldBe {

          apiReferences {
            java("com.lib1.Lib1Class")
          }
          references {
            java("com.lib1.Lib1Class")
          }
          declarations {
            agnostic("com.subject.ParsedClass")
            agnostic("com.subject.ParsedClass.lib1Class")
          }
        }
      }

    @Test
    fun `a public member property with generic type with wildcard import should count as api reference`() =
      test {

        val file = createFile(
          """
          package com.subject;

          import com.lib1.*;
          import java.util.List;

          public class ParsedClass {

            public List<Lib1Class> lib1Classes;
          }
          """
        )

        file shouldBe {

          apiReferences {
            java("java.util.List")
            java("com.lib1.Lib1Class")
          }
          references {
            java("java.util.List")
            java("com.lib1.Lib1Class")
          }
          declarations {
            agnostic("com.subject.ParsedClass")
            agnostic("com.subject.ParsedClass.lib1Classes")
          }
        }
      }
  }

  @Nested
  inner class `Android resource references` {

    @Test
    fun `unqualified android resource reference in base package`() = test {

      val project = androidLibrary(":lib1", "com.subject")

      val file = project.createFile(
        """
        package com.subject;

        public class ParsedClass {

          int someString = R.string.app_name;
        }
        """
      )

      file shouldBe {

        apiReferences {}
        references {
          unqualifiedAndroidResource("R.string.app_name")
          androidR("com.subject".asPackageName())
          qualifiedAndroidResource("com.subject.R.string.app_name")
        }
        declarations {
          agnostic("com.subject.ParsedClass")
          agnostic("com.subject.ParsedClass.someString")
        }
      }
    }

    @Test
    fun `unqualified android resource reference with R import`() = test {

      val otherLib = androidLibrary(":other", "com.modulecheck.other")

      val project = androidLibrary(":lib1", "com.lib1") {
        addDependency(ConfigurationName.implementation, otherLib)

        addJavaSource(
          """
          package com.subject;

          import com.modulecheck.other.R;

          public class ParsedClass {

            int someString = R.string.app_name;
          }
          """
        )
      }

      val file = project.jvmFiles().get(SourceSetName.MAIN).single()

      file shouldBe {

        apiReferences {}
        references {
          androidR("com.modulecheck.other".asPackageName())
          qualifiedAndroidResource("com.modulecheck.other.R.string.app_name")
          unqualifiedAndroidResource("R.string.app_name")
        }
        declarations {
          agnostic("com.subject.ParsedClass")
          agnostic("com.subject.ParsedClass.someString")
        }
      }
    }

    @Test
    fun `android resource reference with R string import`() = test {

      val otherLib = androidLibrary(":other", "com.modulecheck.other")

      val project = androidLibrary(":lib1", "com.lib1") {
        addDependency(ConfigurationName.implementation, otherLib)

        addJavaSource(
          """
          package com.subject;

          import com.modulecheck.other.R.string;

          public class ParsedClass {

            int someString = string.app_name;
          }
          """
        )
      }

      val file = project.jvmFiles().get(SourceSetName.MAIN).single()

      file shouldBe {

        apiReferences {}
        references {
          androidR("com.modulecheck.other".asPackageName())
          java("com.modulecheck.other.R.string")
          qualifiedAndroidResource("com.modulecheck.other.R.string.app_name")
          unqualifiedAndroidResource("R.string.app_name")
        }
        declarations {
          agnostic("com.subject.ParsedClass")
          agnostic("com.subject.ParsedClass.someString")
        }
      }
    }

    @Test
    fun `android resource reference with wildcard R import in base package`() = test {

      val otherLib = androidLibrary(":other", "com.modulecheck.other")

      val project = androidLibrary(":lib1", "com.lib1") {
        addDependency(ConfigurationName.implementation, otherLib)

        addJavaSource(
          """
          package com.subject;

          import com.modulecheck.other.*;

          public class ParsedClass {

            int someString = R.string.app_name;
          }
          """
        )
      }

      val file = project.jvmFiles().get(SourceSetName.MAIN).single()

      file shouldBe {

        apiReferences {}
        references {
          androidR("com.modulecheck.other".asPackageName())
          qualifiedAndroidResource("com.modulecheck.other.R.string.app_name")
          unqualifiedAndroidResource("R.string.app_name")
        }
        declarations {
          agnostic("com.subject.ParsedClass")
          agnostic("com.subject.ParsedClass.someString")
        }
      }
    }

    @Test
    fun `android resource reference with wildcard R import not in base package`() = test {

      val otherLib = androidLibrary(":other", "com.modulecheck.other")

      val project = androidLibrary(":lib1", "com.lib1") {
        addDependency(ConfigurationName.implementation, otherLib)

        addJavaSource(
          """
          package com.subject.internal;

          import com.modulecheck.other.*;

          public class ParsedClass {

            int someString = R.string.app_name;
          }
          """
        )
      }

      val file = project.jvmFiles().get(SourceSetName.MAIN).single()

      file shouldBe {

        apiReferences {}
        references {
          androidR("com.modulecheck.other".asPackageName())
          qualifiedAndroidResource("com.modulecheck.other.R.string.app_name")
          unqualifiedAndroidResource("R.string.app_name")
        }
        declarations {
          agnostic("com.subject.internal.ParsedClass")
          agnostic("com.subject.internal.ParsedClass.someString")
        }
      }
    }

    @Test
    fun `android resource reference with wildcard R member import`() = test {

      val otherLib = androidLibrary(":other", "com.modulecheck.other")

      val project = androidLibrary(":lib1", "com.lib1") {
        addDependency(ConfigurationName.implementation, otherLib)
      }

      val file = project.createFile(
        """
        package com.subject.internal;

        import com.modulecheck.other.R.*;

        public class ParsedClass {

          int someString = string.app_name;
        }
        """
      )

      file shouldBe {

        apiReferences {}
        references {
          androidR("com.modulecheck.other".asPackageName())
          qualifiedAndroidResource("com.modulecheck.other.R.string.app_name")
          unqualifiedAndroidResource("R.string.app_name")
        }
        declarations {
          agnostic("com.subject.internal.ParsedClass")
          agnostic("com.subject.internal.ParsedClass.someString")
        }
      }
    }

    @Test
    fun `android resource reference with explicit R string import`() = test {

      val otherLib = androidLibrary(":other", "com.modulecheck.other")

      val project = androidLibrary(":lib1", "com.lib1") {
        addDependency(ConfigurationName.implementation, otherLib)
      }

      val file = project.createFile(
        """
        package com.subject;

        import com.modulecheck.other.R.string;

        public class ParsedClass {

          int someString = string.app_name;
        }
        """
      )

      file shouldBe {

        apiReferences {}
        references {
          androidR("com.modulecheck.other".asPackageName())
          java("com.modulecheck.other.R.string")
          qualifiedAndroidResource("com.modulecheck.other.R.string.app_name")
          unqualifiedAndroidResource("R.string.app_name")
        }
        declarations {
          agnostic("com.subject.ParsedClass")
          agnostic("com.subject.ParsedClass.someString")
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

      val project = androidLibrary(":lib1", "com.lib1") {
        addDependency(ConfigurationName.implementation, otherLib)
      }

      val file = project.createFile(
        """
        package com.subject;

        import com.modulecheck.other.databinding.FragmentOtherBinding;

        public class ParsedClass {

          FragmentOtherBinding binding = FragmentOtherBinding.inflate();
        }
        """
      )

      file shouldBe {

        apiReferences {
          java("com.modulecheck.other.databinding.FragmentOtherBinding")
        }
        references {
          androidDataBinding("com.modulecheck.other.databinding.FragmentOtherBinding")
          androidDataBinding("com.modulecheck.other.databinding.FragmentOtherBinding.inflate")
        }
        declarations {
          agnostic("com.subject.ParsedClass")
          agnostic("com.subject.ParsedClass.binding")
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

      val project = androidLibrary(":lib1", "com.lib1") {
        addDependency(ConfigurationName.implementation, otherLib)
      }

      val file = project.createFile(
        """
        package com.subject;

        public class ParsedClass {

          com.modulecheck.other.databinding.FragmentOtherBinding binding = com.modulecheck.other.databinding.FragmentOtherBinding.inflate();
        }
        """
      )

      file shouldBe {

        apiReferences {}
        references {
          androidDataBinding("com.modulecheck.other.databinding.FragmentOtherBinding")
          androidDataBinding("com.modulecheck.other.databinding.FragmentOtherBinding.inflate")
        }
        declarations {
          agnostic("com.subject.ParsedClass")
          agnostic("com.subject.ParsedClass.binding")
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

      val project = androidLibrary(":lib1", "com.lib1") {
        addDependency(ConfigurationName.implementation, otherLib)
      }

      val file = project.createFile(
        """
        package com.subject;

        import com.modulecheck.other.databinding.*;

        public class ParsedClass {

          FragmentOtherBinding binding = FragmentOtherBinding.inflate();
        }
        """
      )

      file shouldBe {

        apiReferences {
          java("com.modulecheck.other.databinding.FragmentOtherBinding")
        }
        references {
          androidDataBinding("com.modulecheck.other.databinding.FragmentOtherBinding")
          androidDataBinding("com.modulecheck.other.databinding.FragmentOtherBinding.inflate")
        }
        declarations {
          agnostic("com.subject.ParsedClass")
          agnostic("com.subject.ParsedClass.binding")
        }
      }
    }
  }

  @Test
  fun `local classes should not count as declarations`() = test {

    val project = androidLibrary(":lib1", "com.lib1")

    val file = project.createFile(
      """
        package com.subject;

        import com.subject.AnInterface;
        import org.junit.Test;

        public class ATest {

          @Test
          public void anonymous_things_can_be_parsed() {
            class AnAnonymousClass implements AnInterface {

              @Override
              public void aMethod() {
              }
            }
          }
        }
        """
    )

    file shouldBe {

      apiReferences {}
      references {
        java("com.subject.AnInterface")
        java("org.junit.Test")
      }
      declarations {
        agnostic("com.subject.ATest")
        agnostic("com.subject.ATest.anonymous_things_can_be_parsed")
      }
    }
  }

  @Test
  fun `multiple variables in a single line should all be declared`() = test {

    val project = androidLibrary(":lib1", "com.lib1")

    val file = project.createFile(
      """
        package com.subject;

        public class ParsedClass {

          public int i, j, k;
        }
        """
    )

    file shouldBe {

      apiReferences {}
      references {}
      declarations {
        agnostic("com.subject.ParsedClass")
        agnostic("com.subject.ParsedClass.i")
        agnostic("com.subject.ParsedClass.j")
        agnostic("com.subject.ParsedClass.k")
      }
    }
  }

  fun java(name: String) = ReferenceName(name, JAVA)

  fun McProject.createFile(
    @Language("java")
    content: String,
    sourceSetName: SourceSetName = SourceSetName.MAIN
  ): RealJavaFile = runBlocking {
    createFile(
      content = content,
      project = this@createFile,
      sourceSetName = sourceSetName
    )
  }

  fun createFile(
    @Language("java")
    content: String,
    project: McProject = simpleProject(),
    sourceSetName: SourceSetName = SourceSetName.MAIN,
    jvmTarget: JvmTarget = JVM_11
  ): RealJavaFile = runBlocking(Trace.start(listOf(JavaFileTest::class))) {
    project.editSimple {
      addJavaSource(content, sourceSetName)
      this.jvmTarget = jvmTarget
    }.jvmFiles()
      .get(sourceSetName)
      .filterIsInstance<RealJavaFile>()
      .first { it.file.readText() == content.trimIndent() }
  }
}
