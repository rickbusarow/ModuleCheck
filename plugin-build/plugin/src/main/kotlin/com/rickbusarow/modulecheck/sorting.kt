package com.rickbusarow.modulecheck

import com.rickbusarow.modulecheck.internal.asKtFile
import org.gradle.api.DefaultTask
import org.gradle.api.tasks.TaskAction
import org.jetbrains.kotlin.psi.*

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

  override fun visitCallExpression(expression: KtCallExpression) {

    if (expression.text.startsWith("dependencies {")) {

      val visitor = DependencyDeclarationVisitor()


      visitor.visitElement(expression)

//      expression.children.forEach {
//        println(
//          """child -------------------- ${it}
//      |
//      |${it.text}
//    """.trimMargin()
//        )
//
//        it.children.forEach {
//          println(
//            """grand-child ------------------- $it
//        |
//        |${it.text}
//      """.trimMargin()
//          )
//
//          it.children.forEach {
//            println(
//              """great-grand-child ------------------- $it
//        |
//        |${it.text}
//      """.trimMargin()
//            )
//          }
//
//          it.children.forEach {
//            println(
//              """great-great-grand-child ------------------- $it
//        |
//        |${it.text}
//      """.trimMargin()
//            )
//          }
//
//          it.children.forEach {
//            println(
//              """great-great-great-grand-child ------------------- $it
//        |
//        |${it.text}
//      """.trimMargin()
//            )
//          }
//        }
//      }
    }
  }

//  override fun visitLambdaExpression(lambdaExpression: KtLambdaExpression) {
//
//    println(
//      """lambda expression ---------------------------
//      |
//      |${lambdaExpression.text}
//      |
//      |--------------------------------
//    """.trimMargin()
//    )
//
//
//  }
//
//  override fun visitReferenceExpression(expression: KtReferenceExpression) {
//
//    if (expression.text.startsWith("dependencies {")) {
//      val visitor = DependencyDeclarationVisitor()
//      expression.children
//        .filterIsInstance<KtReferenceExpression>()
//        .forEach { visitor.visitReferenceExpression(it) }
//
////      println(
////        """reference expression ***
////    |
////    |${expression.text}
////    |
////    |^^^^
////  """.trimMargin()
////      )
//
//      super.visitReferenceExpression(expression)
//    }
//  }

  class DependencyDeclarationVisitor : KtTreeVisitorVoid() {

    override fun visitCallExpression(expression: KtCallExpression) {

      println("""call expression ----------------------
        |
        |${expression.text}
        |${expression.parent.text}
        |
        |==========================================
      """.trimMargin())


    }

    override fun visitReferenceExpression(expression: KtReferenceExpression) {

//      println(
//        """visited expression ***
//      |
//      |${expression.text}
//      |
//      |^^^^^^^^^^^^^^^^
//    """.trimMargin()
//      )


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
