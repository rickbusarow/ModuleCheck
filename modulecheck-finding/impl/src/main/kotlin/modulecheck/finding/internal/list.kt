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

package modulecheck.finding.internal

import modulecheck.finding.Finding.Position
import modulecheck.model.dependency.ConfigurationName

/**
 * Finds the position of a specific Gradle project path and configuration in a list of strings.
 *
 * @param path The Gradle project path to find.
 * @param configuration The [ConfigurationName] related to the dependency to find.
 * @receiver The list of strings to search in, typically lines of a build.gradle file.
 * @return The [Position] of the dependency in the list
 *   if found, null otherwise. The position is 1-indexed.
 */
fun List<String>.positionOf(path: String, configuration: ConfigurationName): Position? {
  val reg = """.*"?${configuration.value}"?\(project[(]?(?:path =\s*)"$path".*""".toRegex()

  val row = indexOfFirst { it.trim().matches(reg) }

  if (row < 0) return null

  val col = get(row).indexOfFirst { it != ' ' }

  return Position(row + 1, col + 1)
}
