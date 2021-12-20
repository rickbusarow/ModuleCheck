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

package modulecheck.parsing.groovy.antlr

import groovyjarjarantlr4.v4.runtime.ParserRuleContext
import modulecheck.parsing.gradle.AgpBlock.AndroidBlock
import modulecheck.parsing.gradle.AgpBlock.BuildFeaturesBlock
import modulecheck.parsing.gradle.AndroidGradleParser
import modulecheck.parsing.gradle.AndroidGradleSettings
import modulecheck.parsing.gradle.Assignment
import modulecheck.utils.requireNotNull
import org.apache.groovy.parser.antlr4.GroovyParser.AssignmentExprAltContext
import org.apache.groovy.parser.antlr4.GroovyParser.ClosureOrLambdaExpressionContext
import org.apache.groovy.parser.antlr4.GroovyParser.NamePartContext
import org.apache.groovy.parser.antlr4.GroovyParser.PathElementContext
import org.apache.groovy.parser.antlr4.GroovyParser.PathExpressionContext
import org.apache.groovy.parser.antlr4.GroovyParser.PostfixExpressionContext
import java.io.File
import javax.inject.Inject

class GroovyAndroidGradleParser @Inject constructor() : AndroidGradleParser {

  override fun parse(buildFile: File): AndroidGradleSettings = parse(buildFile) {

    val androidBlocks = mutableListOf<AndroidBlock>()
    val buildFeaturesBlocks = mutableListOf<BuildFeaturesBlock>()

    val allAssignments = mutableListOf<Assignment>()
    val buildFeaturesAssignments = mutableListOf<Assignment>()

    fun List<AssignmentExprAltContext>.mapAssignments(fullText: String) =
      map { assignmentExpression ->

        val assignmentText = assignmentExpression.originalText()

        val propertyName = assignmentExpression.left.childOfType<PostfixExpressionContext>()
          ?.childOfType<PathExpressionContext>()
          ?.childrenOfType<PathElementContext>()
          ?.lastOrNull()
          ?.childOfType<NamePartContext>()
          ?.originalText()
          ?: assignmentExpression.left.originalText()

        val valueText = assignmentExpression.children
          .filterIsInstance<ParserRuleContext>()
          .last()
          .originalText()

        Assignment(
          fullText = fullText,
          propertyFullName = propertyName,
          value = valueText,
          declarationText = assignmentText
        )
      }

    val commandVisitor = commandExpressionVisitor(true) { ctx ->

      val android = ctx.takeIf { it.isNamed("android") }
        ?: return@commandExpressionVisitor

      val androidLambda = android.lambdaBlock()

      val androidIsBlock = androidLambda != null

      val androidAssignments = android.childrenOfTypeRecursive<AssignmentExprAltContext>()
        .mapAssignments(ctx.originalText())

      allAssignments += androidAssignments

      if (androidIsBlock) {

        val lambdaContent = androidLambda?.closureContent()
          ?.originalText()
          .requireNotNull()

        androidBlocks.add(AndroidBlock(android.originalText(), lambdaContent, androidAssignments))

        android.accept(
          commandExpressionVisitor { buildFeatures ->
            if (buildFeatures.isNamed("buildFeatures")) {

              val assignments = buildFeatures.childrenOfTypeRecursive<AssignmentExprAltContext>()
                .mapAssignments(ctx.originalText())

              buildFeaturesAssignments += assignments

              buildFeatures.lambdaBlock()
                ?.closureContent()
                ?.originalText()
                ?.let { buildFeaturesLambda ->

                  buildFeaturesBlocks.add(
                    BuildFeaturesBlock(
                      fullText = android.originalText(),
                      lambdaContent = buildFeaturesLambda,
                      settings = assignments
                    )
                  )
                }
            }
          }
        )
      } else {
        android.accept(
          pathExpressionVisitor { buildFeatures ->

            if (buildFeatures.isNamed("buildFeatures")) {
              val assignments = buildFeatures.childrenOfTypeRecursive<AssignmentExprAltContext>()
                .mapAssignments(ctx.originalText())

              buildFeaturesAssignments += assignments

              val buildFeaturesLambdaContent = buildFeatures.pathElement()
                ?.lastOrNull()
                ?.childOfType<ClosureOrLambdaExpressionContext>()
                ?.closureContent()
                ?.originalText()

              if (buildFeaturesLambdaContent != null) {
                buildFeaturesBlocks.add(
                  BuildFeaturesBlock(
                    fullText = android.originalText(),
                    lambdaContent = buildFeaturesLambdaContent,
                    settings = assignments
                  )
                )
              }
            }
          }
        )
      }
    }

    parser.accept(commandVisitor)

    return AndroidGradleSettings(
      assignments = allAssignments,
      androidBlocks = androidBlocks,
      buildFeaturesBlocks = buildFeaturesBlocks
    )
  }
}
