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

package modulecheck.core.rule

import modulecheck.api.finding.Finding
import modulecheck.api.rule.ModuleCheckRule

sealed class DocumentedRule<T : Finding> : ModuleCheckRule<T> {

  /**
   * This is the part of the URL after `/rules/`. This should correspond to the relative path of
   * the file inside the `/website/docs/rules/` directory, except instead of the file name with
   * extension, use the `id` field from the header of the doc.
   *
   * For example, given a file `/website/docs/rules/compiler/ksp/foo.md` with an id of `id: bar`,
   * the documentation path is `compiler/ksp/bar`.
   */
  protected abstract val documentationPath: String

  final override val documentationUrl: String
    get() = "${RULES_BASE_URL}$documentationPath"

  companion object {
    const val RULES_BASE_URL = "https://rbusarow.github.io/ModuleCheck/docs/rules/"
  }
}
