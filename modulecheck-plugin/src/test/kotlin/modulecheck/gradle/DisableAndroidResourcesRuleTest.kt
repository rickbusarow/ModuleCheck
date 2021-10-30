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

package modulecheck.gradle

import modulecheck.specs.ProjectBuildSpec
import modulecheck.specs.ProjectSettingsSpec
import modulecheck.specs.ProjectSpec
import modulecheck.specs.ProjectSrcSpec
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import java.io.File
import java.nio.file.Path

class DisableAndroidResourcesRuleTest : BasePluginTest() {

  val project by resets {
    ProjectSpec("project") {
      addSettingsSpec(
        ProjectSettingsSpec {
          addInclude("app")
        }
      )
      addBuildSpec(
        ProjectBuildSpec {
          addPlugin("id(\"com.rickbusarow.module-check\")")
          buildScript()
          addBlock(
            """moduleCheck {
              |  checks.disableAndroidResources = true
              |}
              """.trimMargin()
          )
        }
      )
      addSubproject(
        ProjectSpec("app") {
          addBuildSpec(
            ProjectBuildSpec {
              addPlugin("""id("com.android.library")""")
              android = true
            }
          )
        }
      )
    }
  }

  @Nested
  inner class `resource generation is used` {

    @BeforeEach
    fun beforeEach() {
      project.edit {
        subprojects.first().edit {
          addSrcSpec(
            ProjectSrcSpec(Path.of("src/main/res/values")) {
              addRawFile(
                "strings.xml",
                """<resources>
                |  <string name="app_name" translatable="false">MyApp</string>
                |</resources>
                """.trimMargin()
              )
            }
          )
        }
      }
    }

    @Test
    fun `default enabled should not be changed`() {
      // this can't be auto-corrected since the property isn't there to change.
      // Technically we could just add an Android block or seek out an existing one,
      // so this might change

      project.writeIn(testProjectDir.toPath())

      shouldSucceed("moduleCheckDisableAndroidResources")
      File(testProjectDir, "/app/build.gradle.kts").readText() shouldBe """plugins {
        |  id("com.android.library")
        |}
        |
        |android {
        |  compileSdkVersion(30)
        |
        |  defaultConfig {
        |    minSdkVersion(23)
        |    targetSdkVersion(30)
        |  }
        |
        |  buildTypes {
        |    getByName("release") {
        |      proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
        |    }
        |  }
        |}
        |
        |""".trimMargin()
    }

    @Test
    fun `scoped and then dot qualified should not be changed`() {
      project.edit {
        subprojects.first().edit {
          projectBuildSpec!!.edit {
            addBlock(
              """android {
              |  buildFeatures.androidResources = true
              |}
              """.trimMargin()
            )
          }
        }
      }.writeIn(testProjectDir.toPath())

      shouldSucceed("moduleCheckDisableAndroidResources")
      File(testProjectDir, "/app/build.gradle.kts").readText() shouldBe """plugins {
        |  id("com.android.library")
        |}
        |
        |android {
        |  compileSdkVersion(30)
        |
        |  defaultConfig {
        |    minSdkVersion(23)
        |    targetSdkVersion(30)
        |  }
        |
        |  buildTypes {
        |    getByName("release") {
        |      proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
        |    }
        |  }
        |}
        |
        |android {
        |  buildFeatures.androidResources = true
        |}
        |
        |""".trimMargin()
    }

    @Test
    fun `fully dot qualified should not be changed`() {
      project.edit {
        subprojects.first().edit {
          projectBuildSpec!!.edit {
            addBlock(
              """android.buildFeatures.androidResources = true
              """.trimMargin()
            )
          }
        }
      }.writeIn(testProjectDir.toPath())

      shouldSucceed("moduleCheckDisableAndroidResources")
      File(testProjectDir, "/app/build.gradle.kts").readText() shouldBe """plugins {
        |  id("com.android.library")
        |}
        |
        |android {
        |  compileSdkVersion(30)
        |
        |  defaultConfig {
        |    minSdkVersion(23)
        |    targetSdkVersion(30)
        |  }
        |
        |  buildTypes {
        |    getByName("release") {
        |      proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
        |    }
        |  }
        |}
        |
        |android.buildFeatures.androidResources = true
        |
        |""".trimMargin()
    }

    @Test
    fun `fully scoped should not be changed`() {
      project.edit {
        subprojects.first().edit {
          projectBuildSpec!!.edit {
            addBlock(
              """android {
                  |  buildFeatures {
                  |    androidResources = true
                  |  }
                  |}
              """.trimMargin()
            )
          }
        }
      }.writeIn(testProjectDir.toPath())

      shouldSucceed("moduleCheckDisableAndroidResources")
      File(testProjectDir, "/app/build.gradle.kts").readText() shouldBe """plugins {
        |  id("com.android.library")
        |}
        |
        |android {
        |  compileSdkVersion(30)
        |
        |  defaultConfig {
        |    minSdkVersion(23)
        |    targetSdkVersion(30)
        |  }
        |
        |  buildTypes {
        |    getByName("release") {
        |      proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
        |    }
        |  }
        |}
        |
        |android {
        |  buildFeatures {
        |    androidResources = true
        |  }
        |}
        |
        |""".trimMargin()
    }

    @Test
    fun `dot qualified and then scoped should not be changed`() {
      project.edit {
        subprojects.first().edit {
          projectBuildSpec!!.edit {
            addBlock(
              """android.buildFeatures {
                  |  androidResources = true
                  |}
              """.trimMargin()
            )
          }
        }
      }.writeIn(testProjectDir.toPath())

      shouldSucceed("moduleCheckDisableAndroidResources")
      File(testProjectDir, "/app/build.gradle.kts").readText() shouldBe """plugins {
        |  id("com.android.library")
        |}
        |
        |android {
        |  compileSdkVersion(30)
        |
        |  defaultConfig {
        |    minSdkVersion(23)
        |    targetSdkVersion(30)
        |  }
        |
        |  buildTypes {
        |    getByName("release") {
        |      proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
        |    }
        |  }
        |}
        |
        |android.buildFeatures {
        |  androidResources = true
        |}
        |
        |""".trimMargin()
    }
  }

  @Nested
  inner class `resource generation is unused` {

    @Nested
    inner class `auto-correct enabled` {

      @BeforeEach
      fun beforeEach() {
        project.edit {
          projectBuildSpec!!.edit {
            addBlock(
              """moduleCheck {
              |  autoCorrect = true
              |}
              """.trimMargin()
            )
          }
        }
      }

      @Test
      fun `default value of enabled enabled should fail`() {
        // this can't be auto-corrected since the property isn't there to change.
        // Technically we could just add an Android block or seek out an existing one,
        // so this might change

        project.writeIn(testProjectDir.toPath())

        shouldFail("moduleCheckDisableAndroidResources") withTrimmedMessage """:app
           dependency    name                       source    build file
        X                disableAndroidResources              /app/build.gradle.kts:

ModuleCheck found 1 issue"""
      }

      @Test
      fun `scoped and then dot qualified should be fixed`() {
        project.edit {
          subprojects.first().edit {
            projectBuildSpec!!.edit {
              addBlock(
                """android {
              |  buildFeatures.androidResources = true
              |}
              """.trimMargin()
              )
            }
          }
        }.writeIn(testProjectDir.toPath())

        shouldSucceed("moduleCheckDisableAndroidResources") withTrimmedMessage """:app
           dependency    name                       source    build file
        ✔                disableAndroidResources              /app/build.gradle.kts: (21, 3):

ModuleCheck found 1 issue"""

        File(testProjectDir, "/app/build.gradle.kts").readText() shouldBe """plugins {
        |  id("com.android.library")
        |}
        |
        |android {
        |  compileSdkVersion(30)
        |
        |  defaultConfig {
        |    minSdkVersion(23)
        |    targetSdkVersion(30)
        |  }
        |
        |  buildTypes {
        |    getByName("release") {
        |      proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
        |    }
        |  }
        |}
        |
        |android {
        |  buildFeatures.androidResources = false
        |}
        |
        |""".trimMargin()
      }

      @Test
      fun `fully dot qualified should be fixed`() {
        project.edit {
          subprojects.first().edit {
            projectBuildSpec!!.edit {
              addBlock(
                """android.buildFeatures.androidResources = true
              """.trimMargin()
              )
            }
          }
        }.writeIn(testProjectDir.toPath())

        shouldSucceed("moduleCheckDisableAndroidResources") withTrimmedMessage """:app
           dependency    name                       source    build file
        ✔                disableAndroidResources              /app/build.gradle.kts: (20, 1):

ModuleCheck found 1 issue"""

        File(testProjectDir, "/app/build.gradle.kts").readText() shouldBe """plugins {
        |  id("com.android.library")
        |}
        |
        |android {
        |  compileSdkVersion(30)
        |
        |  defaultConfig {
        |    minSdkVersion(23)
        |    targetSdkVersion(30)
        |  }
        |
        |  buildTypes {
        |    getByName("release") {
        |      proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
        |    }
        |  }
        |}
        |
        |android.buildFeatures.androidResources = false
        |
        |""".trimMargin()
      }

      @Test
      fun `fully scoped should be fixed`() {
        project.edit {
          subprojects.first().edit {
            projectBuildSpec!!.edit {
              addBlock(
                """android {
                  |  buildFeatures {
                  |    androidResources = true
                  |  }
                  |}
              """.trimMargin()
              )
            }
          }
        }.writeIn(testProjectDir.toPath())

        shouldSucceed("moduleCheckDisableAndroidResources") withTrimmedMessage """:app
           dependency    name                       source    build file
        ✔                disableAndroidResources              /app/build.gradle.kts: (22, 5):

ModuleCheck found 1 issue"""

        File(testProjectDir, "/app/build.gradle.kts").readText() shouldBe """plugins {
        |  id("com.android.library")
        |}
        |
        |android {
        |  compileSdkVersion(30)
        |
        |  defaultConfig {
        |    minSdkVersion(23)
        |    targetSdkVersion(30)
        |  }
        |
        |  buildTypes {
        |    getByName("release") {
        |      proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
        |    }
        |  }
        |}
        |
        |android {
        |  buildFeatures {
        |    androidResources = false
        |  }
        |}
        |
        |""".trimMargin()
      }

      @Test
      fun `dot qualified and then scoped should be fixed`() {
        project.edit {
          subprojects.first().edit {
            projectBuildSpec!!.edit {
              addBlock(
                """android.buildFeatures {
                  |  androidResources = true
                  |}
              """.trimMargin()
              )
            }
          }
        }.writeIn(testProjectDir.toPath())

        shouldSucceed("moduleCheckDisableAndroidResources") withTrimmedMessage """:app
           dependency    name                       source    build file
        ✔                disableAndroidResources              /app/build.gradle.kts: (21, 3):

ModuleCheck found 1 issue"""

        File(testProjectDir, "/app/build.gradle.kts").readText() shouldBe """plugins {
        |  id("com.android.library")
        |}
        |
        |android {
        |  compileSdkVersion(30)
        |
        |  defaultConfig {
        |    minSdkVersion(23)
        |    targetSdkVersion(30)
        |  }
        |
        |  buildTypes {
        |    getByName("release") {
        |      proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
        |    }
        |  }
        |}
        |
        |android.buildFeatures {
        |  androidResources = false
        |}
        |
        |""".trimMargin()
      }
    }

    @Nested
    inner class `auto-correct disabled` {

      @BeforeEach
      fun beforeEach() {
        project.edit {
          projectBuildSpec!!.edit {
            addBlock(
              """moduleCheck {
              |  autoCorrect = false
              |}
              """.trimMargin()
            )
          }
        }
      }

      @Test
      fun `scoped and then dot qualified should fail`() {
        project.edit {
          subprojects.first().edit {
            projectBuildSpec!!.edit {
              addBlock(
                """android {
              |  buildFeatures.androidResources = true
              |}
              """.trimMargin()
              )
            }
          }
        }.writeIn(testProjectDir.toPath())

        shouldFail("moduleCheckDisableAndroidResources") withTrimmedMessage """:app
           dependency    name                       source    build file
        X                disableAndroidResources              /app/build.gradle.kts: (21, 3):

ModuleCheck found 1 issue"""
      }

      @Test
      fun `fully dot qualified should be fixed`() {
        project.edit {
          subprojects.first().edit {
            projectBuildSpec!!.edit {
              addBlock(
                """android.buildFeatures.androidResources = true
              """.trimMargin()
              )
            }
          }
        }.writeIn(testProjectDir.toPath())

        shouldFail("moduleCheckDisableAndroidResources") withTrimmedMessage """:app
           dependency    name                       source    build file
        X                disableAndroidResources              /app/build.gradle.kts: (20, 1):

ModuleCheck found 1 issue"""
      }

      @Test
      fun `fully scoped should not be fixed`() {
        project.edit {
          subprojects.first().edit {
            projectBuildSpec!!.edit {
              addBlock(
                """android {
                  |  buildFeatures {
                  |    androidResources = true
                  |  }
                  |}
              """.trimMargin()
              )
            }
          }
        }.writeIn(testProjectDir.toPath())

        shouldFail("moduleCheckDisableAndroidResources") withTrimmedMessage """:app
           dependency    name                       source    build file
        X                disableAndroidResources              /app/build.gradle.kts: (22, 5):

ModuleCheck found 1 issue"""
      }

      @Test
      fun `dot qualified and then scoped should be fixed`() {
        project.edit {
          subprojects.first().edit {
            projectBuildSpec!!.edit {
              addBlock(
                """android.buildFeatures {
                  |  androidResources = true
                  |}
              """.trimMargin()
              )
            }
          }
        }.writeIn(testProjectDir.toPath())

        shouldFail("moduleCheckDisableAndroidResources") withTrimmedMessage """:app
           dependency    name                       source    build file
        X                disableAndroidResources              /app/build.gradle.kts: (21, 3):

ModuleCheck found 1 issue"""
      }
    }
  }
}
