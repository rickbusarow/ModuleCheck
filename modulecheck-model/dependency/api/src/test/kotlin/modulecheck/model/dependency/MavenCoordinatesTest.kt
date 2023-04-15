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

import io.kotest.matchers.shouldBe
import modulecheck.model.dependency.MavenCoordinates
import org.junit.jupiter.api.Test

class MavenCoordinatesTest {

  @Test
  fun `a name with a period is parsed`() {

    MavenCoordinates.parseOrNull(
      "javax.inject:javax.inject:1"
    ) shouldBe MavenCoordinates(
      group = "javax.inject",
      moduleName = "javax.inject",
      version = "1"
    )
  }

  @Test
  fun `a single-digit version is parsed`() {

    MavenCoordinates.parseOrNull(
      "javax.inject:javax.inject:1"
    ) shouldBe MavenCoordinates(
      group = "javax.inject",
      moduleName = "javax.inject",
      version = "1"
    )
  }

  @Test
  fun `a SNAPSHOT version is parsed`() {

    MavenCoordinates.parseOrNull(
      "com.jakewharton.timber:timber-jdk:5.0.0-SNAPSHOT"
    ) shouldBe MavenCoordinates(
      group = "com.jakewharton.timber",
      moduleName = "timber-jdk",
      version = "5.0.0-SNAPSHOT"
    )
  }

  @Test
  fun `a beta version is parsed`() {

    MavenCoordinates.parseOrNull(
      "com.rickbusarow.dispatch:dispatch-core:1.0.0-beta10"
    ) shouldBe MavenCoordinates(
      group = "com.rickbusarow.dispatch",
      moduleName = "dispatch-core",
      version = "1.0.0-beta10"
    )
  }

  @Test
  fun `a non-semantic version is parsed`() {

    MavenCoordinates.parseOrNull(
      "org.jetbrains:annotations:13.0"
    ) shouldBe MavenCoordinates(
      group = "org.jetbrains",
      moduleName = "annotations",
      version = "13.0"
    )
  }
}
