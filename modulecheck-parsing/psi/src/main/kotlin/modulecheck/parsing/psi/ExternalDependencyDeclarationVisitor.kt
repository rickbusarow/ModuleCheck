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

package modulecheck.parsing.psi

import org.jetbrains.kotlin.psi.KtCallExpression
import org.jetbrains.kotlin.psi.KtLiteralStringTemplateEntry
import org.jetbrains.kotlin.psi.KtStringTemplateExpression
import org.jetbrains.kotlin.psi.callExpressionVisitor
import org.jetbrains.kotlin.psi.psiUtil.getChildOfType
import org.jetbrains.kotlin.psi.psiUtil.referenceExpression

class ExternalDependencyDeclarationVisitor(
  private val configuration: String,
  /**
   * In "com.google.dagger:dagger:2.32", this would be "com.google.dagger"
   *
   * @since 0.12.0
   */
  group: String? = null,
  /**
   * In "com.google.dagger:dagger:2.32", this would be "dagger"
   *
   * @since 0.12.0
   */
  name: String? = null,
  /**
   * In "com.google.dagger:dagger:2.32", this would be "2.32"
   *
   * @since 0.12.0
   */
  version: String? = null
) {

  val projectMatcher = """${group.orWildcard()}:${name.orWildcard()}:${version.orWildcard()}"""
    .replace(".", "\\.")
    .toRegex()

  private fun String?.orWildcard() = this ?: "*"

  @Suppress("ReturnCount", "MaxLineLength")
  fun find(expression: KtCallExpression): Boolean {
    var found = false

    val configCallExpressionVisitor = callExpressionVisitor { innerExpression ->
      innerExpression
        .referenceExpression()
        .takeIf { it?.text == configuration }
        ?.let {
          /* ktlint-disable no-multi-spaces */
          innerExpression                                     // implementation(dependencyNotation = "com.google.dagger:dagger:2.32")
            .valueArguments                                   // [dependencyNotation = "com.google.dagger:dagger:2.32"]
            .firstOrNull()                                    // dependencyNotation = "com.google.dagger:dagger:2.32"
            ?.getChildOfType<KtStringTemplateExpression>()    // "com.google.dagger:dagger:2.32"
            ?.getChildOfType<KtLiteralStringTemplateEntry>()  // com.google.dagger:dagger:2.32
            ?.text                                            // com.google.dagger:dagger:2.32
            ?.let { groupName ->

              if (groupName.matches(projectMatcher)) {
                found = true
              }
            }
          /* ktlint-enable no-multi-spaces */
        }
    }

    configCallExpressionVisitor.visitCallExpression(expression)

    return found
  }
}
