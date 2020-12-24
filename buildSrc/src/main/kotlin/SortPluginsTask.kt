/*
 * Copyright (C) 2020 Rick Busarow
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

import org.gradle.api.DefaultTask
import org.gradle.api.tasks.TaskAction
import org.jetbrains.kotlin.com.intellij.psi.PsiComment
import org.jetbrains.kotlin.com.intellij.psi.PsiElement
import org.jetbrains.kotlin.com.intellij.psi.PsiWhiteSpace
import org.jetbrains.kotlin.psi.KtBlockExpression
import org.jetbrains.kotlin.psi.KtCallExpression
import org.jetbrains.kotlin.psi.KtTreeVisitorVoid
import org.jetbrains.kotlin.psi.psiUtil.findDescendantOfType
import java.util.*

abstract class SortPluginsTask : DefaultTask() {

  @TaskAction
  fun run() {

    project.allprojects.forEach { sub ->
      if (sub.buildFile.exists()) {

        val visitor = GradlePluginVisitor()
        sub
          .buildFile
          .asKtFile()
          .accept(visitor)

        val comparator = compareBy<PsiElementWithSurroundings>(
          { !it.psiElement.text.startsWith("""id("android""") },
          { !it.psiElement.text.startsWith("""id("java-library")""") },
          { it.psiElement.text != """kotlin("jvm")""" },
          { !it.psiElement.text.startsWith("android") },
          { it.psiElement.text != "javaLibrary" },
          { !it.psiElement.text.startsWith("kotlin") },
          { !it.psiElement.text.startsWith("id") },
          { it.psiElement.text }
        )

        val sorted =visitor
          .things
          .sortedWith(comparator)
          .joinToString("\n")
          .trim()

        val allText = sub.buildFile.readText()

        visitor.blockText?.let {
          val newText = allText.replace(it, sorted)

          sub.buildFile.writeText(newText)
        }
      }
    }
  }
}

class GradlePluginVisitor : KtTreeVisitorVoid() {

  val things = mutableListOf<PsiElementWithSurroundings>()
  var blockText: String? = null

  override fun visitCallExpression(expression: KtCallExpression) {

    if (expression.text.startsWith("plugins {")) {

      val visitor = PluginBlockDeclarationVisitor()

      expression.findDescendantOfType<KtBlockExpression>()?.let {
        visitor.visitBlockExpression(it)
      }
    }
  }

  inner class PluginBlockDeclarationVisitor : KtTreeVisitorVoid() {

    override fun visitBlockExpression(expression: KtBlockExpression) {

      blockText = expression.text

      val visited = mutableSetOf<PsiElement>()

      val elements = expression
        .children
        .filterNot { it is PsiComment || it is PsiWhiteSpace }
        .filterIsInstance<PsiElement>()
        .map { it.withSurroundings(visited) }

      things.addAll(elements)
    }
  }
/*
private fun parsePlugins(expression: KtCallExpression) {
    val declarations = expression.children
      .mapNotNull { it as? KtLambdaArgument }
      .flatMap { lambdaArg ->
        lambdaArg.children.mapNotNull { it as? KtLambdaExpression }
          .flatMap { lambdaExpression ->
            lambdaExpression.children.mapNotNull { it as? KtExpression }
              .flatMap { ktExpression ->
                ktExpression.children.mapNotNull { declarationExpression ->
                  declarationExpression as? KtExpression
                }
                  .flatMap { declarationExpression ->
                    declarationExpression.children.mapNotNull { it.text }
                  }
              }
          }
      }

    plugins = declarations

    val comparator = compareBy<String>(
      { it.startsWith("android").not() && it != "javaLibrary" },
      { it.startsWith("kotlin").not() },
      { it.startsWith("id").not() },
      { it }
    )

    newPlugins = """plugins {
      |  ${declarations.sortedWith(comparator).joinToString("\n") { "  $it" }.trim()}
      |}
    """.trimMargin()
  }
*/
}
