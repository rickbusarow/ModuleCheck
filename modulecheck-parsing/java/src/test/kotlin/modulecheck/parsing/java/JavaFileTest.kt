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
import modulecheck.parsing.gradle.ConfigurationName
import modulecheck.parsing.gradle.SourceSetName
import modulecheck.parsing.source.JavaVersion
import modulecheck.parsing.source.JavaVersion.VERSION_14
import modulecheck.parsing.source.asExplicitJavaReference
import modulecheck.parsing.source.asInterpretedJavaReference
import modulecheck.parsing.test.NamedSymbolTest
import modulecheck.project.McProject
import modulecheck.project.test.ProjectTest
import org.intellij.lang.annotations.Language
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test

internal class JavaFileTest : ProjectTest(), NamedSymbolTest {

  @Nested
  inner class `resolvable declarations` {

    @Test
    fun `enum constants should count as declarations`() {

      val file = createFile(
        """
        package com.test;

        public enum Color { RED, BLUE }
        """
      )

      file.apiReferences shouldBe listOf()
      file.references shouldBe listOf()
      file.declarations shouldBe setOf(
        agnostic("com.test.Color"),
        agnostic("com.test.Color.RED"),
        agnostic("com.test.Color.BLUE")
      )
    }

    @Test
    fun `nested enum constants should count as declarations`() {

      val file = createFile(
        """
        package com.test;

        public class Constants {
          public enum Color { RED, BLUE }
        }
        """
      )

      file.apiReferences shouldBe listOf()
      file.references shouldBe listOf()
      file.declarations shouldBe setOf(
        agnostic("com.test.Constants"),
        agnostic("com.test.Constants.Color"),
        agnostic("com.test.Constants.Color.RED"),
        agnostic("com.test.Constants.Color.BLUE")
      )
    }

    @Test
    fun `declared constants should count as declarations`() {

      val file = createFile(
        """
        package com.test;

        public class Constants {

          public static final int MY_VALUE = 250;
        }
        """
      )

      file.apiReferences shouldBe listOf()
      file.references shouldBe listOf()
      file.declarations shouldBe setOf(
        agnostic("com.test.Constants"),
        agnostic("com.test.Constants.MY_VALUE")
      )
    }

    @Test
    fun `declared nested constants should count as declarations`() {

      val file = createFile(
        """
        package com.test;

        public class Constants {

          public static class Values {

            public static final int MY_VALUE = 250;
          }
        }
        """
      )

      file.apiReferences shouldBe listOf()
      file.references shouldBe listOf()
      file.declarations shouldBe setOf(
        agnostic("com.test.Constants"),
        agnostic("com.test.Constants.Values"),
        agnostic("com.test.Constants.Values.MY_VALUE")
      )
    }

    @Test
    fun `public static methods should count as declarations`() {

      val file = createFile(
        """
        package com.test;

        public class ParsedClass {

          public static void foo() {}
        }
        """
      )

      file.apiReferences shouldBe listOf()
      file.references shouldBe listOf()
      file.declarations shouldBe setOf(
        agnostic("com.test.ParsedClass"),
        agnostic("com.test.ParsedClass.foo")
      )
    }

    @Test
    fun `a record should count as a declaration`() {

      val file = createFile(
        //language=text
        """
        package com.test;

        import com.lib1.Lib1Class;

        public static record MyRecord(Lib1Class lib1Class) {}
        """,
        javaVersion = JavaVersion.VERSION_16
      )

      file.apiReferences shouldBe listOf()
      file.references shouldBe listOf(
        explicit("com.lib1.Lib1Class")
      )
      file.declarations shouldBe setOf(
        agnostic("com.test.MyRecord")
      )
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
        javaVersion = JavaVersion.VERSION_16
      )

      file.apiReferences shouldBe listOf()
      file.references shouldBe listOf(
        explicit("com.lib1.Lib1Class")
      )
      file.declarations shouldBe setOf(
        agnostic("MyRecord")
      )
    }

    @Test
    fun `file without imports should still parse`() {

      val file = createFile(
        """
        package com.test;

        public class MyClass {}
        """,
        javaVersion = JavaVersion.VERSION_16
      )

      file.apiReferences shouldBe listOf()
      file.references shouldBe listOf()
      file.declarations shouldBe setOf(
        agnostic("com.test.MyClass")
      )
    }
  }

  @Nested
  inner class `api references` {

    @Test
    fun `public method return type should count as api reference`() {

      val file = createFile(
        """
        package com.test;

        import com.lib1.Lib1Class;

        public class ParsedClass {

          public Lib1Class foo() { return Lib1Class(); }
        }
        """
      )

      file.apiReferences shouldBe listOf(
        explicit("com.lib1.Lib1Class")
      )
      file.references shouldBe listOf(
        explicit("com.lib1.Lib1Class")
      )
      file.declarations shouldBe setOf(
        agnostic("com.test.ParsedClass"),
        agnostic("com.test.ParsedClass.foo")
      )
    }

    @Test
    fun `private method return type should not count as api reference`() {

      val file = createFile(
        """
        package com.test;

        import com.lib1.Lib1Class;

        public class ParsedClass {

          private Lib1Class foo() { return Lib1Class(); }
        }
        """
      )

      file.apiReferences shouldBe listOf()
      file.references shouldBe listOf(
        explicit("com.lib1.Lib1Class")
      )
      file.declarations shouldBe setOf(
        agnostic("com.test.ParsedClass")
      )
    }

    @Test
    fun `package-private method return type should not count as api reference`() {

      val file = createFile(
        """
        package com.test;

        import com.lib1.Lib1Class;

        public class ParsedClass {

          Lib1Class foo() { return Lib1Class(); }
        }
        """
      )

      file.apiReferences shouldBe listOf()
      file.references shouldBe listOf(
        explicit("com.lib1.Lib1Class")
      )
      file.declarations shouldBe setOf(
        agnostic("com.test.ParsedClass"),
        agnostic("com.test.ParsedClass.foo")
      )
    }

    @Test
    fun `public method with wildcard-imported return type should count as api reference`() {

      val file = createFile(
        """
        package com.test;

        import com.lib1.*;

        public class ParsedClass {

          public Lib1Class foo() { return Lib1Class(); }
        }
        """
      )

      file.apiReferences shouldBe listOf(
        interpreted("com.test.Lib1Class"),
        interpreted("com.lib1.Lib1Class"),
        interpreted("Lib1Class")
      )
      file.references shouldBe listOf(
        interpreted("com.test.Lib1Class"),
        interpreted("com.lib1.Lib1Class"),
        interpreted("Lib1Class")
      )
      file.declarations shouldBe setOf(
        agnostic("com.test.ParsedClass"),
        agnostic("com.test.ParsedClass.foo")
      )
    }

    @Test
    fun `public method with fully qualified return type should count as api reference`() {

      val file = createFile(
        """
        package com.test;

        public class ParsedClass {

          public com.lib1.Lib1Class foo() { return com.lib1.Lib1Class(); }
        }
        """
      )

      file.apiReferences shouldBe listOf(
        interpreted("com.lib1.Lib1Class"),
        interpreted("com.test.com.lib1.Lib1Class")
      )
      file.references shouldBe listOf(
        interpreted("com.lib1.Lib1Class"),
        interpreted("com.test.com.lib1.Lib1Class")
      )
      file.declarations shouldBe setOf(
        agnostic("com.test.ParsedClass"),
        agnostic("com.test.ParsedClass.foo")
      )
    }

    @Test
    fun `public method parameterized return type should count as api reference`() {

      val file = createFile(
        """
        package com.test;

        import com.lib1.Lib1Class;
        import java.util.List;

        public class ParsedClass {

          public List<Lib1Class> foo() { return null; }
        }
        """
      )

      file.apiReferences shouldBe listOf(
        explicit("com.lib1.Lib1Class"),
        explicit("java.util.List")
      )
      file.references shouldBe listOf(
        explicit("com.lib1.Lib1Class"),
        explicit("java.util.List")
      )
      file.declarations shouldBe setOf(
        agnostic("com.test.ParsedClass"),
        agnostic("com.test.ParsedClass.foo")
      )
    }

    @Test
    fun `public method generic return type parameter should not count as api reference`() {

      val file = createFile(
        """
        package com.test;

        import java.util.List;

        public class ParsedClass {

          public <E> List<E> foo() { return null; }
        }
        """
      )

      file.apiReferences shouldBe listOf(
        explicit("java.util.List")
      )
      file.references shouldBe listOf(
        explicit("java.util.List")
      )
      file.declarations shouldBe setOf(
        agnostic("com.test.ParsedClass"),
        agnostic("com.test.ParsedClass.foo")
      )
    }

    @Test
    fun `import should not be an api reference if it isn't actually part of an api reference`() {

      val file = createFile(
        """
        package com.test;

        import com.lib1.Lib1Class;
        import java.util.List;

        public class ParsedClass {

          public <E> List<E> foo() { return null; }
        }
        """
      )

      file.apiReferences shouldBe listOf(
        explicit("java.util.List")
      )
      file.references shouldBe listOf(
        explicit("com.lib1.Lib1Class"),
        explicit("java.util.List")
      )
      file.declarations shouldBe setOf(
        agnostic("com.test.ParsedClass"),
        agnostic("com.test.ParsedClass.foo")
      )
    }

    @Test
    fun `public method generic return type parameter bound should count as api reference`() {

      val file = createFile(
        """
        package com.test;

        import java.util.List;

        public class ParsedClass {

          public <E extends CharSequence> List<E> foo() { return null; }
        }
        """
      )

      file.apiReferences shouldBe listOf(
        explicit("java.util.List"),
        explicit("java.lang.CharSequence")
      )
      file.references shouldBe listOf(
        explicit("java.util.List"),
        explicit("java.lang.CharSequence")
      )
      file.declarations shouldBe setOf(
        agnostic("com.test.ParsedClass"),
        agnostic("com.test.ParsedClass.foo")
      )
    }

    @Test
    fun `public method argument should count as api reference`() {

      val file = createFile(
        """
        package com.test;

        import java.util.List;

        public class ParsedClass {

          public <E> List<E> foo(String name) { return null; }
        }
        """
      )

      file.apiReferences shouldBe listOf(
        explicit("java.util.List"),
        explicit("java.lang.String")
      )
      file.references shouldBe listOf(
        explicit("java.util.List"),
        explicit("java.lang.String")
      )
      file.declarations shouldBe setOf(
        agnostic("com.test.ParsedClass"),
        agnostic("com.test.ParsedClass.foo")
      )
    }

    @Test
    fun `public member property type with wildcard import should count as api reference`() =
      test {

        val file = createFile(
          """
          package com.test;

          import com.lib1.*;

          public class ParsedClass {

            public Lib1Class lib1Class;
          }
          """
        )

        file.apiReferences shouldBe listOf(
          interpreted("Lib1Class"),
          interpreted("com.lib1.Lib1Class"),
          interpreted("com.test.Lib1Class")
        )
        file.references shouldBe listOf(
          interpreted("Lib1Class"),
          interpreted("com.lib1.Lib1Class"),
          interpreted("com.test.Lib1Class")
        )
        file.declarations shouldBe setOf(
          agnostic("com.test.ParsedClass"),
          agnostic("com.test.ParsedClass.lib1Class")
        )
      }

    @Test
    fun `public member property type with import should count as api reference`() =
      test {

        val file = createFile(
          """
          package com.test;

          import com.lib1.Lib1Class;

          public class ParsedClass {

            public Lib1Class lib1Class;
          }
          """
        )

        file.apiReferences shouldBe listOf(
          explicit("com.lib1.Lib1Class")
        )
        file.references shouldBe listOf(
          explicit("com.lib1.Lib1Class")
        )
        file.declarations shouldBe setOf(
          agnostic("com.test.ParsedClass"),
          agnostic("com.test.ParsedClass.lib1Class")
        )
      }

    @Test
    fun `a public member property with generic type with wildcard import should count as api reference`() =
      test {

        val file = createFile(
          """
          package com.test;

          import com.lib1.*;
          import java.util.List;

          public class ParsedClass {

            public List<Lib1Class> lib1Classes;
          }
          """
        )

        file.apiReferences shouldBe listOf(
          interpreted("Lib1Class"),
          explicit("java.util.List"),
          interpreted("com.lib1.Lib1Class"),
          interpreted("com.test.Lib1Class")
        )
        file.references shouldBe listOf(
          interpreted("Lib1Class"),
          explicit("java.util.List"),
          interpreted("com.lib1.Lib1Class"),
          interpreted("com.test.Lib1Class")
        )
        file.declarations shouldBe setOf(
          agnostic("com.test.ParsedClass"),
          agnostic("com.test.ParsedClass.lib1Classes")
        )
      }
  }

  @Nested
  inner class `Android resource references` {

    @Test
    fun `unqualified android resource reference in base package`() = test {

      val project = androidLibrary(":lib1", "com.test")

      val file = project.createFile(
        """
        package com.test;

        public class ParsedClass {

          int someString = R.string.app_name;
        }
        """
      )

      file.references shouldBe listOf(
        unqualifiedAndroidResource("R.string.app_name"),
        androidR("com.test.R"),
        qualifiedAndroidResource("com.test.R.string.app_name")
      )

      file.declarations shouldBe listOf(
        agnostic("com.test.ParsedClass"),
        agnostic("com.test.ParsedClass.someString")
      )
    }

    @Test
    fun `unqualified android resource reference with R import`() = test {

      val otherLib = androidLibrary(":other", "com.modulecheck.other")

      val project = androidLibrary(":lib1", "com.test") {
        addDependency(ConfigurationName.implementation, otherLib)

        addJavaSource(
          """
          package com.test;

          import com.modulecheck.other.R;

          public class ParsedClass {

            int someString = R.string.app_name;
          }
          """
        )
      }

      val file = project.jvmFiles().get(SourceSetName.MAIN).single()

      file.references shouldBe listOf(
        androidR("com.modulecheck.other.R"),
        qualifiedAndroidResource("com.modulecheck.other.R.string.app_name"),
        unqualifiedAndroidResource("R.string.app_name")
      )

      file.declarations shouldBe listOf(
        agnostic("com.test.ParsedClass"),
        agnostic("com.test.ParsedClass.someString")
      )
    }

    @Test
    fun `android resource reference with R string import`() = test {

      val otherLib = androidLibrary(":other", "com.modulecheck.other")

      val project = androidLibrary(":lib1", "com.test") {
        addDependency(ConfigurationName.implementation, otherLib)

        addJavaSource(
          """
          package com.test;

          import com.modulecheck.other.R.string;

          public class ParsedClass {

            int someString = string.app_name;
          }
          """
        )
      }

      val file = project.jvmFiles().get(SourceSetName.MAIN).single()

      file.references shouldBe listOf(
        androidR("com.modulecheck.other.R"),
        explicit("com.modulecheck.other.R.string"),
        qualifiedAndroidResource("com.modulecheck.other.R.string.app_name"),
        unqualifiedAndroidResource("R.string.app_name")
      )

      file.declarations shouldBe listOf(
        agnostic("com.test.ParsedClass"),
        agnostic("com.test.ParsedClass.someString")
      )
    }

    @Test
    fun `android resource reference with wildcard R import in base package`() = test {

      val otherLib = androidLibrary(":other", "com.modulecheck.other")

      val project = androidLibrary(":lib1", "com.test") {
        addDependency(ConfigurationName.implementation, otherLib)

        addJavaSource(
          """
          package com.test;

          import com.modulecheck.other.*;

          public class ParsedClass {

            int someString = R.string.app_name;
          }
          """
        )
      }

      val file = project.jvmFiles().get(SourceSetName.MAIN).single()

      file.references shouldBe listOf(
        androidR("com.test.R"),
        qualifiedAndroidResource("com.test.R.string.app_name"),
        unqualifiedAndroidResource("R.string.app_name")
      )

      file.declarations shouldBe listOf(
        agnostic("com.test.ParsedClass"),
        agnostic("com.test.ParsedClass.someString")
      )
    }

    @Test
    fun `android resource reference with wildcard R import not in base package`() = test {

      val otherLib = androidLibrary(":other", "com.modulecheck.other")

      val project = androidLibrary(":lib1", "com.test") {
        addDependency(ConfigurationName.implementation, otherLib)

        addJavaSource(
          """
          package com.test.internal;

          import com.modulecheck.other.*;

          public class ParsedClass {

            int someString = R.string.app_name;
          }
          """
        )
      }

      val file = project.jvmFiles().get(SourceSetName.MAIN).single()

      file.references shouldBe listOf(
        androidR("com.modulecheck.other.R"),
        qualifiedAndroidResource("com.modulecheck.other.R.string.app_name"),
        unqualifiedAndroidResource("R.string.app_name")
      )

      file.declarations shouldBe listOf(
        agnostic("com.test.internal.ParsedClass"),
        agnostic("com.test.internal.ParsedClass.someString")
      )
    }

    @Test
    fun `android resource reference with wildcard R member import`() = test {

      val otherLib = androidLibrary(":other", "com.modulecheck.other")

      val project = androidLibrary(":lib1", "com.test") {
        addDependency(ConfigurationName.implementation, otherLib)
      }

      val file = project.createFile(
        """
        package com.test.internal;

        import com.modulecheck.other.R.*;

        public class ParsedClass {

          int someString = string.app_name;
        }
        """
      )

      file.references shouldBe listOf(
        androidR("com.modulecheck.other.R"),
        qualifiedAndroidResource("com.modulecheck.other.R.string.app_name"),
        unqualifiedAndroidResource("R.string.app_name")
      )

      file.declarations shouldBe listOf(
        agnostic("com.test.internal.ParsedClass"),
        agnostic("com.test.internal.ParsedClass.someString")
      )
    }

    @Test
    fun `android resource reference with explicit R string import`() = test {

      val otherLib = androidLibrary(":other", "com.modulecheck.other")

      val project = androidLibrary(":lib1", "com.test") {
        addDependency(ConfigurationName.implementation, otherLib)
      }

      val file = project.createFile(
        """
        package com.test;

        import com.modulecheck.other.R.string;

        public class ParsedClass {

          int someString = string.app_name;
        }
        """
      )

      file.references shouldBe listOf(
        androidR("com.modulecheck.other.R"),
        explicit("com.modulecheck.other.R.string"),
        qualifiedAndroidResource("com.modulecheck.other.R.string.app_name"),
        unqualifiedAndroidResource("R.string.app_name")
      )

      file.declarations shouldBe listOf(
        agnostic("com.test.ParsedClass"),
        agnostic("com.test.ParsedClass.someString")
      )
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

      val project = androidLibrary(":lib1", "com.test") {
        addDependency(ConfigurationName.implementation, otherLib)
      }

      val file = project.createFile(
        """
        package com.test;

        import com.modulecheck.other.databinding.FragmentOtherBinding;

        public class ParsedClass {

          FragmentOtherBinding binding = FragmentOtherBinding.inflate();
        }
        """
      )

      file.references shouldBe listOf(
        androidDataBinding("com.modulecheck.other.databinding.FragmentOtherBinding"),
        androidDataBinding("com.modulecheck.other.databinding.FragmentOtherBinding.inflate")
      )

      file.declarations shouldBe listOf(
        agnostic("com.test.ParsedClass"),
        agnostic("com.test.ParsedClass.binding")
      )
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

      val project = androidLibrary(":lib1", "com.test") {
        addDependency(ConfigurationName.implementation, otherLib)
      }

      val file = project.createFile(
        """
        package com.test;

        public class ParsedClass {

          com.modulecheck.other.databinding.FragmentOtherBinding binding = com.modulecheck.other.databinding.FragmentOtherBinding.inflate();
        }
        """
      )

      file.references shouldBe listOf(
        androidDataBinding("com.modulecheck.other.databinding.FragmentOtherBinding"),
        androidDataBinding("com.modulecheck.other.databinding.FragmentOtherBinding.inflate")
      )

      file.declarations shouldBe listOf(
        agnostic("com.test.ParsedClass"),
        agnostic("com.test.ParsedClass.binding")
      )
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

      val project = androidLibrary(":lib1", "com.test") {
        addDependency(ConfigurationName.implementation, otherLib)
      }

      val file = project.createFile(
        """
        package com.test;

        import com.modulecheck.other.databinding.*;

        public class ParsedClass {

          FragmentOtherBinding binding = FragmentOtherBinding.inflate();
        }
        """
      )

      file.references shouldBe listOf(
        androidDataBinding("com.modulecheck.other.databinding.FragmentOtherBinding"),
        androidDataBinding("com.modulecheck.other.databinding.FragmentOtherBinding.inflate")
      )

      file.declarations shouldBe listOf(
        agnostic("com.test.ParsedClass"),
        agnostic("com.test.ParsedClass.binding")
      )
    }

    @Test
    fun `local classes should not count as declarations`() = test {

      val project = androidLibrary(":lib1", "com.test")

      val file = project.createFile(
        """
        package com.test;

        import com.test.AnInterface;
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

      file.references shouldBe listOf(
        explicit("com.test.AnInterface"),
        explicit("org.junit.Test")
      )

      file.declarations shouldBe listOf(
        agnostic("com.test.ATest"),
        agnostic("com.test.ATest.anonymous_things_can_be_parsed")
      )
    }

    @Test
    fun `multiple variables in a single line should all be declared`() = test {

      val project = androidLibrary(":lib1", "com.test")

      val file = project.createFile(
        """
        package com.test;

        public class ParsedClass {

          public int i, j, k;
        }
        """
      )

      file.references shouldBe listOf()

      file.declarations shouldBe listOf(
        agnostic("com.test.ParsedClass"),
        agnostic("com.test.ParsedClass.i"),
        agnostic("com.test.ParsedClass.j"),
        agnostic("com.test.ParsedClass.k")
      )
    }
  }

  fun explicit(name: String) = name.asExplicitJavaReference()
  fun interpreted(name: String) = name.asInterpretedJavaReference()

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
    javaVersion: JavaVersion = VERSION_14
  ): RealJavaFile = runBlocking {
    project.editSimple {
      addJavaSource(content, sourceSetName)
      javaSourceVersion = javaVersion
    }.jvmFiles()
      .get(sourceSetName)
      .filterIsInstance<RealJavaFile>()
      .first { it.file.readText() == content.trimIndent() }
  }
}
