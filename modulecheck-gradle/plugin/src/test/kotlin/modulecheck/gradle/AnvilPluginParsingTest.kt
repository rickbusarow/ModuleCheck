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

package modulecheck.gradle

import modulecheck.testing.replaceOrFail
import org.junit.jupiter.api.TestFactory

class AnvilPluginParsingTest : BaseGradleTest() {

  @TestFactory
  fun `anvil may be added to the root project buildScript classpath`() = anvil {
    rootBuild {
      DEFAULT_BUILD_FILE.replaceOrFail(
        """(\s*buildscript\s*\{\s*dependencies\s*\{)(\s*)""".toRegex(),
        "$1$2classpath(\"com.squareup.anvil:gradle-plugin:$anvilVersion\")$2"
      )
    }

    kotlinProject(":lib1") {
      buildFile {
        """
        plugins {
          kotlin("jvm")
          id("com.squareup.anvil")
        }
        """
      }

      addKotlinSource(
        """
          package com.modulecheck.lib1

          class Lib1Class
          """,
        sourceDirName = "kotlin"
      )
    }

    shouldSucceed("moduleCheck")
  }

  @TestFactory
  fun `anvil may be added directly to a subproject only`() = anvil {

    kotlinProject(":lib1") {
      buildFile {
        """
        plugins {
          kotlin("jvm")
          id("com.squareup.anvil") version "$anvilVersion"
        }
        """
      }

      addKotlinSource(
        """
          package com.modulecheck.lib1

          class Lib1Class
          """,
        sourceDirName = "kotlin"
      )
    }

    shouldSucceed("moduleCheck")
  }
}
