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

import kotlinx.coroutines.runBlocking
import modulecheck.parsing.gradle.SourceSetName
import modulecheck.parsing.source.JavaVersion
import modulecheck.parsing.source.JavaVersion.VERSION_14
import modulecheck.parsing.source.Reference
import modulecheck.project.McProject
import modulecheck.project.test.ProjectTest
import org.intellij.lang.annotations.Language
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import java.io.File

internal class JavaFileTest :
  ProjectTest(),
  JavaFileTestUtils by RealJavaFileTestUtils() {

  @Nested
  inner class `resolvable declarations` {

    @Test
    fun `enum constants should count as declarations`() {

      val file = file(
        """
    package com.test;

    public enum Color { RED, BLUE }
      """
      )

      file shouldBe javaFile(
        declarations = setOf(
          "com.test.Color",
          "com.test.Color.RED",
          "com.test.Color.BLUE"
        )
      )
    }

    @Test
    fun `nested enum constants should count as declarations`() {

      val file = file(
        """
    package com.test;

    public class Constants {
      public enum Color { RED, BLUE }
    }
      """
      )

      file shouldBe javaFile(
        declarations = setOf(
          "com.test.Constants",
          "com.test.Constants.Color",
          "com.test.Constants.Color.RED",
          "com.test.Constants.Color.BLUE"
        )
      )
    }

    @Test
    fun `declared constants should count as declarations`() {

      val file = file(
        """
    package com.test;

    public class Constants {

      public static final int MY_VALUE = 250;
    }
      """
      )

      file shouldBe javaFile(
        declarations = setOf(
          "com.test.Constants",
          "com.test.Constants",
          "com.test.Constants.MY_VALUE"
        )
      )
    }

    @Test
    fun `declared nested constants should count as declarations`() {

      val file = file(
        """
    package com.test;

    public class Constants {

      public static class Values {

        public static final int MY_VALUE = 250;
      }
    }
      """
      )

      file shouldBe javaFile(
        declarations = setOf(
          "com.test.Constants",
          "com.test.Constants.Values",
          "com.test.Constants.Values.MY_VALUE"
        )
      )
    }

    @Test
    fun `public static methods should count as declarations`() {

      val file = file(
        """
    package com.test;

    public class ParsedClass {

      public static void foo() {}
    }
      """
      )

      file shouldBe javaFile(
        declarations = setOf(
          "com.test.ParsedClass", "com.test.ParsedClass.foo"
        )
      )
    }

    @Test
    fun `a record should count as a declaration`() {

      val file = file(
        """
    package com.test;

    import com.lib1.Lib1Class;

    public static record MyRecord(Lib1Class lib1Class) {}
      """,
        javaVersion = JavaVersion.VERSION_16
      )

      file shouldBe javaFile(
        imports = setOf("com.lib1.Lib1Class"),
        declarations = setOf("com.test.MyRecord")
      )
    }
  }

  @Nested
  inner class `api references` {

    @Test
    fun `public method return type should count as api reference`() {

      val file = file(
        """
    package com.test;

    import com.lib1.Lib1Class;

    public class ParsedClass {

      public Lib1Class foo() { return Lib1Class(); }
    }
      """
      )

      file shouldBe javaFile(
        imports = setOf("com.lib1.Lib1Class"),
        declarations = setOf("com.test.ParsedClass"),
        apiReferences = setOf("com.lib1.Lib1Class")
      )
    }

    @Test
    fun `private method return type should not count as api reference`() {

      val file = file(
        """
    package com.test;

    import com.lib1.Lib1Class;

    public class ParsedClass {

      private Lib1Class foo() { return Lib1Class(); }
    }
      """
      )

      file shouldBe javaFile(
        imports = setOf("com.lib1.Lib1Class"),
        declarations = setOf("com.test.ParsedClass")
      )
    }

    @Test
    fun `package-private method return type should not count as api reference`() {

      val file = file(
        """
    package com.test;

    import com.lib1.Lib1Class;

    public class ParsedClass {

      Lib1Class foo() { return Lib1Class(); }
    }
      """
      )

      file shouldBe javaFile(
        imports = setOf("com.lib1.Lib1Class"),
        declarations = setOf("com.test.ParsedClass")
      )
    }

    @Test
    fun `public method with wildcard-imported return type should count as api reference`() {

      val file = file(
        """
    package com.test;

    import com.lib1.*;

    public class ParsedClass {

      public Lib1Class foo() { return Lib1Class(); }
    }
      """
      )

      file shouldBe javaFile(
        wildcardImports = setOf("com.lib1"),
        declarations = setOf("com.test.ParsedClass"),
        interpretedReferences = setOf(
          Reference.InterpretedReference(
            setOf("Lib1Class", "com.lib1.Lib1Class", "com.test.Lib1Class")
          )
        ),
        apiReferences = setOf("Lib1Class", "com.lib1.Lib1Class", "com.test.Lib1Class")
      )
    }

    @Test
    fun `public method with fully qualified return type should count as api reference`() {

      val file = file(
        """
    package com.test;

    public class ParsedClass {

      public com.lib1.Lib1Class foo() { return Lib1Class(); }
    }
      """
      )

      file shouldBe javaFile(
        declarations = setOf("com.test.ParsedClass"),
        apiReferences = setOf("com.lib1.Lib1Class", "com.test.com.lib1.Lib1Class"),
        interpretedReferences = setOf(
          Reference.InterpretedReference(
            setOf("com.lib1.Lib1Class", "com.test.com.lib1.Lib1Class")
          )
        )
      )
    }

    @Test
    fun `public method parameterized return type should count as api reference`() {

      val file = file(
        """
    package com.test;

    import com.lib1.Lib1Class;
    import java.util.List;

    public class ParsedClass {

      public List<Lib1Class> foo() { return null; }
    }
      """
      )

      file shouldBe javaFile(
        imports = setOf("com.lib1.Lib1Class", "java.util.List"),
        declarations = setOf("com.test.ParsedClass"),
        apiReferences = setOf("com.lib1.Lib1Class", "java.util.List")
      )
    }

    @Test
    fun `public method generic return type parameter should not count as api reference`() {

      val file = file(
        """
    package com.test;

    import java.util.List;

    public class ParsedClass {

      public <E> List<E> foo() { return null; }
    }
      """
      )

      file shouldBe javaFile(
        imports = setOf("java.util.List"),
        declarations = setOf("com.test.ParsedClass"),
        apiReferences = setOf("java.util.List")
      )
    }

    @Test
    fun `import should not be an api reference if it isn't actually part of an api reference`() {

      val file = file(
        """
    package com.test;

    import com.lib1.Lib1Class;
    import java.util.List;

    public class ParsedClass {

      public <E> List<E> foo() { return null; }
    }
      """
      )

      file shouldBe javaFile(
        imports = setOf("com.lib1.Lib1Class", "java.util.List"),
        declarations = setOf("com.test.ParsedClass"),
        apiReferences = setOf("java.util.List")
      )
    }

    @Test
    fun `public method generic return type parameter bound should count as api reference`() {

      val file = file(
        """
    package com.test;

    import java.util.List;

    public class ParsedClass {

      public <E extends CharSequence> List<E> foo() { return null; }
    }
      """
      )

      file shouldBe javaFile(
        imports = setOf("java.util.List"),
        declarations = setOf("com.test.ParsedClass"),
        apiReferences = setOf("java.lang.CharSequence", "java.util.List"),
        interpretedReferences = setOf()
      )
    }

    @Test
    fun `public method argument should count as api reference`() {

      val file = file(
        """
    package com.test;

    import java.util.List;

    public class ParsedClass {

      public <E> List<E> foo(String name) { return null; }
    }
      """
      )

      file shouldBe javaFile(
        imports = setOf("java.util.List"),
        declarations = setOf("com.test.ParsedClass"),
        apiReferences = setOf("java.util.List", "java.lang.String")
      )
    }

    @Test
    fun `public member property type with wildcard import should count as api reference`() =
      runBlocking {

        val file = file(
          """
    package com.test;

    import com.lib1.*;

    public class ParsedClass {

      public Lib1Class lib1Class;
    }
      """
        )

        file shouldBe javaFile(
          declarations = setOf("com.test.ParsedClass"),
          wildcardImports = setOf("com.lib1"),
          apiReferences = setOf(
            "Lib1Class",
            "com.lib1.Lib1Class",
            "com.test.Lib1Class"
          ),
          interpretedReferences = setOf(
            Reference.InterpretedReference(
              setOf(
                "Lib1Class",
                "com.lib1.Lib1Class",
                "com.test.Lib1Class"
              )
            )
          )
        )
      }

    @Test
    fun `public member property type with import should count as api reference`() =
      runBlocking {

        val file = file(
          """
    package com.test;

    import com.lib1.Lib1Class;

    public class ParsedClass {

      public Lib1Class lib1Class;
    }
      """
        )

        file shouldBe javaFile(
          imports = setOf("com.lib1.Lib1Class"),
          declarations = setOf("com.test.ParsedClass"),
          apiReferences = setOf("com.lib1.Lib1Class")
        )
      }

    @Test
    fun `a public member property with generic type with wildcard import should count as api reference`() =
      runBlocking {

        val file = file(
          """
    package com.test;

    import com.lib1.*;
    import java.util.List;

    public class ParsedClass {

      public List<Lib1Class> lib1Classes;
    }
      """
        )

        file shouldBe javaFile(
          imports = setOf("java.util.List"),
          declarations = setOf("com.test.ParsedClass"),
          wildcardImports = setOf("com.lib1"),
          apiReferences = setOf(
            "java.util.List",
            "com.lib1.Lib1Class",
            "Lib1Class",
            "com.test.Lib1Class"
          ),
          interpretedReferences = setOf(
            Reference.InterpretedReference(
              setOf(
                "Lib1Class",
                "com.lib1.Lib1Class",
                "com.test.Lib1Class"
              )
            )
          )
        )
      }
  }

  @Test
  fun `public static functions should count as declarations`() {

    val file = file(
      """
    package com.test;

    public class Utils {

      public static void foo() {}
    }
      """
    )

    file shouldBe javaFile(
      declarations = setOf(
        "com.test.Utils",
        "com.test.Utils.foo"
      )
    )
  }

  @Test
  fun `public member property type with wildcard import should count as reference`() = runBlocking {

    val file = file(
      """
    package com.test;

    import com.lib1.*;

    public class Utils {

      public Lib1Class lib1Class;
    }
      """
    )

    file shouldBe javaFile(
      declarations = setOf("com.test.Utils"),
      apiReferences = setOf("Lib1Class", "com.lib1.Lib1Class", "com.test.Lib1Class"),
      interpretedReferences = interpreted(
        "Lib1Class",
        "com.lib1.Lib1Class",
        "com.test.Lib1Class"
      )
    )
  }

  @Test
  fun `public member property generic type with wildcard import should count as reference`() =
    runBlocking {

      val file = file(
        """
    package com.test;

    import com.lib1.*;
    import java.util.List;

    public class Utils {

      public List<Lib1Class> lib1Classes;
    }
      """
      )

      file shouldBe javaFile(
        imports = setOf("java.util.List"),
        declarations = setOf("com.test.Utils"),
        apiReferences = setOf(
          "java.util.List",
          "Lib1Class",
          "com.lib1.Lib1Class",
          "com.test.Lib1Class"
        ),
        interpretedReferences = interpreted(
          "Lib1Class",
          "com.lib1.Lib1Class",
          "com.test.Lib1Class"
        )
      )
    }

  fun interpreted(vararg names: String) = setOf(
    Reference.InterpretedReference(names.toSet())
  )

  fun simpleProject(
    javaVersion: JavaVersion = VERSION_14
  ) = project(":lib") {

    javaSourceVersion = javaVersion

    addSource(
      "com/lib1/Lib1Class.kt",
      """
        package com.lib1

        class Lib1Class
      """,
      SourceSetName.MAIN
    )
  }

  fun file(
    @Language("java")
    content: String,
    sourceSetName: SourceSetName = SourceSetName.MAIN,
    javaVersion: JavaVersion = VERSION_14,
    project: McProject = simpleProject(javaVersion)
  ): RealJavaFile {
    testProjectDir.mkdirs()

    val file = File(testProjectDir, "JavaFile.java")
      .also { it.writeText(content.trimIndent()) }

    return RealJavaFile(
      file = file, javaVersion = javaVersion,
      nodeResolver = JavaParserNodeResolver(project, sourceSetName)
    )
  }
}
