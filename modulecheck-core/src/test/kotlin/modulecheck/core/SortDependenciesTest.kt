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

package modulecheck.core

import modulecheck.api.test.TestChecksSettings
import modulecheck.api.test.TestSettings
import modulecheck.runtime.test.ProjectFindingReport.unsortedDependencies
import modulecheck.runtime.test.RunnerTest
import modulecheck.testing.writeGroovy
import org.junit.jupiter.api.Test
import java.io.File

class SortDependenciesTest : RunnerTest() {

  override val settings by resets {
    TestSettings(checks = TestChecksSettings(sortDependencies = true))
  }

  @Test
  fun `kts out-of-order dependencies should be sorted`() {

    val lib1 = project(":lib1") {
      buildFile {
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
      }
    }

    run().isSuccess shouldBe true

    lib1.buildFile shouldHaveText """
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

    logger.parsedReport() shouldBe listOf(
      ":lib1" to listOf(unsortedDependencies(fixed = true))
    )
  }

  @Test
  fun `kts multi-line comments should move with their declarations`() {

    val lib1 = project(":lib1") {
      buildFile {
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
      }
    }

    run().isSuccess shouldBe true

    lib1.buildFile shouldHaveText """
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

    logger.parsedReport() shouldBe listOf(
      ":lib1" to listOf(unsortedDependencies(fixed = true))
    )
  }

  @Test
  fun `kts multi-line kdoc comments should move with their declarations`() {

    val lib1 = project(":lib1") {
      buildFile {
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
      }
    }

    run().isSuccess shouldBe true

    lib1.buildFile shouldHaveText """
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

    logger.parsedReport() shouldBe listOf(
      ":lib1" to listOf(unsortedDependencies(fixed = true))
    )
  }

  @Test
  fun `kts preceding comments should move with their declarations`() {

    val lib1 = project(":lib1") {
      buildFile {
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
      }
    }

    run().isSuccess shouldBe true

    lib1.buildFile shouldHaveText """
      plugins {
        kotlin("jvm")
      }

      dependencies {

        api(project(path = "lib-3"))

        // preceding comment
        runtimeOnly(project(path = "lib-1"))
      }
    """

    logger.parsedReport() shouldBe listOf(
      ":lib1" to listOf(unsortedDependencies(fixed = true))
    )
  }

  @Test
  fun `kts trailing comments should move with their declarations`() {

    val lib1 = project(":lib1") {
      buildFile {
        """
      plugins {
        kotlin("jvm")
      }

      dependencies {

        runtimeOnly(project(path = "lib-1")) // trailing comment
        api(project(path = "lib-3"))
      }
        """
      }
    }

    run().isSuccess shouldBe true

    lib1.buildFile shouldHaveText """
      plugins {
        kotlin("jvm")
      }

      dependencies {

        api(project(path = "lib-3"))

        runtimeOnly(project(path = "lib-1")) // trailing comment
      }
    """

    logger.parsedReport() shouldBe listOf(
      ":lib1" to listOf(unsortedDependencies(fixed = true))
    )
  }

  @Test
  fun `kts sorting should be idempotent`() {

    val lib1 = project(":lib1") {
      buildFile {
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
      }
    }

    run().isSuccess shouldBe true

    lib1.buildFile shouldHaveText """
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

    logger.parsedReport() shouldBe listOf(
      ":lib1" to listOf(unsortedDependencies(fixed = true))
    )

    logger.clear()

    run().isSuccess shouldBe true

    lib1.buildFile shouldHaveText """
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

    logger.parsedReport() shouldBe listOf()
  }

  @Test
  fun `groovy out-of-order plugins should be sorted`() {

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

    run().isSuccess shouldBe true

    lib1.buildFile shouldHaveText """
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

    logger.parsedReport() shouldBe listOf(
      ":lib1" to listOf(unsortedDependencies(fixed = true))
    )
  }

  @Test
  fun `groovy multi-line comments should move with declarations`() {

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

    run().isSuccess shouldBe true

    lib1.buildFile shouldHaveText """
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

    logger.parsedReport() shouldBe listOf(
      ":lib1" to listOf(unsortedDependencies(fixed = true))
    )
  }

  @Test
  fun `groovy multi-line javadoc comments should move with declarations`() {

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

    run().isSuccess shouldBe true

    lib1.buildFile shouldHaveText """
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

    logger.parsedReport() shouldBe listOf(
      ":lib1" to listOf(unsortedDependencies(fixed = true))
    )
  }

  @Test
  fun `groovy preceding comments should move with declarations`() {

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

    run().isSuccess shouldBe true

    lib1.buildFile shouldHaveText """
      plugins {
        id 'org.jetbrains.kotlin.jvm'
      }

      dependencies {

        api project(path = "lib-3")

        // preceding comment
        runtimeOnly project(path = "lib-1")
      }
    """

    logger.parsedReport() shouldBe listOf(
      ":lib1" to listOf(unsortedDependencies(fixed = true))
    )
  }

  @Test
  fun `groovy trailing comments should move with declarations`() {

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

    run().isSuccess shouldBe true

    lib1.buildFile shouldHaveText """
      plugins {
        id 'org.jetbrains.kotlin.jvm'
      }

      dependencies {

        api project(path = "lib-3")

        runtimeOnly project(path = "lib-1") // trailing comment
      }
    """

    logger.parsedReport() shouldBe listOf(
      ":lib1" to listOf(unsortedDependencies(fixed = true))
    )
  }

  @Test
  fun `groovy sorting should be idempotent`() {

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

    run().isSuccess shouldBe true

    lib1.buildFile shouldHaveText """
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

    logger.parsedReport() shouldBe listOf(
      ":lib1" to listOf(unsortedDependencies(fixed = true))
    )
    logger.clear()

    run().isSuccess shouldBe true

    lib1.buildFile shouldHaveText """
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

    logger.parsedReport() shouldBe listOf()
  }
}
