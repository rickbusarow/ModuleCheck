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

import org.jetbrains.kotlin.psi.*
import org.jetbrains.kotlin.psi.psiUtil.getChildOfType

class AndroidBuildFeaturesVisitor {

  fun find(buildFile: KtFile, propertyName: String): PsiElementWithSurroundingText? {
    return buildFile.fullyQualifiedReferenceOrNull(propertyName)
      ?: buildFile.qualifiedScopedReferenceOrNull(propertyName)
      ?: buildFile.scopedQualifiedReferenceOrNull(propertyName)
      ?: buildFile.scopedScopedReferenceOrNull(propertyName)
  }

  private fun KtFile.fullyQualifiedReferenceOrNull(
    propertyName: String
  ): PsiElementWithSurroundingText? {
    val str = "$fullyQualifiedStart.$propertyName"
    var element: PsiElementWithSurroundingText? = null

    val visitor = dotQualifiedExpressionRecursiveVisitor vis@{ expression ->

      if (expression.textMatches(str)) {
        if (expression.parent is KtBinaryExpression) {
          element = PsiElementWithSurroundingText(expression.parent, "", "")
          return@vis
        }
      }
    }

    visitor.visitFile(this)

    return element
  }

  private fun KtFile.qualifiedScopedReferenceOrNull(
    propertyName: String
  ): PsiElementWithSurroundingText? {
    var element: PsiElementWithSurroundingText? = null

    val visitor = dotQualifiedExpressionRecursiveVisitor vis@{ expression ->

      if (expression.text.startsWith(fullyQualifiedStart)) {
        val binary = expression                 // android.buildFeatures { ... }
          .getChildOfType<KtCallExpression>()                   // buildFeatures { ... }
          ?.getChildOfType<KtLambdaArgument>()                  // { ... }
          ?.getChildOfType<KtLambdaExpression>()                // { ... }
          ?.getChildOfType<KtFunctionLiteral>()                 // { ... }
          ?.getChildOfType<KtBlockExpression>()                 // { ... }
          ?.children                                            // ["viewBinding = true", whitespace, etc.]
          ?.filterIsInstance<KtBinaryExpression>()              // ["viewBinding = true", "androidResources = true"]
          ?.find {
            it.text.trim()
              .startsWith(propertyName)
          }    // "viewBinding = true" as KtBinaryExpression

        if (binary != null) {
          element = PsiElementWithSurroundingText(expression, "", "")
          return@vis
        }
      }
    }

    visitor.visitFile(this)

    return element
  }

  private fun KtFile.scopedQualifiedReferenceOrNull(
    propertyName: String
  ): PsiElementWithSurroundingText? {
    var element: PsiElementWithSurroundingText? = null

    val callExpressionVisitor = callExpressionRecursiveVisitor { callExpression ->

      if (callExpression.text.matches("""android\s*\{[\s\S]*""".toRegex())) {
        val binaryExpressionVisitor = binaryExpressionRecursiveVisitor vis@{ binary ->

          if (binary.text.matches("""buildFeatures\s*\.\s*$propertyName[\s\S]*""".toRegex())) {
            element = PsiElementWithSurroundingText(callExpression, "", "")
            return@vis
          }
        }
        binaryExpressionVisitor.visitCallExpression(callExpression)
      }
    }

    callExpressionVisitor.visitFile(this)

    return element
  }

  private fun KtFile.scopedScopedReferenceOrNull(
    propertyName: String
  ): PsiElementWithSurroundingText? {
    var element: PsiElementWithSurroundingText? = null

    val ref1 = referenceExpressionRecursiveVisitor vis1@{ one ->

      if (one.text.matches("""android\s*""".toRegex())) {
        val ref2 = referenceExpressionRecursiveVisitor vis2@{ two ->

          if (two.text.matches("""buildFeatures\s*[\s\S]*""".toRegex())) {
            referenceExpressionRecursiveVisitor vis3@{ three ->

              if (three.text.matches("""$propertyName\s*""".toRegex())) {
                element = one.context?.let {
                  PsiElementWithSurroundingText(it, "", "")
                }

                if (element != null) {
                  return@vis3
                }
              }
            }.visitReferenceExpression(two)

            if (element != null) {
              return@vis2
            }
          }
        }

        (one.parent as? KtCallExpression)?.let { call ->
          ref2.visitCallExpression(call)
        }
      }
      if (element != null) {
        return@vis1
      }
    }

    ref1.visitFile(this)

    return element
  }

  private companion object {

    private const val fullyQualifiedStart = "android.buildFeatures"
  }
}
