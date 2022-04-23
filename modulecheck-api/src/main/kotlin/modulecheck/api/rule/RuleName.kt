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

package modulecheck.api.rule

import modulecheck.utils.CaseMatcher
import modulecheck.utils.capitalize

class RuleName(
  /** some-rule-name */
  val kebabCase: String
) {
  init {
    check(CaseMatcher.KebabCaseMatcher().matches(kebabCase))
  }

  /** SomeRuleName */
  val titleCase: String get() = kebabCase.split('-').joinToString { it.capitalize() }

  /** some_rule_name */
  val snakeCase: String get() = kebabCase.replace('-', '_')

  /** someRuleName */
  val pascalCase: String

  /** 'Some Rule Name' */
  val words: String
}
