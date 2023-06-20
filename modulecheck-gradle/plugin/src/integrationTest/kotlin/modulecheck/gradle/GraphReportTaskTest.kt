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

import modulecheck.utils.resolve
import org.junit.jupiter.api.Test

internal class GraphReportTaskTest : BaseGradleTest() {

  @Test
  fun `graphs report should be created if graph task is invoked with default settings`() = test {

    kotlinProject(":lib1") {
      buildFile {
        """
          plugins {
            kotlin("jvm")
          }
          """
      }
    }

    kotlinProject(":lib2") {
      buildFile {
        """
          plugins {
            kotlin("jvm")
          }
          dependencies {
            implementation(project(":lib1"))
          }
          """
      }
    }

    val app = kotlinProject(":app") {
      buildFile {
        """
          plugins {
            kotlin("jvm")
          }
          dependencies {
            implementation(project(":lib1"))
            implementation(project(":lib2"))
          }
          """
      }
    }

    shouldSucceed("moduleCheckGraphs")

    app.projectDir.resolve(
      "build",
      "reports",
      "modulecheck",
      "graphs",
      "main.dot"
    ) shouldHaveText """
      strict digraph {
        edge ["dir"="forward"]
        graph ["ratio"="0.5625","rankdir"="TB","label"=<<b>:app -- main</b>>,"labelloc"="t"]
        node ["style"="rounded,filled","shape"="box"]
        {
          edge ["dir"="none"]
          graph ["rank"="same"]
          ":lib1" ["fillcolor"="#F89820"]
        }
        {
          edge ["dir"="none"]
          graph ["rank"="same"]
          ":lib2" ["fillcolor"="#F89820"]
        }
        {
          edge ["dir"="none"]
          graph ["rank"="same"]
          ":app" ["fillcolor"="#F89820"]
        }
        ":app" -> ":lib2" ["arrowhead"="normal","style"="bold","color"="#FF6347"]
        ":app" -> ":lib1" ["arrowhead"="normal","style"="bold","color"="#FF6347"]
        ":lib2" -> ":lib1" ["arrowhead"="normal","style"="bold","color"="#FF6347"]
      }
    """
  }
}
