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

import org.junit.jupiter.api.Test

internal class SortingTaskTest : BaseGradleTest() {

  @Test
  fun `dependency sort task should execute even if disabled in multi rule settings`() {

    val lib1 = kotlinProject(":lib1") {
      buildFile {
        """
        plugins {
          kotlin("jvm")
        }

        dependencies {
          implementation("javax.inject:javax.inject:1")
          implementation("com.google.auto:auto-common:1.2.1")
        }
        """
      }
    }

    rootBuild {
      """
      buildscript {
        dependencies {
          classpath("org.jetbrains.kotlin:kotlin-gradle-plugin:$kotlinVersion")
        }
      }

      plugins {
        id("com.rickbusarow.module-check")
      }

      moduleCheck {
        checks {
          sortDependencies = false
        }
      }
      """
    }

    shouldSucceed("moduleCheckSortDependenciesAuto")

    lib1.buildFile shouldHaveText """
      plugins {
        kotlin("jvm")
      }

      dependencies {
        implementation("com.google.auto:auto-common:1.2.1")

        implementation("javax.inject:javax.inject:1")
      }
      """
  }

  @Test
  fun `plugin sort task should execute even if disabled in multi rule settings`() {

    val lib1 = kotlinProject(":lib1") {
      buildFile {
        """
        plugins {
          base
          kotlin("jvm")
        }
        """
      }
    }

    rootBuild {
      """
      buildscript {
        dependencies {
          classpath("org.jetbrains.kotlin:kotlin-gradle-plugin:$kotlinVersion")
        }
      }

      plugins {
        id("com.rickbusarow.module-check")
      }

      moduleCheck {
        checks {
          sortPlugins = false
        }
      }
      """
    }

    shouldSucceed("moduleCheckSortPluginsAuto")

    lib1.buildFile shouldHaveText """
      plugins {
        kotlin("jvm")
        base
      }
      """
  }
}
