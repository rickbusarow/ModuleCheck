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

package modulecheck.utils

import org.intellij.lang.annotations.Language
import kotlin.text.replace
import kotlin.text.replace as matchResultReplace

/**
 * code golf for `replace(regex.toRegex(), replacement)`
 *
 * @since 0.12.0
 */
fun String.replaceRegex(
  @Language("regex")
  regex: String,
  replacement: String
): String = replace(
  regex = regex.toRegex(),
  replacement = replacement
)

inline fun String.replaceDestructured(
  regex: Regex,
  crossinline transform: (group1: String) -> String
): String {
  return matchResultReplace(regex) { matchResult ->
    transform(matchResult.destructured.component1())
  }
}

inline fun String.replaceDestructured(
  regex: Regex,
  crossinline transform: (group1: String, group2: String) -> String
): String {
  return matchResultReplace(regex) { matchResult ->
    transform(
      matchResult.destructured.component1(),
      matchResult.destructured.component2()
    )
  }
}

inline fun String.replaceDestructured(
  regex: Regex,
  crossinline transform: (group1: String, group2: String, group3: String) -> String
): String {
  return matchResultReplace(regex) { matchResult ->
    transform(
      matchResult.destructured.component1(),
      matchResult.destructured.component2(),
      matchResult.destructured.component3()
    )
  }
}

inline fun String.replaceDestructured(
  regex: Regex,
  crossinline transform: (group1: String, group2: String, group3: String, group4: String) -> String
): String {
  return matchResultReplace(regex) { matchResult ->
    transform(
      matchResult.destructured.component1(),
      matchResult.destructured.component2(),
      matchResult.destructured.component3(),
      matchResult.destructured.component4()
    )
  }
}

inline fun String.replaceDestructured(
  regex: Regex,
  crossinline transform: (
    group1: String,
    group2: String,
    group3: String,
    group4: String,
    group5: String
  ) -> String
): String {
  return matchResultReplace(regex) { matchResult ->
    transform(
      matchResult.destructured.component1(),
      matchResult.destructured.component2(),
      matchResult.destructured.component3(),
      matchResult.destructured.component4(),
      matchResult.destructured.component5()
    )
  }
}
