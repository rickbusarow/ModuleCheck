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

package modulecheck.psi

import modulecheck.api.Config
import org.jetbrains.kotlin.psi.KtCallExpression
import org.jetbrains.kotlin.psi.KtLiteralStringTemplateEntry
import org.jetbrains.kotlin.psi.KtStringTemplateExpression
import org.jetbrains.kotlin.psi.callExpressionVisitor
import org.jetbrains.kotlin.psi.psiUtil.getChildOfType
import org.jetbrains.kotlin.psi.psiUtil.referenceExpression

class ProjectDependencyDeclarationVisitor(
  private val configuration: Config,
  private val projectPath: String
) {

  @Suppress("ReturnCount")
  fun find(expression: KtCallExpression): Boolean {
    var found = false

    val configCallExpressionVisitor = callExpressionVisitor { innerExpression ->
      innerExpression
        .referenceExpression()
        .takeIf { it?.text == configuration.name }
        ?.let {
          innerExpression                                     // implementation(project(path = ":foo:bar"))
            .valueArguments                                   // [project(path = ":foo:bar")]
            .firstOrNull()                                    // project(path = ":foo:bar")
            ?.getChildOfType<KtCallExpression>()              // project(path = ":foo:bar")
            ?.valueArguments                                  // [path = ":foo:bar"]
            ?.firstOrNull()                                   // path = ":foo:bar"
            ?.getChildOfType<KtStringTemplateExpression>()    // ":foo:bar"
            ?.getChildOfType<KtLiteralStringTemplateEntry>()  // :foo:bar
            ?.let { projectNameArg ->

              if (projectNameArg.text == projectPath) {
                found = true
              }
            }
        }
    }

    configCallExpressionVisitor.visitCallExpression(expression)

    return found
  }
}
