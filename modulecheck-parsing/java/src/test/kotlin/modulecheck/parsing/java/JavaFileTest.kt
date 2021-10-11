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

import modulecheck.parsing.DeclarationName
import modulecheck.testing.BaseTest
import org.junit.jupiter.api.Test
import java.io.File

internal class JavaFileTest : BaseTest() {

  @Test
  fun `enum constants should count as declarations`() {

    val javaFile = file(
      """
    package com.example;

    public enum Color { RED, BLUE }
      """.trimIndent()
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
      """.trimIndent()
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
      """.trimIndent()
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
      """.trimIndent()
    )

    javaFile.declarations shouldBe listOf(
      DeclarationName("com.example.Constants"),
      DeclarationName("com.example.Constants.Values"),
      DeclarationName("com.example.Constants.Values.MY_VALUE")
    )
  }

  fun file(content: String): JavaFile {
    testProjectDir.mkdirs()

    val javaFile = File(testProjectDir, "javaFile.java")
      .also { it.writeText(content) }

    return JavaFile(javaFile)
  }
}
