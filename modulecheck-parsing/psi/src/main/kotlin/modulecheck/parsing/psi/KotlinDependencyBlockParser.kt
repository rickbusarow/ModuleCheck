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

package modulecheck.parsing.psi

import modulecheck.parsing.MavenCoordinates
import org.jetbrains.kotlin.com.intellij.psi.PsiComment
import org.jetbrains.kotlin.com.intellij.psi.PsiWhiteSpace
import org.jetbrains.kotlin.psi.*
import org.jetbrains.kotlin.psi.psiUtil.findDescendantOfType
import org.jetbrains.kotlin.psi.psiUtil.getChildOfType

class KotlinDependencyBlockParser {

  @Suppress("ReturnCount")
  fun parse(file: KtFile): List<KotlinDependenciesBlock> {
    var blockWhiteSpace: String? = null

    val blocks = mutableListOf<KotlinDependenciesBlock>()

    val blockVisitor = blockExpressionRecursiveVisitor { expression ->

      val block = KotlinDependenciesBlock((blockWhiteSpace ?: "") + expression.text)

      expression
        .children
        .filterNot { it is PsiComment || it is PsiWhiteSpace }
        .filterIsInstance<KtCallExpression>()
        .forEach { callExpression ->

          val configName = callExpression.calleeExpression!!
            .text
            .replace("\"", "")

          val moduleName = callExpression.getStringModuleNameOrNull()
            ?: callExpression.getTypeSafeModuleNameOrNull()

          if (moduleName != null) {
            block.addModuleStatement(
              moduleRef = moduleName,
              configName = configName,
              parsedString = callExpression.text
            )
            return@forEach
          }

          val mavenCoordinates = callExpression.getMavenCoordinatesOrNull()

          if (mavenCoordinates != null) {
            block.addNonModuleStatement(configName, callExpression.text, mavenCoordinates)
            return@forEach
          }

          block.addUnknownStatement(configName, callExpression.text)
        }

      blocks.add(block)
    }

    val callVisitor = callExpressionRecursiveVisitor { expression ->
      if (expression.text.matches(""".*dependencies\s*\{[\s\S]*""".toRegex())) {
        expression.findDescendantOfType<KtBlockExpression>()?.let {
          blockWhiteSpace = (it.prevSibling as? PsiWhiteSpace)?.text?.trimStart('\n', '\r')
          blockVisitor.visitBlockExpression(it)
        }
      }
    }

    file.accept(callVisitor)

    return blocks
  }
}

internal fun KtCallExpression.getStringModuleNameOrNull(): String? {
  return this                                         // implementation(project(path = ":foo:bar"))
    .valueArguments                                   // [project(path = ":foo:bar")]
    .firstOrNull()                                    // project(path = ":foo:bar")
    ?.getChildOfType<KtCallExpression>()              // project(path = ":foo:bar")
    ?.valueArguments                                  // [path = ":foo:bar"]
    ?.firstOrNull()                                   // path = ":foo:bar"
    ?.getChildOfType<KtStringTemplateExpression>()    // ":foo:bar"
    ?.getChildOfType<KtLiteralStringTemplateEntry>()  // :foo:bar
    ?.text
}

internal fun KtCallExpression.getTypeSafeModuleNameOrNull(): String? {
  return this                                       // implementation(projects.foo.bar)
    .valueArguments                                 // [projects.foo.bar]
    .firstOrNull()                                  // projects.foo.bar
    ?.getChildOfType<KtDotQualifiedExpression>()    // projects.foo.bar
    ?.text
    ?.takeIf { it.startsWith("projects.") }
    ?.removePrefix("projects.")
}

@Suppress("MaxLineLength")
internal fun KtCallExpression.getMavenCoordinatesOrNull(): MavenCoordinates? {
  return this                                         // implementation(dependencyNotation = "com.google.dagger:dagger:2.32")
    .valueArguments                                   // [dependencyNotation = "com.google.dagger:dagger:2.32"]
    .firstOrNull()                                    // dependencyNotation = "com.google.dagger:dagger:2.32"
    ?.getChildOfType<KtStringTemplateExpression>()    // "com.google.dagger:dagger:2.32"
    ?.getChildOfType<KtLiteralStringTemplateEntry>()  // com.google.dagger:dagger:2.32
    ?.text                                            // com.google.dagger:dagger:2.32
    ?.let { MavenCoordinates.parseOrNull(it) }
}

inline fun blockExpressionRecursiveVisitor(
  crossinline block: KtTreeVisitorVoid.(expression: KtBlockExpression) -> Unit
) = object : KtTreeVisitorVoid() {
  override fun visitBlockExpression(expression: KtBlockExpression) {
    block(expression)
  }
}

inline fun literalStringTemplateRecursiveVisitor(
  crossinline block: KtTreeVisitorVoid.(entry: KtLiteralStringTemplateEntry) -> Unit
) = object : KtTreeVisitorVoid() {
  override fun visitLiteralStringTemplateEntry(entry: KtLiteralStringTemplateEntry) {
    super.visitLiteralStringTemplateEntry(entry)
    block(entry)
  }
}
