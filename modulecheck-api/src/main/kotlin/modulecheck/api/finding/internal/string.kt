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

package modulecheck.api.finding.internal

import modulecheck.api.finding.Finding.Position

fun String.positionOfStatement(statement: String): Position {

  val lines = lines()

  var index = indexOf(statement.trimStart())

  var row = 0

  while (lines[row].length < index) {
    // if the current row's string isn't long enough, subtract its length from the total index
    // and move on to the next row.  Subtract an additional 1 because the newline character
    // in the full string isn't included in the line's string.
    index -= (lines[row].length + 1)
    row++
  }
  return Position(row + 1, index + 1)
}
