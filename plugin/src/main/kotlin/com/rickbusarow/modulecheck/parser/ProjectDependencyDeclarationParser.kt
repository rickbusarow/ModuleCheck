package com.rickbusarow.modulecheck.parser

import com.rickbusarow.modulecheck.Config
import org.jetbrains.kotlin.psi.KtCallExpression
import org.jetbrains.kotlin.psi.KtLiteralStringTemplateEntry
import org.jetbrains.kotlin.psi.KtStringTemplateExpression
import org.jetbrains.kotlin.psi.KtTreeVisitorVoid
import org.jetbrains.kotlin.psi.psiUtil.getChildOfType
import org.jetbrains.kotlin.psi.psiUtil.referenceExpression

class ProjectDependencyDeclarationParser(
  private val configuration: Config,
  private val projectPath: String
) {

  @Suppress("ReturnCount")
  fun parse(expression: KtCallExpression): Boolean {

    var found = false

    val configCallExpressionVisitor = object : KtTreeVisitorVoid() {
      override fun visitCallExpression(expression: KtCallExpression) {
        expression
          .referenceExpression()
          ?.let { referenceExpression ->

            if (referenceExpression.text == configuration.name) {
              expression                                          // implementation(project(path = ":foo:bar"))
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
      }
    }

    configCallExpressionVisitor.visitCallExpression(expression)

    return found
  }
}
