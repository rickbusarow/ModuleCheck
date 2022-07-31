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
@file:Suppress("DEPRECATION")

package modulecheck.core

import modulecheck.api.KaptMatcher
import modulecheck.config.CodeGeneratorBinding
import modulecheck.model.dependency.ConfigurationName
import modulecheck.model.dependency.asConfigurationName
import modulecheck.model.sourceset.SourceSetName
import modulecheck.runtime.test.ProjectFindingReport.unusedKaptPlugin
import modulecheck.runtime.test.RunnerTest
import org.junit.jupiter.api.Test

class UnusedKaptPluginTest : RunnerTest() {

  val dagger = "com.google.dagger:dagger-compiler:2.40.5"

  @Test
  fun `plugin applied but with processor in non-kapt configuration without autoCorrect should remove plugin`() {

    val app = kotlinProject(":app") {
      hasKapt = true

      addExternalDependency(ConfigurationName.api, dagger)

      buildFile {
        """
        plugins {
          kotlin("jvm")
          kotlin("kapt")
        }

        dependencies {
          api("$dagger")
        }
        """
      }
    }

    run().isSuccess shouldBe true

    app.buildFile shouldHaveText """
        plugins {
          kotlin("jvm")
          // kotlin("kapt")  // ModuleCheck finding [unused-kapt-plugin]
        }

        dependencies {
          api("$dagger")
        }
    """

    logger.parsedReport() shouldBe listOf(
      ":app" to listOf(
        unusedKaptPlugin(
          fixed = true,
          dependency = "org.jetbrains.kotlin.kapt",
          position = "3, 3"
        )
      )
    )
  }

  @Test
  fun `unused with main kapt no other processors should remove plugin`() {

    val app = kotlinProject(":app") {
      hasKapt = true

      buildFile {
        """
        plugins {
          id("kotlin-jvm")
          id("kotlin-kapt")
        }
        """
      }
    }

    run().isSuccess shouldBe true

    app.buildFile shouldHaveText """
      plugins {
        id("kotlin-jvm")
        // id("kotlin-kapt")  // ModuleCheck finding [unused-kapt-plugin]
      }
    """

    logger.parsedReport() shouldBe listOf(
      ":app" to listOf(
        unusedKaptPlugin(
          fixed = true,
          dependency = "org.jetbrains.kotlin.kapt",
          position = "3, 3"
        )
      )
    )
  }

  @Test
  fun `unused kapt should be ignored if suppressed at the statement`() {
    val lib1 = androidLibrary(":lib1", "com.modulecheck.lib1") {
      hasKapt = true
      buildFile {
        """
        plugins {
          id("com.android.library")
          kotlin("android")
          @Suppress("unused-kapt-plugin")
          id("org.jetbrains.kotlin.kapt")
        }
        """
      }
    }

    run(autoCorrect = true).isSuccess shouldBe true

    lib1.buildFile.readText() shouldBe """
      plugins {
        id("com.android.library")
        kotlin("android")
        @Suppress("unused-kapt-plugin")
        id("org.jetbrains.kotlin.kapt")
      }
    """

    logger.parsedReport() shouldBe listOf()
  }

  @Test
  fun `unused kapt should be ignored if suppressed with old finding name`() {
    val lib1 = androidLibrary(":lib1", "com.modulecheck.lib1") {
      hasKapt = true
      buildFile {
        """
        plugins {
          id("com.android.library")
          kotlin("android")
          @Suppress("unusedkaptplugin")
          id("org.jetbrains.kotlin.kapt")
        }
        """
      }
    }

    run(autoCorrect = true).isSuccess shouldBe true

    lib1.buildFile.readText() shouldBe """
      plugins {
        id("com.android.library")
        kotlin("android")
        @Suppress("unusedkaptplugin")
        id("org.jetbrains.kotlin.kapt")
      }
    """

    logger.parsedReport() shouldBe listOf()
  }

  @Test
  fun `unused kapt should be fixed if suppressed with some other finding name`() {
    val lib1 = androidLibrary(":lib1", "com.modulecheck.lib1") {
      hasKapt = true

      buildFile {
        """
        plugins {
          id("com.android.library")
          kotlin("android")
          @Suppress("some-other-name")
          id("org.jetbrains.kotlin.kapt")
        }
        """
      }
    }

    run(autoCorrect = true).isSuccess shouldBe true

    lib1.buildFile.readText() shouldBe """
      plugins {
        id("com.android.library")
        kotlin("android")
        @Suppress("some-other-name")
        // id("org.jetbrains.kotlin.kapt")  // ModuleCheck finding [unused-kapt-plugin]
      }
    """

    logger.parsedReport() shouldBe listOf(
      ":lib1" to listOf(
        unusedKaptPlugin(
          fixed = true,
          dependency = "org.jetbrains.kotlin.kapt",
          position = "5, 3"
        )
      )
    )
  }

  @Test
  fun `unused kapt should be ignored if suppressed at the block`() {
    val lib1 = androidLibrary(":lib1", "com.modulecheck.lib1") {
      hasKapt = true
      buildFile {
        """
        @Suppress("unused-kapt-plugin")
        plugins {
          id("com.android.library")
          kotlin("android")
          id("org.jetbrains.kotlin.kapt")
        }
        """
      }
    }

    run(autoCorrect = true).isSuccess shouldBe true

    lib1.buildFile.readText() shouldBe """
      @Suppress("unused-kapt-plugin")
      plugins {
        id("com.android.library")
        kotlin("android")
        id("org.jetbrains.kotlin.kapt")
      }
    """

    logger.parsedReport() shouldBe listOf()
  }

  @Test
  fun `unused processor should not make the plugin unused if the processor is suppressed`() {

    val app = kotlinProject(":app") {
      hasKapt = true

      addExternalDependency(ConfigurationName.kapt, dagger)

      buildFile {
        """
        plugins {
          id("kotlin-jvm")
          id("kotlin-kapt")
        }

        dependencies {
          @Suppress("unused-kapt-processor")
          kapt("$dagger")
        }
        """
      }
    }

    run().isSuccess shouldBe true

    app.buildFile shouldHaveText """
      plugins {
        id("kotlin-jvm")
        id("kotlin-kapt")
      }

      dependencies {
        @Suppress("unused-kapt-processor")
        kapt("$dagger")
      }
    """

    logger.parsedReport() shouldBe listOf()
  }

  @Test
  fun `used custom processor using KaptMatcher should not make the plugin unused`() {

    @Suppress("DEPRECATION")
    settings.additionalKaptMatchers = listOf(
      KaptMatcher(
        name = "Processor",
        processor = "com.modulecheck:processor",
        annotationImports = listOf(
          "com\\.modulecheck\\.annotations\\.\\*",
          "com\\.modulecheck\\.annotations\\.TheAnnotation"
        )
      )
    )

    val app = kotlinProject(":app") {
      hasKapt = true

      addExternalDependency(ConfigurationName.kapt, "com.modulecheck:processor:0.0.1")

      buildFile {
        """
        plugins {
          id("kotlin-jvm")
          id("kotlin-kapt")
        }

        dependencies {
          kapt("com.modulecheck:processor:0.0.1")
        }
        """
      }

      addKotlinSource(
        """
        package com.modulecheck.lib1

        import com.modulecheck.annotations.TheAnnotation

        @TheAnnotation
        class Lib1Class
        """
      )
    }

    run().isSuccess shouldBe true

    app.buildFile shouldHaveText """
      plugins {
        id("kotlin-jvm")
        id("kotlin-kapt")
      }

      dependencies {
        kapt("com.modulecheck:processor:0.0.1")
      }
    """

    logger.parsedReport() shouldBe listOf()
  }

  @Test
  fun `used custom processor in test source using KaptMatcher should not make the plugin unused`() {

    @Suppress("DEPRECATION")
    settings.additionalKaptMatchers = listOf(
      KaptMatcher(
        name = "Processor",
        processor = "com.modulecheck:processor",
        annotationImports = listOf(
          "com\\.modulecheck\\.annotations\\.TheAnnotation"
        )
      )
    )

    val app = kotlinProject(":app") {
      hasKapt = true

      addExternalDependency("kaptTest".asConfigurationName(), "com.modulecheck:processor:0.0.1")

      buildFile {
        """
        plugins {
          id("kotlin-jvm")
          id("kotlin-kapt")
        }

        dependencies {
          kaptTest("com.modulecheck:processor:0.0.1")
        }
        """
      }

      addKotlinSource(
        """
        package com.modulecheck.annotations

        @TheAnnotation
        class Lib1Class
        """,
        SourceSetName.TEST
      )
    }

    run().isSuccess shouldBe true

    app.buildFile shouldHaveText """
      plugins {
        id("kotlin-jvm")
        id("kotlin-kapt")
      }

      dependencies {
        kaptTest("com.modulecheck:processor:0.0.1")
      }
    """

    logger.parsedReport() shouldBe listOf()
  }

  @Test
  fun `used custom annotation processor using CodeGeneratorBinding should not make the plugin unused`() {

    settings.additionalCodeGenerators = listOf(
      CodeGeneratorBinding.AnnotationProcessor(
        "Processor",
        "com.modulecheck:processor",
        listOf(
          "com.modulecheck.annotations.TheAnnotation"
        )
      )
    )

    val app = kotlinProject(":app") {
      hasKapt = true

      addExternalDependency(ConfigurationName.kapt, "com.modulecheck:processor:0.0.1")

      buildFile {
        """
        plugins {
          id("kotlin-jvm")
          id("kotlin-kapt")
        }

        dependencies {
          kapt("com.modulecheck:processor:0.0.1")
        }
        """
      }

      addKotlinSource(
        """
        package com.modulecheck.lib1

        import com.modulecheck.annotations.TheAnnotation

        @TheAnnotation
        class Lib1Class
        """
      )
    }

    run().isSuccess shouldBe true

    app.buildFile shouldHaveText """
      plugins {
        id("kotlin-jvm")
        id("kotlin-kapt")
      }

      dependencies {
        kapt("com.modulecheck:processor:0.0.1")
      }
    """

    logger.parsedReport() shouldBe listOf()
  }
}
