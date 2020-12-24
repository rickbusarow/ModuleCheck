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
import org.jetbrains.kotlin.cli.common.CLIConfigurationKeys
import org.jetbrains.kotlin.cli.common.messages.MessageRenderer
import org.jetbrains.kotlin.cli.common.messages.PrintingMessageCollector
import org.jetbrains.kotlin.cli.jvm.compiler.EnvironmentConfigFiles
import org.jetbrains.kotlin.cli.jvm.compiler.KotlinCoreEnvironment
import org.jetbrains.kotlin.com.intellij.openapi.util.Disposer
import org.jetbrains.kotlin.com.intellij.openapi.util.Key
import org.jetbrains.kotlin.com.intellij.psi.PsiComment
import org.jetbrains.kotlin.com.intellij.psi.PsiElement
import org.jetbrains.kotlin.com.intellij.psi.PsiFileFactory
import org.jetbrains.kotlin.com.intellij.psi.PsiWhiteSpace
import org.jetbrains.kotlin.config.CompilerConfiguration
import org.jetbrains.kotlin.idea.KotlinLanguage
import org.jetbrains.kotlin.psi.*
import org.jetbrains.kotlin.psi.psiUtil.findDescendantOfType
import java.io.File
import java.io.FileNotFoundException
import java.util.*

val ABSOLUTE_PATH: Key<String> = Key("absolutePath")
val configuration = CompilerConfiguration().apply {
  put(
    CLIConfigurationKeys.MESSAGE_COLLECTOR_KEY,
    PrintingMessageCollector(
      System.err,
      MessageRenderer.PLAIN_FULL_PATHS,
      false
    )
  )
}

private val psiProject by lazy {
  KotlinCoreEnvironment.createForProduction(
    Disposer.newDisposable(),
    configuration,
    EnvironmentConfigFiles.JVM_CONFIG_FILES
  ).project
}
val psiFileFactory: PsiFileFactory = PsiFileFactory.getInstance(psiProject)
fun File.asKtFile(): KtFile =
  (psiFileFactory.createFileFromText(name, KotlinLanguage.INSTANCE, readText()) as? KtFile)?.apply {
    putUserData(ABSOLUTE_PATH, this@asKtFile.absolutePath)
  } ?: throw FileNotFoundException("could not find file $this")

abstract class SortDependenciesTask : DefaultTask() {

  @TaskAction
  fun run() {

    project.subprojects.forEach { sub ->
      if (sub.buildFile.exists()) {

        val visitor = GradleDependencyVisitor()
        sub
          .buildFile
          .asKtFile()
          .accept(visitor)

        val sorted = visitor
          .things
          .grouped()
          .joinToString("\n\n") { list ->
            list
              .sortedBy { psiElementWithSurroundings ->
                psiElementWithSurroundings
                  .psiElement
                  .text
                  .toLowerCase(Locale.US)
              }
              .joinToString("\n")
          }
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

class GradleDependencyVisitor : KtTreeVisitorVoid() {

  val things = mutableListOf<PsiElementWithSurroundings>()
  var blockText: String? = null

  override fun visitCallExpression(expression: KtCallExpression) {

    if (expression.text.startsWith("dependencies {")) {

      val visitor = DependencyBlockDeclarationVisitor()

      expression.findDescendantOfType<KtBlockExpression>()?.let {
        visitor.visitBlockExpression(it)
      }
    }
  }

  inner class DependencyBlockDeclarationVisitor : KtTreeVisitorVoid() {

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
  private fun parseDependencies(expression: KtCallExpression) {
    dependencies = expression.children
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
                    declarationExpression.children.mapNotNull { it as? KtCallExpression }
                      .map { it.text }
                  }
              }
          }
      }

    val declarations = dependencies.groupBy {
      it.split("[(.]".toRegex()).take(2).joinToString("-")
    }

    val comparator = compareBy<String>({ it.startsWith("kapt") }, { it })

    val sortedKeys = declarations.keys.sortedWith(comparator)

    val newDeps = sortedKeys.joinToString("\n\n") { key ->

      declarations.getValue(key)
        .toSet()
        .sortedBy {
          @Suppress("DefaultLocale")
          it.toLowerCase()
        }
        .joinToString("\n") { "  $it" }
    }.trim()

    newDependencies = if (newDeps.isNotEmpty()) {
      """dependencies {
          |
          |  $newDeps
          |}
        """.trimMargin()
    } else {
      oldDependenciesText
    }
  }

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

fun List<PsiElementWithSurroundings>.grouped() = groupBy {
  it.psiElement.text.split("[(.]".toRegex()).take(2).joinToString("-")
}.toSortedMap(compareBy { it.toLowerCase(Locale.US) })
  .map { it.value }

data class PsiElementWithSurroundings(
  val psiElement: PsiElement,
  val previousText: String,
  val nextText: String
) {
  override fun toString(): String {
    return previousText + psiElement.text + nextText
  }
}

fun PsiElement.withSurroundings(visited: MutableSet<PsiElement>): PsiElementWithSurroundings {

  var previous: PsiElement? = prevSibling

  val prevStrings = mutableListOf<String>()

  while (previous !in visited && (previous is PsiWhiteSpace || previous is PsiComment)) {

    visited.add(previous)

    prevStrings.add(previous.text)
    previous = previous.prevSibling
  }

  val previousText = prevStrings
    .reversed()
    .joinToString("")

  var next: PsiElement? = nextSibling

  var nextText = ""

  while (next is PsiWhiteSpace || next is PsiComment) {

    if ((text + nextText + next.text).lines().size == 1) {
      visited.add(next)
      nextText += next.text
      next = next.nextSibling
    } else {
      break
    }
  }

  return PsiElementWithSurroundings(this, previousText.trimStart('\n', '\r'), nextText.trimEnd())


}
