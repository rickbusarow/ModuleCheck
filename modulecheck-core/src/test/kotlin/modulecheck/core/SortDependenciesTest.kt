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

package modulecheck.core

import modulecheck.api.test.*
import modulecheck.core.rule.ModuleCheckRuleFactory
import modulecheck.core.rule.MultiRuleFindingFactory
import org.junit.jupiter.api.Test
import java.io.File

class SortDependenciesTest : ProjectTest() {

  val ruleFactory by resets { ModuleCheckRuleFactory() }

  val baseSettings by resets {
    TestSettings(checks = TestChecksSettings(sortDependencies = true))
  }
  val logger by resets { ReportingLogger() }
  val findingFactory by resets {
    MultiRuleFindingFactory(
      baseSettings,
      ruleFactory.create(baseSettings)
    )
  }

  @Test
  fun `kts out-of-order dependencies should be sorted`() {

    val runner = ModuleCheckRunner(
      autoCorrect = true,
      settings = baseSettings,
      findingFactory = findingFactory,
      logger = logger
    )

    val lib1 = project(":lib1") {
      buildFile.writeKotlin(
        """
      plugins {
        kotlin("jvm")
      }

      dependencies {
        runtimeOnly(project(path = "lib-1"))
        api(project(path = "lib-3"))
        implementation(project(path = "lib-7"))
        compileOnly(project(path = "lib-4"))
        api(project(path = "lib-0"))
        testImplementation(project(path = "lib-5"))
        testImplementation("org.junit.jupiter:junit-jupiter-api:5.7.0")
        compileOnly(project(path = "lib-6"))
        implementation(project(path = "lib-2"))
        implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.4.2")
        testImplementation(project(path = "lib-8"))
        implementation(project(path = "lib-9"))
        api("com.squareup:kotlinpoet:1.7.2")
      }
      """
      )
    }

    runner.run(allProjects()).isSuccess shouldBe true

    lib1.buildFile.readText() shouldBe """
      plugins {
        kotlin("jvm")
      }

      dependencies {
        api("com.squareup:kotlinpoet:1.7.2")

        api(project(path = "lib-0"))
        api(project(path = "lib-3"))

        compileOnly(project(path = "lib-4"))
        compileOnly(project(path = "lib-6"))

        implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.4.2")

        implementation(project(path = "lib-2"))
        implementation(project(path = "lib-7"))
        implementation(project(path = "lib-9"))

        runtimeOnly(project(path = "lib-1"))

        testImplementation("org.junit.jupiter:junit-jupiter-api:5.7.0")

        testImplementation(project(path = "lib-5"))
        testImplementation(project(path = "lib-8"))
      }
      """

    logger.collectReport()
      .joinToString()
      .clean() shouldBe """
          :lib1
                 dependency    name                    source    build file
              ✔                unsortedDependencies              /lib1/build.gradle.kts:

      ModuleCheck found 1 issue
      """.trimIndent()
  }

  @Test
  fun `kts multi-line comments should move with their declarations`() {

    val runner = ModuleCheckRunner(
      autoCorrect = true,
      settings = baseSettings,
      findingFactory = findingFactory,
      logger = logger
    )

    val lib1 = project(":lib1") {
      buildFile.writeKotlin(
        """
      plugins {
        kotlin("jvm")
      }

      dependencies {

        /*
        a multi-line comment
        */
        runtimeOnly(project(path = "lib-1"))
        api(project(path = "lib-3"))
      }
      """
      )
    }

    runner.run(allProjects()).isSuccess shouldBe true

    lib1.buildFile.readText() shouldBe """
      plugins {
        kotlin("jvm")
      }

      dependencies {

        api(project(path = "lib-3"))

        /*
        a multi-line comment
        */
        runtimeOnly(project(path = "lib-1"))
      }
      """

    logger.collectReport()
      .joinToString()
      .clean() shouldBe """
          :lib1
                 dependency    name                    source    build file
              ✔                unsortedDependencies              /lib1/build.gradle.kts:

      ModuleCheck found 1 issue
      """.trimIndent()
  }

  @Test
  fun `kts multi-line kdoc comments should move with their declarations`() {

    val runner = ModuleCheckRunner(
      autoCorrect = true,
      settings = baseSettings,
      findingFactory = findingFactory,
      logger = logger
    )

    val lib1 = project(":lib1") {
      buildFile.writeKotlin(
        """
      plugins {
        kotlin("jvm")
      }

      dependencies {

        /**
         * a multi-line comment
         */
        runtimeOnly(project(path = "lib-1"))
        api(project(path = "lib-3"))
      }
      """
      )
    }

    runner.run(allProjects()).isSuccess shouldBe true

    lib1.buildFile.readText() shouldBe """
      plugins {
        kotlin("jvm")
      }

      dependencies {

        api(project(path = "lib-3"))

        /**
         * a multi-line comment
         */
        runtimeOnly(project(path = "lib-1"))
      }
      """

    logger.collectReport()
      .joinToString()
      .clean() shouldBe """
          :lib1
                 dependency    name                    source    build file
              ✔                unsortedDependencies              /lib1/build.gradle.kts:

      ModuleCheck found 1 issue
      """.trimIndent()
  }

  @Test
  fun `kts preceding comments should move with their declarations`() {

    val runner = ModuleCheckRunner(
      autoCorrect = true,
      settings = baseSettings,
      findingFactory = findingFactory,
      logger = logger
    )

    val lib1 = project(":lib1") {
      buildFile.writeKotlin(
        """
      plugins {
        kotlin("jvm")
      }

      dependencies {

        // preceding comment
        runtimeOnly(project(path = "lib-1"))
        api(project(path = "lib-3"))
      }
      """
      )
    }

    runner.run(allProjects()).isSuccess shouldBe true

    lib1.buildFile.readText() shouldBe """
      plugins {
        kotlin("jvm")
      }

      dependencies {

        api(project(path = "lib-3"))

        // preceding comment
        runtimeOnly(project(path = "lib-1"))
      }
      """

    logger.collectReport()
      .joinToString()
      .clean() shouldBe """
          :lib1
                 dependency    name                    source    build file
              ✔                unsortedDependencies              /lib1/build.gradle.kts:

      ModuleCheck found 1 issue
      """.trimIndent()
  }

  @Test
  fun `kts trailing comments should move with their declarations`() {

    val runner = ModuleCheckRunner(
      autoCorrect = true,
      settings = baseSettings,
      findingFactory = findingFactory,
      logger = logger
    )

    val lib1 = project(":lib1") {
      buildFile.writeKotlin(
        """
      plugins {
        kotlin("jvm")
      }

      dependencies {

        runtimeOnly(project(path = "lib-1")) // trailing comment
        api(project(path = "lib-3"))
      }
      """
      )
    }

    runner.run(allProjects()).isSuccess shouldBe true

    lib1.buildFile.readText() shouldBe """
      plugins {
        kotlin("jvm")
      }

      dependencies {

        api(project(path = "lib-3"))

        runtimeOnly(project(path = "lib-1")) // trailing comment
      }
      """

    logger.collectReport()
      .joinToString()
      .clean() shouldBe """
          :lib1
                 dependency    name                    source    build file
              ✔                unsortedDependencies              /lib1/build.gradle.kts:

      ModuleCheck found 1 issue
      """.trimIndent()
  }

  @Test
  fun `kts sorting should be idempotent`() {

    val runner = ModuleCheckRunner(
      autoCorrect = true,
      settings = baseSettings,
      findingFactory = findingFactory,
      logger = logger
    )

    val lib1 = project(":lib1") {
      buildFile.writeKotlin(
        """
      plugins {
        kotlin("jvm")
      }

      dependencies {
        runtimeOnly(project(path = "lib-1"))
        api(project(path = "lib-3"))
        implementation(project(path = "lib-7"))
        compileOnly(project(path = "lib-4"))
        api(project(path = "lib-0"))
        testImplementation(project(path = "lib-5"))
        testImplementation("org.junit.jupiter:junit-jupiter-api:5.7.0")
        compileOnly(project(path = "lib-6"))
        implementation(project(path = "lib-2"))
        implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.4.2")
        testImplementation(project(path = "lib-8"))
        implementation(project(path = "lib-9"))
        api("com.squareup:kotlinpoet:1.7.2")
      }
      """
      )
    }

    runner.run(allProjects()).isSuccess shouldBe true

    lib1.buildFile.readText() shouldBe """
      plugins {
        kotlin("jvm")
      }

      dependencies {
        api("com.squareup:kotlinpoet:1.7.2")

        api(project(path = "lib-0"))
        api(project(path = "lib-3"))

        compileOnly(project(path = "lib-4"))
        compileOnly(project(path = "lib-6"))

        implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.4.2")

        implementation(project(path = "lib-2"))
        implementation(project(path = "lib-7"))
        implementation(project(path = "lib-9"))

        runtimeOnly(project(path = "lib-1"))

        testImplementation("org.junit.jupiter:junit-jupiter-api:5.7.0")

        testImplementation(project(path = "lib-5"))
        testImplementation(project(path = "lib-8"))
      }
      """

    logger.collectReport()
      .joinToString()
      .clean() shouldBe """
          :lib1
                 dependency    name                    source    build file
              ✔                unsortedDependencies              /lib1/build.gradle.kts:

      ModuleCheck found 1 issue
      """.trimIndent()
    logger.clear()

    runner.run(allProjects()).isSuccess shouldBe true

    lib1.buildFile.readText() shouldBe """
      plugins {
        kotlin("jvm")
      }

      dependencies {
        api("com.squareup:kotlinpoet:1.7.2")

        api(project(path = "lib-0"))
        api(project(path = "lib-3"))

        compileOnly(project(path = "lib-4"))
        compileOnly(project(path = "lib-6"))

        implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.4.2")

        implementation(project(path = "lib-2"))
        implementation(project(path = "lib-7"))
        implementation(project(path = "lib-9"))

        runtimeOnly(project(path = "lib-1"))

        testImplementation("org.junit.jupiter:junit-jupiter-api:5.7.0")

        testImplementation(project(path = "lib-5"))
        testImplementation(project(path = "lib-8"))
      }
      """

    logger.collectReport()
      .joinToString()
      .clean() shouldBe """ModuleCheck found 0 issues"""
  }

  @Test
  fun `groovy out-of-order plugins should be sorted`() {

    val runner = ModuleCheckRunner(
      autoCorrect = true,
      settings = baseSettings,
      findingFactory = findingFactory,
      logger = logger
    )

    val lib1 = project(":lib1") {
      buildFile.delete()
      buildFile = File(projectDir, "build.gradle")
      buildFile.writeGroovy(
        """
      plugins {
        id 'org.jetbrains.kotlin.jvm'
      }

      dependencies {
        runtimeOnly project(path = "lib-1")
        api project(path = "lib-3")
        implementation project(path = "lib-7")
        compileOnly project(path = "lib-4")
        api project(path = "lib-0")
        testImplementation project(path = "lib-5")
        compileOnly project(path = "lib-6")
        implementation project(path = "lib-2")
        testImplementation project(path = "lib-8")
        implementation project(path = "lib-9")


        testImplementation 'org.junit.jupiter:junit-jupiter-api:5.7.0'
        implementation 'org.jetbrains.kotlinx:kotlinx-coroutines-core:1.4.2'
        api 'com.squareup:kotlinpoet:1.7.2'
      }
      """
      )
    }

    runner.run(allProjects()).isSuccess shouldBe true

    lib1.buildFile.readText() shouldBe """
      plugins {
        id 'org.jetbrains.kotlin.jvm'
      }

      dependencies {
        api 'com.squareup:kotlinpoet:1.7.2'

        api project(path = "lib-0")
        api project(path = "lib-3")

        compileOnly project(path = "lib-4")
        compileOnly project(path = "lib-6")

        implementation 'org.jetbrains.kotlinx:kotlinx-coroutines-core:1.4.2'

        implementation project(path = "lib-2")
        implementation project(path = "lib-7")
        implementation project(path = "lib-9")

        runtimeOnly project(path = "lib-1")

        testImplementation 'org.junit.jupiter:junit-jupiter-api:5.7.0'

        testImplementation project(path = "lib-5")
        testImplementation project(path = "lib-8")
      }
      """

    logger.collectReport()
      .joinToString()
      .clean() shouldBe """
          :lib1
                 dependency    name                    source    build file
              ✔                unsortedDependencies              /lib1/build.gradle:

      ModuleCheck found 1 issue
      """.trimIndent()
  }

  @Test
  fun `groovy multi-line comments should move with declarations`() {

    val runner = ModuleCheckRunner(
      autoCorrect = true,
      settings = baseSettings,
      findingFactory = findingFactory,
      logger = logger
    )

    val lib1 = project(":lib1") {
      buildFile.delete()
      buildFile = File(projectDir, "build.gradle")
      buildFile.writeGroovy(
        """
      plugins {
        id 'org.jetbrains.kotlin.jvm'
      }

      dependencies {

        /*
        a multi-line comment
        */
        runtimeOnly project(path = "lib-1")
        api project(path = "lib-3")
      }
      """
      )
    }

    runner.run(allProjects()).isSuccess shouldBe true

    lib1.buildFile.readText() shouldBe """
      plugins {
        id 'org.jetbrains.kotlin.jvm'
      }

      dependencies {

        api project(path = "lib-3")

        /*
        a multi-line comment
        */
        runtimeOnly project(path = "lib-1")
      }
      """

    logger.collectReport()
      .joinToString()
      .clean() shouldBe """
          :lib1
                 dependency    name                    source    build file
              ✔                unsortedDependencies              /lib1/build.gradle:

      ModuleCheck found 1 issue
      """.trimIndent()
  }

  @Test
  fun `groovy multi-line javadoc comments should move with declarations`() {

    val runner = ModuleCheckRunner(
      autoCorrect = true,
      settings = baseSettings,
      findingFactory = findingFactory,
      logger = logger
    )

    val lib1 = project(":lib1") {
      buildFile.delete()
      buildFile = File(projectDir, "build.gradle")
      buildFile.writeGroovy(
        """
      plugins {
        id 'org.jetbrains.kotlin.jvm'
      }

      dependencies {

        /**
         * a multi-line comment
         */
        runtimeOnly project(path = "lib-1")
        api project(path = "lib-3")
      }
      """
      )
    }

    runner.run(allProjects()).isSuccess shouldBe true

    lib1.buildFile.readText() shouldBe """
      plugins {
        id 'org.jetbrains.kotlin.jvm'
      }

      dependencies {

        api project(path = "lib-3")

        /**
         * a multi-line comment
         */
        runtimeOnly project(path = "lib-1")
      }
      """

    logger.collectReport()
      .joinToString()
      .clean() shouldBe """
          :lib1
                 dependency    name                    source    build file
              ✔                unsortedDependencies              /lib1/build.gradle:

      ModuleCheck found 1 issue
      """.trimIndent()
  }

  @Test
  fun `groovy preceding comments should move with declarations`() {

    val runner = ModuleCheckRunner(
      autoCorrect = true,
      settings = baseSettings,
      findingFactory = findingFactory,
      logger = logger
    )

    val lib1 = project(":lib1") {
      buildFile.delete()
      buildFile = File(projectDir, "build.gradle")
      buildFile.writeGroovy(
        """
      plugins {
        id 'org.jetbrains.kotlin.jvm'
      }

      dependencies {

        // preceding comment
        runtimeOnly project(path = "lib-1")
        api project(path = "lib-3")
      }
      """
      )
    }

    runner.run(allProjects()).isSuccess shouldBe true

    lib1.buildFile.readText() shouldBe """
      plugins {
        id 'org.jetbrains.kotlin.jvm'
      }

      dependencies {

        api project(path = "lib-3")

        // preceding comment
        runtimeOnly project(path = "lib-1")
      }
      """

    logger.collectReport()
      .joinToString()
      .clean() shouldBe """
          :lib1
                 dependency    name                    source    build file
              ✔                unsortedDependencies              /lib1/build.gradle:

      ModuleCheck found 1 issue
      """.trimIndent()
  }

  @Test
  fun `groovy trailing comments should move with declarations`() {

    val runner = ModuleCheckRunner(
      autoCorrect = true,
      settings = baseSettings,
      findingFactory = findingFactory,
      logger = logger
    )

    val lib1 = project(":lib1") {
      buildFile.delete()
      buildFile = File(projectDir, "build.gradle")
      buildFile.writeGroovy(
        """
      plugins {
        id 'org.jetbrains.kotlin.jvm'
      }

      dependencies {

        runtimeOnly project(path = "lib-1") // trailing comment
        api project(path = "lib-3")
      }
      """
      )
    }

    runner.run(allProjects()).isSuccess shouldBe true

    lib1.buildFile.readText() shouldBe """
      plugins {
        id 'org.jetbrains.kotlin.jvm'
      }

      dependencies {

        api project(path = "lib-3")

        runtimeOnly project(path = "lib-1") // trailing comment
      }
      """

    logger.collectReport()
      .joinToString()
      .clean() shouldBe """
          :lib1
                 dependency    name                    source    build file
              ✔                unsortedDependencies              /lib1/build.gradle:

      ModuleCheck found 1 issue
      """.trimIndent()
  }

  @Test
  fun `groovy sorting should be idempotent`() {

    val runner = ModuleCheckRunner(
      autoCorrect = true,
      settings = baseSettings,
      findingFactory = findingFactory,
      logger = logger
    )

    val lib1 = project(":lib1") {
      buildFile.delete()
      buildFile = File(projectDir, "build.gradle")
      buildFile.writeGroovy(
        """
      plugins {
        id 'org.jetbrains.kotlin.jvm'
      }

      dependencies {
        runtimeOnly project(path = "lib-1")
        api project(path = "lib-3")
        implementation project(path = "lib-9")


        testImplementation 'org.junit.jupiter:junit-jupiter-api:5.7.0'
        implementation 'org.jetbrains.kotlinx:kotlinx-coroutines-core:1.4.2'
        api 'com.squareup:kotlinpoet:1.7.2'

        implementation project(path = "lib-7")
        compileOnly project(path = "lib-4")
        api project(path = "lib-0")
        testImplementation project(path = "lib-5")
        compileOnly project(path = "lib-6")
        implementation project(path = "lib-2")
        testImplementation project(path = "lib-8")
      }
      """
      )
    }

    runner.run(allProjects()).isSuccess shouldBe true

    lib1.buildFile.readText() shouldBe """
      plugins {
        id 'org.jetbrains.kotlin.jvm'
      }

      dependencies {
        api 'com.squareup:kotlinpoet:1.7.2'

        api project(path = "lib-0")
        api project(path = "lib-3")

        compileOnly project(path = "lib-4")
        compileOnly project(path = "lib-6")

        implementation 'org.jetbrains.kotlinx:kotlinx-coroutines-core:1.4.2'

        implementation project(path = "lib-2")
        implementation project(path = "lib-7")
        implementation project(path = "lib-9")

        runtimeOnly project(path = "lib-1")

        testImplementation 'org.junit.jupiter:junit-jupiter-api:5.7.0'

        testImplementation project(path = "lib-5")
        testImplementation project(path = "lib-8")
      }
      """

    logger.collectReport()
      .joinToString()
      .clean() shouldBe """
          :lib1
                 dependency    name                    source    build file
              ✔                unsortedDependencies              /lib1/build.gradle:

      ModuleCheck found 1 issue
      """.trimIndent()
    logger.clear()

    runner.run(allProjects()).isSuccess shouldBe true

    lib1.buildFile.readText() shouldBe """
      plugins {
        id 'org.jetbrains.kotlin.jvm'
      }

      dependencies {
        api 'com.squareup:kotlinpoet:1.7.2'

        api project(path = "lib-0")
        api project(path = "lib-3")

        compileOnly project(path = "lib-4")
        compileOnly project(path = "lib-6")

        implementation 'org.jetbrains.kotlinx:kotlinx-coroutines-core:1.4.2'

        implementation project(path = "lib-2")
        implementation project(path = "lib-7")
        implementation project(path = "lib-9")

        runtimeOnly project(path = "lib-1")

        testImplementation 'org.junit.jupiter:junit-jupiter-api:5.7.0'

        testImplementation project(path = "lib-5")
        testImplementation project(path = "lib-8")
      }
      """

    logger.collectReport()
      .joinToString()
      .clean() shouldBe """ModuleCheck found 0 issues"""
  }
}
