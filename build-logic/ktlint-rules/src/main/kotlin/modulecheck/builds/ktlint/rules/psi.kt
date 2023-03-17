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

package modulecheck.builds.ktlint.rules

import org.jetbrains.kotlin.kdoc.psi.api.KDoc
import org.jetbrains.kotlin.kdoc.psi.impl.KDocTag
import org.jetbrains.kotlin.psi.psiUtil.collectDescendantsOfType
import org.jetbrains.kotlin.psi.psiUtil.startOffset

internal fun KDoc.getAllTags(): List<KDocTag> {
  return collectDescendantsOfType<KDocTag>()
    .sortedBy { it.startOffset }
}

internal fun KDoc.findIndent(): String {
  val fileLines = containingFile.text.lines()

  var acc = startOffset + 1

  val numSpaces = fileLines.asSequence()
    .mapNotNull {
      if (it.length + 1 < acc) {
        acc -= (it.length + 1)
        null
      } else {
        acc
      }
    }
    .first()
  return " ".repeat(numSpaces)
}
