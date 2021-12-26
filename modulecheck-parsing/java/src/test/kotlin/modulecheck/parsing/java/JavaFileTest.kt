/*
 * Copyright (C) 2021 Rick Busarow
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
import modulecheck.parsing.source.DeclarationName
import modulecheck.project.test.ProjectTest
import org.intellij.lang.annotations.Language
import org.junit.jupiter.api.Test
import java.io.File

internal class JavaFileTest : ProjectTest() {

  @Test
  fun `enum constants should count as declarations`() {

    val javaFile = file(
      """
    package com.example;

    public enum Color { RED, BLUE }
      """
    )

    javaFile.declarations shouldBe listOf(
      DeclarationName("com.example.Color"),
      DeclarationName("com.example.Color.RED"),
      DeclarationName("com.example.Color.BLUE")
    )
  }

  @Test
  fun `nested enum constants should count as declarations`() {

    val javaFile = file(
      """
    package com.example;

    public class Constants {
      public enum Color { RED, BLUE }
    }
      """
    )

    javaFile.declarations shouldBe listOf(
      DeclarationName("com.example.Constants"),
      DeclarationName("com.example.Constants.Color"),
      DeclarationName("com.example.Constants.Color.RED"),
      DeclarationName("com.example.Constants.Color.BLUE")
    )
  }

  @Test
  fun `declared constants should count as declarations`() {

    val javaFile = file(
      """
    package com.example;

    public class Constants {

      public static final int MY_VALUE = 250;
    }
      """
    )

    javaFile.declarations shouldBe listOf(
      DeclarationName("com.example.Constants"),
      DeclarationName("com.example.Constants.MY_VALUE")
    )
  }

  @Test
  fun `declared nested constants should count as declarations`() {

    val javaFile = file(
      """
    package com.example;

    public class Constants {

      public static class Values {

        public static final int MY_VALUE = 250;
      }
    }
      """
    )

    javaFile.declarations shouldBe listOf(
      DeclarationName("com.example.Constants"),
      DeclarationName("com.example.Constants.Values"),
      DeclarationName("com.example.Constants.Values.MY_VALUE")
    )
  }

  @Test
  fun `public static functions should count as declarations`() {

    val javaFile = file(
      """
    package com.example;

    public class Utils {

      public static void foo() {}
    }
      """
    )

    javaFile.declarations shouldBe listOf(
      DeclarationName("com.example.Utils"),
      DeclarationName("com.example.Utils.foo")
    )
  }

  @Test
  fun `public member property type with wildcard import should count as reference`() = runBlocking {

    val javaFile = file(
      """
    package com.example;

    import com.lib1.*;

    public class Utils {

      public Lib1Class lib1Class;
    }
      """
    )

    javaFile.declarations shouldBe listOf(
      DeclarationName("com.example.Utils")
    )
    javaFile.imports shouldBe listOf()
    javaFile.maybeExtraReferences.await() shouldBe listOf(
      "Lib1Class",
      "com.lib1.Lib1Class",
      "com.example.Lib1Class"
    )
  }

  @Test
  fun `public member property generic type with wildcard import should count as reference`() =
    runBlocking {

      val javaFile = file(
        """
    package com.example;

    import com.lib1.*;
    import java.util.List;

    public class Utils {

      public List<Lib1Class> lib1Classes;
    }
      """
      )

      javaFile.declarations shouldBe listOf(
        DeclarationName("com.example.Utils")
      )
      javaFile.imports shouldBe listOf("java.util.List")
      javaFile.maybeExtraReferences.await() shouldBe listOf(
        "Lib1Class",
        "com.lib1.Lib1Class",
        "com.example.Lib1Class"
      )
    }

  fun simpleProject() = project(":lib") {
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
    content: String
  ): RealJavaFile {
    testProjectDir.mkdirs()

    val javaFile = File(testProjectDir, "javaFile.java")
      .also { it.writeText(content.trimIndent()) }

    return RealJavaFile(javaFile)
  }
}
