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

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.filterIsInstance
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.runBlocking
import modulecheck.api.context.jvmFiles
import modulecheck.parsing.gradle.SourceSetName
import modulecheck.parsing.source.AgnosticDeclaredName
import modulecheck.parsing.source.AndroidRDeclaredName
import modulecheck.parsing.source.AndroidResourceDeclaredName
import modulecheck.parsing.source.DeclaredName
import modulecheck.parsing.source.JavaSpecificDeclaredName
import modulecheck.parsing.source.JavaVersion
import modulecheck.parsing.source.JavaVersion.VERSION_14
import modulecheck.parsing.source.KotlinSpecificDeclaredName
import modulecheck.parsing.source.Reference
import modulecheck.parsing.source.Reference.AndroidRReference
import modulecheck.parsing.source.Reference.ExplicitJavaReference
import modulecheck.parsing.source.Reference.ExplicitKotlinReference
import modulecheck.parsing.source.Reference.ExplicitXmlReference
import modulecheck.parsing.source.Reference.InterpretedJavaReference
import modulecheck.parsing.source.Reference.InterpretedKotlinReference
import modulecheck.parsing.source.Reference.QualifiedAndroidResourceReference
import modulecheck.parsing.source.Reference.UnqualifiedAndroidResourceReference
import modulecheck.parsing.source.asExplicitJavaReference
import modulecheck.parsing.source.asInterpretedJavaReference
import modulecheck.project.McProject
import modulecheck.project.test.ProjectTest
import modulecheck.utils.LazyDeferred
import modulecheck.utils.LazySet
import org.intellij.lang.annotations.Language
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import io.kotest.matchers.shouldBe as kotestShouldBe
import modulecheck.parsing.source.asDeclaredName as neutralExtension
import modulecheck.parsing.source.asJavaDeclaredName as javaExtension
import modulecheck.parsing.source.asKotlinDeclaredName as kotlinExtension

internal class JavaFileTest : ProjectTest() {

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
      file.references() shouldBe listOf()
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
      file.references() shouldBe listOf()
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
      file.references() shouldBe listOf()
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
      file.references() shouldBe listOf()
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
      file.references() shouldBe listOf()
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
      file.references() shouldBe listOf(
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
      file.references() shouldBe listOf(
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
      file.references() shouldBe listOf()
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
      file.references() shouldBe listOf(
        explicit("com.lib1.Lib1Class")
      )
      file.declarations shouldBe setOf(
        agnostic("com.test.ParsedClass")
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
      file.references() shouldBe listOf(
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
      file.references() shouldBe listOf(
        explicit("com.lib1.Lib1Class")
      )
      file.declarations shouldBe setOf(
        agnostic("com.test.ParsedClass")
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
      file.references() shouldBe listOf(
        interpreted("com.test.Lib1Class"),
        interpreted("com.lib1.Lib1Class"),
        interpreted("Lib1Class")
      )
      file.declarations shouldBe setOf(
        agnostic("com.test.ParsedClass")
      )
    }

    @Test
    fun `public method with fully qualified return type should count as api reference`() {

      val file = createFile(
        """
        package com.test;

        public class ParsedClass {

          public com.lib1.Lib1Class foo() { return Lib1Class(); }
        }
        """
      )

      file.apiReferences shouldBe listOf(
        interpreted("com.lib1.Lib1Class"),
        interpreted("com.test.com.lib1.Lib1Class")
      )
      file.references() shouldBe listOf(
        interpreted("com.lib1.Lib1Class"),
        interpreted("com.test.com.lib1.Lib1Class")
      )
      file.declarations shouldBe setOf(
        agnostic("com.test.ParsedClass")
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
      file.references() shouldBe listOf(
        explicit("com.lib1.Lib1Class"),
        explicit("java.util.List")
      )
      file.declarations shouldBe setOf(
        agnostic("com.test.ParsedClass")
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
      file.references() shouldBe listOf(
        explicit("java.util.List")
      )
      file.declarations shouldBe setOf(
        agnostic("com.test.ParsedClass")
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
      file.references() shouldBe listOf(
        explicit("com.lib1.Lib1Class"),
        explicit("java.util.List")
      )
      file.declarations shouldBe setOf(
        agnostic("com.test.ParsedClass")
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
      file.references() shouldBe listOf(
        explicit("java.util.List")
      )
      file.declarations shouldBe setOf(
        agnostic("com.test.ParsedClass")
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
      file.references() shouldBe listOf(
        explicit("java.util.List")
      )
      file.declarations shouldBe setOf(
        agnostic("com.test.ParsedClass")
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
        file.references() shouldBe listOf(
          interpreted("Lib1Class"),
          interpreted("com.lib1.Lib1Class"),
          interpreted("com.test.Lib1Class")
        )
        file.declarations shouldBe setOf(
          agnostic("com.test.ParsedClass")
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
        file.references() shouldBe listOf(
          explicit("com.lib1.Lib1Class")
        )
        file.declarations shouldBe setOf(
          agnostic("com.test.ParsedClass")
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
        file.references() shouldBe listOf(
          interpreted("Lib1Class"),
          explicit("java.util.List"),
          interpreted("com.lib1.Lib1Class"),
          interpreted("com.test.Lib1Class")
        )
        file.declarations shouldBe setOf(
          agnostic("com.test.ParsedClass")
        )
      }
  }

  fun kotlin(name: String) = name.kotlinExtension()

  fun java(name: String) = name.javaExtension()

  fun agnostic(name: String) = name.neutralExtension()

  fun explicit(name: String) = name.asExplicitJavaReference()
  fun interpreted(name: String) = name.asInterpretedJavaReference()
  fun unqualifiedAndroidResource(name: String) = UnqualifiedAndroidResourceReference(name)

  fun test(action: suspend CoroutineScope.() -> Unit) = runBlocking(block = action)

  infix fun LazyDeferred<Set<Reference>>.shouldBe(other: Collection<Reference>) {
    runBlocking {
      await()
        .distinct()
        .prettyPrint() kotestShouldBe other.prettyPrint()
    }
  }

  infix fun List<LazySet.DataSource<Reference>>.shouldBe(other: Collection<Reference>) {
    runBlocking {
      flatMap { it.get() }
        .distinct()
        .prettyPrint() kotestShouldBe other.prettyPrint()
    }
  }

  infix fun LazySet<Reference>.shouldBe(other: Collection<Reference>) {
    runBlocking {
      toList()
        .distinct()
        .prettyPrint() shouldBe other.prettyPrint()
    }
  }

  @JvmName("prettyPrintReferences")
  fun Collection<Reference>.prettyPrint() = groupBy { it::class }
    .toList()
    .sortedBy { it.first.qualifiedName }
    .joinToString("\n") { (_, names) ->
      val name = when (names.first()) {
        is ExplicitJavaReference -> "explicit"
        is ExplicitKotlinReference -> "explicitKotlin"
        is ExplicitXmlReference -> "explicitXml"
        is InterpretedJavaReference -> "interpreted"
        is InterpretedKotlinReference -> "interpretedKotlin"
        is UnqualifiedAndroidResourceReference -> "unqualifiedAndroidResource"
        is AndroidRReference -> "androidR"
        is QualifiedAndroidResourceReference -> "qualifiedAndroidResource"
      }
      names
        .sortedBy { it.name }
        .joinToString("\n", "$name {\n", "\n}") { "\t${it.name}" }
    }

  fun Collection<DeclaredName>.prettyPrint() = groupBy { it::class }
    .toList()
    .sortedBy { it.first.qualifiedName }
    .joinToString("\n") { (_, names) ->
      val name = when (val declaration = names.first()) {
        is AgnosticDeclaredName -> "agnostic"
        is AndroidRDeclaredName -> "androidR"
        is JavaSpecificDeclaredName -> "java"
        is KotlinSpecificDeclaredName -> "kotlin"
        is AndroidResourceDeclaredName -> declaration.prefix
      }
      names
        .sortedBy { it.name }
        .joinToString("\n", "$name {\n", "\n}") { "\t${it.name}" }
    }

  infix fun Collection<DeclaredName>.shouldBe(other: Collection<DeclaredName>) {
    prettyPrint() kotestShouldBe other.prettyPrint()
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
