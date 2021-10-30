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

import com.squareup.kotlinpoet.ClassName
import com.squareup.kotlinpoet.FileSpec
import com.squareup.kotlinpoet.PropertySpec
import modulecheck.specs.ProjectBuildSpec
import modulecheck.specs.ProjectSettingsSpec
import modulecheck.specs.ProjectSpec
import modulecheck.specs.ProjectSrcSpec
import modulecheck.specs.ProjectSrcSpecBuilder.RawFile
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import java.io.File
import java.nio.file.Path

class DisableAndroidViewBindingRuleTest : BasePluginTest() {

  val project by resets {
    ProjectSpec("project") {
      addSettingsSpec(
        ProjectSettingsSpec {
          addInclude("lib1")
        }
      )
      addBuildSpec(
        ProjectBuildSpec {
          addPlugin("id(\"com.rickbusarow.module-check\")")
          buildScript()
          addBlock(
            """moduleCheck {
              |  checks.disableViewBinding = true
              |}
              """.trimMargin()
          )
        }
      )
      addSubproject(
        ProjectSpec("lib1") {
          addBuildSpec(
            ProjectBuildSpec {
              addPlugin("""id("com.android.library")""")
              addPlugin("kotlin(\"android\")")
              android = true
            }
          )
        }
      )
    }
  }

  @Nested
  inner class `viewBinding generation is used in another module` {

    val bindingClassName = ClassName("com.example.lib1.databinding", "ActivityMainBinding")
    val bindingProperty = PropertySpec.builder("binding", bindingClassName).build()
    val myLib2File = FileSpec.builder("com.example.lib2", "MyLib2")
      .addProperty(bindingProperty)
      .build()
    val activity_main_xml = RawFile(
      "activity_main.xml",
      """<?xml version="1.0" encoding="utf-8"?>
                |<androidx.constraintlayout.widget.ConstraintLayout xmlns:android="http://schemas.android.com/apk/res/android"
                |  android:id="@+id/fragment_container"
                |  android:layout_width="match_parent"
                |  android:layout_height="match_parent" />
                |
                """.trimMargin()
    )

    @Test
    fun `scoped and then dot qualified in another module should not be changed`() {
      project.edit {
        projectSettingsSpec?.edit {
          addInclude("lib2")
        }
        subprojects.first().edit {
          projectBuildSpec!!.edit {
            addBlock(
              """android {
              |  buildFeatures.viewBinding = true
              |}
              """.trimMargin()
            )
          }
          addSrcSpec(
            ProjectSrcSpec(Path.of("src/main/res/layout")) {
              addRawFile(activity_main_xml)
            }
          )
          addSrcSpec(
            ProjectSrcSpec(Path.of("src/main")) {
              addRawFile(
                "AndroidManifest.xml",
                """<manifest package="com.example.lib1" />
                """.trimMargin()
              )
            }
          )
        }
        addSubproject(
          ProjectSpec("lib2") {
            addSrcSpec(
              ProjectSrcSpec(Path.of("src/main/java")) {
                addFileSpec(myLib2File)
              }
            )
            addBuildSpec(
              ProjectBuildSpec {
                addPlugin("""id("com.android.library")""")
                addPlugin("kotlin(\"android\")")
                android = true
                addProjectDependency("api", "lib1")
              }
            )
          }
        )
      }.writeIn(testProjectDir.toPath())

      shouldSucceed("moduleCheckDisableViewBinding")
      File(testProjectDir, "/lib1/build.gradle.kts").readText() shouldBe """plugins {
        |  id("com.android.library")
        |  kotlin("android")
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
        |  buildFeatures.viewBinding = true
        |}
        |
        |""".trimMargin()
    }
  }

  @Nested
  inner class `viewBinding generation is used` {

    val bindingClassName = ClassName("com.example.lib1.databinding", "ActivityMainBinding")
    val bindingProperty = PropertySpec.builder("binding", bindingClassName).build()
    val mylib1File = FileSpec.builder("com.example.lib1", "Mylib1")
      .addProperty(bindingProperty)
      .build()
    val activity_main_xml = RawFile(
      "activity_main.xml",
      """<?xml version="1.0" encoding="utf-8"?>
                |<androidx.constraintlayout.widget.ConstraintLayout xmlns:android="http://schemas.android.com/apk/res/android"
                |  android:id="@+id/fragment_container"
                |  android:layout_width="match_parent"
                |  android:layout_height="match_parent" />
                |
                """.trimMargin()
    )

    @BeforeEach
    fun beforeEach() {
      project.edit {
        subprojects.first().edit {
          addSrcSpec(
            ProjectSrcSpec(Path.of("src/main/java")) {
              addFileSpec(mylib1File)
            }
          )
          addSrcSpec(
            ProjectSrcSpec(Path.of("src/main/res/layout")) {
              addRawFile(activity_main_xml)
            }
          )
          addSrcSpec(
            ProjectSrcSpec(Path.of("src/main")) {
              addRawFile(
                "AndroidManifest.xml",
                """<manifest package="com.example.lib1" />
                """.trimMargin()
              )
            }
          )
        }
      }
    }

    @Test
    fun `scoped and then dot qualified should not be changed`() {
      project.edit {
        subprojects.first().edit {
          projectBuildSpec!!.edit {
            addBlock(
              """android {
              |  buildFeatures.viewBinding = true
              |}
              """.trimMargin()
            )
          }
        }
      }.writeIn(testProjectDir.toPath())

      shouldSucceed("moduleCheckDisableViewBinding")
      File(testProjectDir, "/lib1/build.gradle.kts").readText() shouldBe """plugins {
        |  id("com.android.library")
        |  kotlin("android")
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
        |  buildFeatures.viewBinding = true
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
              """android.buildFeatures.viewBinding = true
              """.trimMargin()
            )
          }
        }
      }.writeIn(testProjectDir.toPath())

      shouldSucceed("moduleCheckDisableViewBinding")
      File(testProjectDir, "/lib1/build.gradle.kts").readText() shouldBe """plugins {
        |  id("com.android.library")
        |  kotlin("android")
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
        |android.buildFeatures.viewBinding = true
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
                  |    viewBinding = true
                  |  }
                  |}
              """.trimMargin()
            )
          }
        }
      }.writeIn(testProjectDir.toPath())

      shouldSucceed("moduleCheckDisableViewBinding")
      File(testProjectDir, "/lib1/build.gradle.kts").readText() shouldBe """plugins {
        |  id("com.android.library")
        |  kotlin("android")
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
        |    viewBinding = true
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
                  |  viewBinding = true
                  |}
              """.trimMargin()
            )
          }
        }
      }.writeIn(testProjectDir.toPath())

      shouldSucceed("moduleCheckDisableViewBinding")
      File(testProjectDir, "/lib1/build.gradle.kts").readText() shouldBe """plugins {
        |  id("com.android.library")
        |  kotlin("android")
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
        |  viewBinding = true
        |}
        |
        |""".trimMargin()
    }
  }

  @Nested
  inner class `viewBinding generation is unused` {

    @Nested
    inner class `with auto-correct` {

      @Test
      fun `default value of disabled enabled should pass`() {
        // this can't be auto-corrected since the property isn't there to change.
        // Technically we could just add an Android block or seek out an existing one,
        // so this might change

        project.writeIn(testProjectDir.toPath())

        shouldSucceed("moduleCheckDisableViewBindingApply")
      }

      @Test
      fun `scoped and then dot qualified should be fixed`() {
        project.edit {
          subprojects.first().edit {
            projectBuildSpec!!.edit {
              addBlock(
                """android {
              |  buildFeatures.viewBinding = true
              |}
              """.trimMargin()
              )
            }
          }
        }.writeIn(testProjectDir.toPath())

        shouldSucceed("moduleCheckDisableViewBindingApply")
        File(testProjectDir, "/lib1/build.gradle.kts").readText() shouldBe """plugins {
        |  id("com.android.library")
        |  kotlin("android")
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
        |  buildFeatures.viewBinding = false
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
                """android.buildFeatures.viewBinding = true
              """.trimMargin()
              )
            }
          }
        }.writeIn(testProjectDir.toPath())

        shouldSucceed("moduleCheckDisableViewBindingApply")
        File(testProjectDir, "/lib1/build.gradle.kts").readText() shouldBe """plugins {
        |  id("com.android.library")
        |  kotlin("android")
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
        |android.buildFeatures.viewBinding = false
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
                  |    viewBinding = true
                  |  }
                  |}
              """.trimMargin()
              )
            }
          }
        }.writeIn(testProjectDir.toPath())

        shouldSucceed("moduleCheckDisableViewBindingApply")
        File(testProjectDir, "/lib1/build.gradle.kts").readText() shouldBe """plugins {
        |  id("com.android.library")
        |  kotlin("android")
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
        |    viewBinding = false
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
                  |  viewBinding = true
                  |}
              """.trimMargin()
              )
            }
          }
        }.writeIn(testProjectDir.toPath())

        shouldSucceed("moduleCheckDisableViewBindingApply")
        File(testProjectDir, "/lib1/build.gradle.kts").readText() shouldBe """plugins {
        |  id("com.android.library")
        |  kotlin("android")
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
        |  viewBinding = false
        |}
        |
        |""".trimMargin()
      }
    }

    @Nested
    inner class `no auto-correct` {

      @Test
      fun `scoped and then dot qualified should fail`() {
        project.edit {
          subprojects.first().edit {
            projectBuildSpec!!.edit {
              addBlock(
                """android {
              |  buildFeatures.viewBinding = true
              |}
              """.trimMargin()
              )
            }
          }
        }.writeIn(testProjectDir.toPath())

        shouldFail("moduleCheckDisableViewBinding") withTrimmedMessage """:lib1
           dependency    name                  source    build file
        X                disableViewBinding              /lib1/build.gradle.kts: (22, 3):

ModuleCheck found 1 issue"""
      }

      @Test
      fun `fully dot qualified should fail`() {
        project.edit {
          subprojects.first().edit {
            projectBuildSpec!!.edit {
              addBlock(
                """android.buildFeatures.viewBinding = true
              """.trimMargin()
              )
            }
          }
        }.writeIn(testProjectDir.toPath())

        shouldFail("moduleCheckDisableViewBinding") withTrimmedMessage """:lib1
           dependency    name                  source    build file
        X                disableViewBinding              /lib1/build.gradle.kts: (21, 1):

ModuleCheck found 1 issue"""
      }

      @Test
      fun `fully scoped should fail`() {
        project.edit {
          subprojects.first().edit {
            projectBuildSpec!!.edit {
              addBlock(
                """android {
                  |  buildFeatures {
                  |    viewBinding = true
                  |  }
                  |}
              """.trimMargin()
              )
            }
          }
        }.writeIn(testProjectDir.toPath())

        shouldFail("moduleCheckDisableViewBinding") withTrimmedMessage """:lib1
           dependency    name                  source    build file
        X                disableViewBinding              /lib1/build.gradle.kts: (23, 5):

ModuleCheck found 1 issue"""
      }

      @Test
      fun `dot qualified and then scoped should fail`() {
        project.edit {
          subprojects.first().edit {
            projectBuildSpec!!.edit {
              addBlock(
                """android.buildFeatures {
                  |  viewBinding = true
                  |}
              """.trimMargin()
              )
            }
          }
        }.writeIn(testProjectDir.toPath())

        shouldFail("moduleCheckDisableViewBinding") withTrimmedMessage """:lib1
           dependency    name                  source    build file
        X                disableViewBinding              /lib1/build.gradle.kts: (22, 3):

ModuleCheck found 1 issue"""
      }
    }
  }
}
