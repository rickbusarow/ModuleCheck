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

package modulecheck.gradle

import modulecheck.testing.writeGroovy
import modulecheck.testing.writeKotlin
import modulecheck.utils.child
import modulecheck.utils.createSafely
import org.junit.jupiter.api.Test
import kotlin.reflect.full.memberProperties

class ConfigValidationTest : BasePluginTest() {

  @Test
  fun `all properties`() {

    ModuleCheckExtension::class.memberProperties
      .map { it.name } shouldBe listOf(
      "additionalKaptMatchers",
      "checks",
      "deleteUnused",
      "doNotCheck",
      "ignoreUnusedFinding",
      "reports",
      "sort"
    )

    ChecksExtension::class.memberProperties
      .map { it.name } shouldBe listOf(
      "anvilFactoryGeneration",
      "depths",
      "disableAndroidResources",
      "disableViewBinding",
      "inheritedDependency",
      "mustBeApi",
      "overShotDependency",
      "redundantDependency",
      "sortDependencies",
      "sortPlugins",
      "unusedDependency",
      "unusedKapt",
      "unusedKotlinAndroidExtensions"
    )

    SortExtension::class.memberProperties
      .map { it.name } shouldBe listOf("dependencyComparators", "pluginComparators")

    ReportsExtension::class.memberProperties
      .map { it.name } shouldBe listOf("checkstyle", "depths", "graphs", "sarif", "text")
  }

  @Test
  fun `Kotlin configuration`() {
    kotlinProject(":") {
      buildFile.writeKotlin(
        """
        plugins {
          id("com.rickbusarow.module-check")
        }

        moduleCheck {

          deleteUnused = true // default is false

          checks {
            overShotDependency = true  // default is true
            redundantDependency = false  // default is false
            unusedDependency = true  // default is true
            mustBeApi = true  // default is true
            inheritedDependency = true  // default is true
            sortDependencies = false  // default is false
            sortPlugins = false  // default is false
            unusedKapt = true  // default is true
            anvilFactoryGeneration = true  // default is true
            disableAndroidResources = false  // default is false
            disableViewBinding = false  // default is false
            unusedKotlinAndroidExtensions = false  // default is false
            depths = false  // default is false
          }

          // allow these modules to be declared as dependency anywhere,
          // regardless of whether they're used
          ignoreUnusedFinding = setOf(":test:core-jvm", ":test:core-android")

          // do not check the dependencies of these modules.
          // in this case, :app could declare any module it wants without issue
          doNotCheck = setOf(":app")

          additionalKaptMatchers = listOf(
            modulecheck.api.KaptMatcher(
              name = "MyProcessor",
              processor = "my-project.codegen:processor",
              annotationImports = listOf(
                "myproject\\.\\*",
                "myproject\\.MyInject",
                "myproject\\.MyInject\\.Factory",
                "myproject\\.MyInjectParam",
                "myproject\\.MyInjectModule"
              )
            )
          )

          reports {
            checkstyle {
              enabled = true  // default is false
              outputPath = "${'$'}{project.buildDir}/reports/modulecheck/checkstyle.xml"
            }
            sarif {
              enabled = true  // default is false
              outputPath = "${'$'}{project.buildDir}/reports/modulecheck/modulecheck.sarif"
            }
            depths {
              enabled = true  // default is false
              outputPath = "${'$'}{project.buildDir}/reports/modulecheck/depths.txt"
            }
            graphs {
              enabled = true  // default is false
              // The root directory of all generated graphs.  If set, directories will be created
              // for each module, mirroring the structure of the project.  If this property is null,
              // graphs will be created in the `build/reports/modulecheck/graphs/` relative
              // directory of each project.
              outputDir = "${'$'}{project.buildDir}/reports/modulecheck/graphs"
            }
            text {
              enabled = true  // default is false
              outputPath = "${'$'}{project.buildDir}/reports/modulecheck/report.txt"
            }
          }
        }
        """.trimIndent()
      )

      projectDir.child("settings.gradle.kts").createSafely(null)
    }

    shouldSucceed("moduleCheck")
  }

  @Test
  fun `Groovy configuration`() {
    kotlinProject(":") {
      buildFile.delete()
      buildFile = projectDir.child("build.gradle")
      buildFile.writeGroovy(
        """
        plugins {
          id 'com.rickbusarow.module-check'
        }

        moduleCheck {
          deleteUnused = true // default is false

          checks {
            overShotDependency = true  // default is true
            redundantDependency = false  // default is false
            unusedDependency = true  // default is true
            mustBeApi = true  // default is true
            inheritedDependency = true  // default is true
            sortDependencies = false  // default is false
            sortPlugins = false  // default is false
            unusedKapt = true  // default is true
            anvilFactoryGeneration = true  // default is true
            disableAndroidResources = false  // default is false
            disableViewBinding = false  // default is false
            unusedKotlinAndroidExtensions = false  // default is false
            depths = false  // default is false
          }

          // allow these modules to be declared as dependency anywhere,
          // regardless of whether they're used
          ignoreUnusedFinding = [':test:core-jvm', ':test:core-android']

          // do not check the dependencies of these modules.
          // in this case, :app could declare any module it wants without issue
          doNotCheck = [':app']

          additionalKaptMatchers = [
            new modulecheck.api.KaptMatcher(
              'MyProcessor',
              'my-project.codegen:processor',
               [
                "myproject\\.\\*",
                "myproject\\.MyInject",
                "myproject\\.MyInject\\.Factory",
                "myproject\\.MyInjectParam",
                "myproject\\.MyInjectModule"
              ]
            )
          ]

          reports {
            checkstyle {
              it.enabled = true  // default is false
              it.outputPath = "${'$'}{project.buildDir}/reports/modulecheck/checkstyle.xml"
            }
            sarif {
              it.enabled = true  // default is false
              it.outputPath = "${'$'}{project.buildDir}/reports/modulecheck/modulecheck.sarif"
            }
            depths {
              it.enabled = true  // default is false
              it.outputPath = "${'$'}{project.buildDir}/reports/modulecheck/depths.txt"
            }
            graphs {
              it.enabled = true  // default is false
              // The root directory of all generated graphs.  If set, directories will be created
              // for each module, mirroring the structure of the project.  If this property is null,
              // graphs will be created in the `build/reports/modulecheck/graphs/` relative
              // directory of each project.
              it.outputDir = "${'$'}{project.buildDir}/reports/modulecheck/graphs"
            }
            text {
              it.enabled = true  // default is false
              it.outputPath = "${'$'}{project.buildDir}/reports/modulecheck/report.txt"
            }
          }

        }
        """.trimIndent()
      )

      projectDir.child("settings.gradle").createSafely()
    }

    shouldSucceed("moduleCheck")
  }
}
