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

package modulecheck.builds.ktlint

import com.pinterest.ktlint.core.LintError
import com.pinterest.ktlint.core.RuleProvider
import com.pinterest.ktlint.core.api.EditorConfigOverride
import com.pinterest.ktlint.test.format as ktlintTestFormat

fun Set<RuleProvider>.format(
  text: String,
  editorConfigOverride: EditorConfigOverride = EditorConfigOverride.emptyEditorConfigOverride,
  userData: Map<String, String> = emptyMap(),
  cb: (e: LintError, corrected: Boolean) -> Unit = { _, _ -> },
  script: Boolean = false
): String = ktlintTestFormat(
  lintedFilePath = null,
  text = text,
  editorConfigOverride = editorConfigOverride,
  userData = userData,
  cb = cb,
  script = script
)
