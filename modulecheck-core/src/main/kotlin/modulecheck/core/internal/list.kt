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

package modulecheck.core.internal

import modulecheck.api.finding.Finding.Position
import modulecheck.parsing.gradle.ConfigurationName

fun List<String>.positionOf(
  path: String,
  configuration: ConfigurationName
): Position? {
  val reg = """.*"?${configuration.value}"?\(project[(]?(?:path =\s*)"$path".*""".toRegex()

  val row = indexOfFirst { it.trim().matches(reg) }

  if (row < 0) return null

  val col = get(row).indexOfFirst { it != ' ' }

  return Position(row + 1, col + 1)
}
