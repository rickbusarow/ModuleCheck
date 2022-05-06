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

package modulecheck.reporting.checkstyle

import modulecheck.finding.Finding.FindingResult
import modulecheck.finding.Finding.Position
import modulecheck.finding.FindingName
import modulecheck.parsing.gradle.ProjectPath.StringProjectPath
import modulecheck.testing.BaseTest
import org.junit.jupiter.api.Test
import java.io.File

internal class CheckstyleReporterTest : BaseTest() {

  @Test
  fun `empty result list should create checkstyle xml with no child attributes`() {

    val result = CheckstyleReporter().createXml(emptyList())

    println("`$result`")

    result shouldBe """
      <?xml version="1.0" encoding="UTF-8"?>
      <checkstyle version="4.3">
      </checkstyle>
    """
  }

  @Test
  fun `unfixed result should create checkstyle xml with error type`() {

    val result = CheckstyleReporter().createXml(
      listOf(
        FindingResult(
          dependentPath = StringProjectPath(":dependentPath"),
          findingName = FindingName("some-rule"),
          sourceOrNull = "sourceOrNull",
          configurationName = "configurationName",
          dependencyIdentifier = "dependencyIdentifier",
          positionOrNull = Position(1, 2),
          buildFile = File("buildFile"),
          message = "message",
          fixed = false
        )
      )
    )

    println("`$result`")

    result shouldBe """
      <?xml version="1.0" encoding="UTF-8"?>
      <checkstyle version="4.3">
      	<file name="buildFile">
      		<error line="1" column="2" severity="error" dependency="dependencyIdentifier" message="message" source="modulecheck.some-rule" />
      	</file>
      </checkstyle>
    """
  }

  @Test
  fun `fixed result should create checkstyle xml with info type`() {

    val result = CheckstyleReporter().createXml(
      listOf(
        FindingResult(
          dependentPath = StringProjectPath(":dependentPath"),
          findingName = FindingName("some-rule"),
          sourceOrNull = "sourceOrNull",
          configurationName = "configurationName",
          dependencyIdentifier = "dependencyIdentifier",
          positionOrNull = Position(1, 2),
          buildFile = File("buildFile"),
          message = "message",
          fixed = true
        )
      )
    )

    println("`$result`")

    result shouldBe """
      <?xml version="1.0" encoding="UTF-8"?>
      <checkstyle version="4.3">
      	<file name="buildFile">
      		<error line="1" column="2" severity="info" dependency="dependencyIdentifier" message="message" source="modulecheck.some-rule" />
      	</file>
      </checkstyle>
    """
  }

  @Test
  fun `results should be grouped by build file path`() {

    val result = CheckstyleReporter().createXml(
      listOf(
        FindingResult(
          dependentPath = StringProjectPath(":lib1"),
          findingName = FindingName("some-rule"),
          sourceOrNull = "sourceOrNull",
          configurationName = "configurationName",
          dependencyIdentifier = ":path1",
          positionOrNull = Position(1, 2),
          buildFile = File("lib1/build.gradle.kts"),
          message = "message",
          fixed = true
        ),
        FindingResult(
          dependentPath = StringProjectPath(":lib1"),
          findingName = FindingName("some-rule"),
          sourceOrNull = "sourceOrNull",
          configurationName = "configurationName",
          dependencyIdentifier = ":path2",
          positionOrNull = Position(2, 2),
          buildFile = File("lib1/build.gradle.kts"),
          message = "message",
          fixed = true
        ),
        FindingResult(
          dependentPath = StringProjectPath(":lib2"),
          findingName = FindingName("some-rule"),
          sourceOrNull = "sourceOrNull",
          configurationName = "configurationName",
          dependencyIdentifier = ":path1",
          positionOrNull = Position(1, 2),
          buildFile = File("lib2/build.gradle.kts"),
          message = "message",
          fixed = true
        )
      )
    )

    println("`$result`")

    result shouldBe """
      <?xml version="1.0" encoding="UTF-8"?>
      <checkstyle version="4.3">
      	<file name="lib1/build.gradle.kts">
      		<error line="1" column="2" severity="info" dependency=":path1" message="message" source="modulecheck.some-rule" />
      		<error line="2" column="2" severity="info" dependency=":path2" message="message" source="modulecheck.some-rule" />
      	</file>
      	<file name="lib2/build.gradle.kts">
      		<error line="1" column="2" severity="info" dependency=":path1" message="message" source="modulecheck.some-rule" />
      	</file>
      </checkstyle>
    """
  }
}
