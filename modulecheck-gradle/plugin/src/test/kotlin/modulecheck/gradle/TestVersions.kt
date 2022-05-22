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

import hermit.test.ResetManager
import modulecheck.testing.DynamicTests
import org.junit.jupiter.api.DynamicTest

interface VersionsMatrixTest : DynamicTests, ResetManager {

  val kotlinVersions get() = listOf("1.5.32", "1.6.21", "1.7.0-Beta")
  val gradleVersions get() = listOf("7.2", "7.3.3", "7.4.2", "7.5-rc-1")
  val agpVersions get() = listOf("7.0.1", "7.1.3", "7.2.0")
  val anvilVersions get() = listOf("2.3.11", "2.4.0")

  var kotlinVersion: String
  var agpVersion: String
  var gradleVersion: String
  var anvilVersion: String

  fun testProjectVersions() =
    gradleVersions.flatMap { gradleVersion ->
      agpVersions.flatMap { agpVersion ->
        kotlinVersions.map { kotlinVersion ->
          TestVersions(gradleVersion, agpVersion, kotlinVersion, anvilVersion)
        }
      }
    }

  fun gradle(
    filter: (String) -> Boolean = { true },
    action: (String) -> Unit
  ): List<DynamicTest> {

    return gradleVersions.dynamic(
      filter = { filter(this) },
      testName = { "gradle $it" },
      setup = { gradleVersion = it },
      action = action
    )
  }

  fun agp(
    filter: (String) -> Boolean = { true },
    action: (String) -> Unit
  ): List<DynamicTest> {

    return agpVersions.dynamic(
      filter = { filter(this) },
      testName = { "agp $it" },
      setup = { agpVersion = it },
      action = action
    )
  }
  fun kotlin(
    filter: (String) -> Boolean = { true },
    action: (String) -> Unit
  ): List<DynamicTest> {

    return kotlinVersions.dynamic(
      filter = { filter(this) },
      testName = { "kotlin $it" },
      setup = { kotlinVersion = it },
      action = action
    )
  }

  fun matrix(
    action: (TestVersions) -> Unit
  ): List<DynamicTest> {

    return testProjectVersions().toList()
      .dynamic(
        filter = { isValid() },
        testName = { it.toString() },
        setup = { subject ->
          agpVersion = subject.agpVersion
          gradleVersion = subject.gradleVersion
          kotlinVersion = subject.kotlinVersion
        },
        action = action
      )
  }

  fun <T> List<T>.dynamic(
    filter: T.() -> Boolean,
    testName: (T) -> String,
    setup: (T) -> Unit,
    action: (T) -> Unit
  ): List<DynamicTest> {
    return filter { it.filter() }
      .map { subject ->

        dynamicTest(
          subject = subject,
          testName = testName(subject),
          setup = setup,
          action = action
        )
      }
  }

  fun <T> dynamicTest(
    subject: T,
    testName: String,
    setup: (T) -> Unit,
    action: (T) -> Unit
  ): DynamicTest
}

data class TestVersions(
  val gradleVersion: String,
  val agpVersion: String,
  val kotlinVersion: String,
  val anvilVersion: String
) {
  override fun toString(): String {
    return "[gradle $gradleVersion, agp $agpVersion, kotlin $kotlinVersion, anvil $anvilVersion]"
  }

  fun isValid(): Boolean {
    return when {
      // anvil 2.3.x requires 1.5.32, anvil 2.4.x requires 1.6.x
      kotlinVersion == "1.5.32" && anvilVersion >= "2.4.0" -> false
      kotlinVersion >= "1.6.0" && anvilVersion < "2.4.0" -> false
      // agp 7.2.0 requires gradle 7.3.3
      gradleVersion < "7.3.3" && agpVersion >= "7.2.0" -> false
      // these exclusions just save time
      kotlinVersion == "1.5.32" && agpVersion >= "7.1.0" -> false
      kotlinVersion <= "1.6.10" && agpVersion < "7.1.0" -> false
      gradleVersion < "7.4" && agpVersion < "7.1.0" -> false
      else -> true
    }
  }

  companion object {

    val DEFAULT_GRADLE_VERSION: String = System
      .getProperty("modulecheck.gradleVersion", "7.4.2")
      /*
       * The GitHub Actions test matrix parses "7.0" into an Int and passes in a command
       * line argument of "7". That version doesn't resolve.  So if the String doesn't contain
       * a period, just append ".0"
       */
      .let { prop ->
        if (prop.contains('.')) prop else "$prop.0"
      }
    val DEFAULT_KOTLIN_VERSION: String =
      System.getProperty("modulecheck.kotlinVersion", "1.6.20")
    val DEFAULT_AGP_VERSION: String =
      System.getProperty("modulecheck.agpVersion", "7.0.4")
    val DEFAULT_ANVIL_VERSION: String =
      System.getProperty("modulecheck.anvilVersion", "2.4.0")
  }
}
