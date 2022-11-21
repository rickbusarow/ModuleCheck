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

package modulecheck.rule.impl

import modulecheck.finding.Finding
import modulecheck.rule.ModuleCheckRule

sealed class DocumentedRule<T : Finding> : ModuleCheckRule<T> {

  /**
   * This should correspond to the finding name in `snake_case`. So a rule with a `findingName.id`
   * of 'unused-dependency' would have a documentation url of `<docs root>/rules/unused_dependency`.
   *
   * @since 0.12.0
   */
  final override val documentationUrl: String
    get() = "$RULES_BASE_URL${name.snakeCase}"

  override fun toString(): String = "${this::class.simpleName}(${name.id})"

  companion object {
    const val RULES_BASE_URL: String = "https://rbusarow.github.io/ModuleCheck/docs/rules/"
  }
}
