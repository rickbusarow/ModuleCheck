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
import org.jetbrains.kotlin.com.intellij.psi.PsiElement
import org.jetbrains.kotlin.com.intellij.psi.PsiFileFactory
import org.jetbrains.kotlin.com.intellij.psi.PsiWhiteSpace
import org.jetbrains.kotlin.config.CompilerConfiguration
import org.jetbrains.kotlin.idea.KotlinLanguage
import org.jetbrains.kotlin.psi.*
import org.jetbrains.kotlin.psi.psiUtil.getChildOfType
import java.io.File
import java.io.FileNotFoundException

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
      println(sub.buildFile)
      if (sub.buildFile.exists()) {
        println("----")
        sub
          .buildFile
          .asKtFile()
          .accept(GradleDependencyVisitor())
      }
    }
  }
}

class GradleDependencyVisitor : KtTreeVisitorVoid() {

  lateinit var root: KtFile
  lateinit var oldDependenciesText: String
  lateinit var dependencies: List<String>
  lateinit var plugins: List<String>
  lateinit var newDependencies: String
  lateinit var newPlugins: String
  lateinit var oldPluginsText: String

  fun PsiElement.blockChildOrNull(): KtBlockExpression? {

    if (this is KtBlockExpression) return this

    val blockChild = children.filterIsInstance<KtBlockExpression>().firstOrNull()

    if (blockChild != null)
      return blockChild

    return children.flatMap { gc ->
      gc.children.toList()
    }.firstOrNull { it.blockChildOrNull() != null } as? KtBlockExpression
  }

  fun PsiElement.nextNonWhitespace(): PsiElement? =
    if (this !is PsiWhiteSpace) {
      this
    } else {
      nextSibling?.nextNonWhitespace()
    }

  override fun visitCallExpression(expression: KtCallExpression) {

    if (expression.text.startsWith("dependencies {")) {

      val visitor = DependencyBlockDeclarationVisitor()

      expression.lastChild.lastChild.lastChild.getChildOfType<KtBlockExpression>()?.let {
        visitor.visitBlockExpression(it)
      }
    }
  }


  class DependencyBlockDeclarationVisitor : KtTreeVisitorVoid() {

    override fun visitBlockExpression(expression: KtBlockExpression) {

      println(
        """block ---> 
      |${expression.text}""".trimMargin()
      )

      super.visitBlockExpression(expression)
    }

  }

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
}
