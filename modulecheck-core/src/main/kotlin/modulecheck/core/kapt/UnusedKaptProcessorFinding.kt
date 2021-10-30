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

package modulecheck.core.kapt

import modulecheck.api.Finding.Position
import modulecheck.core.internal.positionOf
import modulecheck.parsing.ConfigurationName
import java.io.File

data class UnusedKaptProcessorFinding(
  override val dependentPath: String,
  override val buildFile: File,
  val dependencyPath: String,
  val configurationName: ConfigurationName
) : UnusedKaptFinding {

  override val message: String
    get() = "The annotation processor dependency is not used in this module.  " +
      "This can be a significant performance hit."

  override val dependencyIdentifier = dependencyPath

  override val findingName = "unusedKaptProcessor"

  override val positionOrNull: Position? by lazy {
    // Kapt paths are different from other project dependencies.
    // Given a module of `:foo:bar:baz` in a project named `my-proj`,
    // the resolved artifact is `my-proj.foo.bar.baz`.
    // Reverse that to get the coordinates actually written in the build file.
    val correctedPath = dependencyPath.split(".")
      .drop(1)
      .joinToString(":", ":")

    buildFile
      .readText()
      .lines()
      .positionOf(dependencyPath, configurationName)
      ?: buildFile
        .readText()
        .lines()
        .positionOf(correctedPath, configurationName)
  }
}
