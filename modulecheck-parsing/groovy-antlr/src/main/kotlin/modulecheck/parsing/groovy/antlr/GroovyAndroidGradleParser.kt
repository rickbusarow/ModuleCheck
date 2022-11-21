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

package modulecheck.parsing.groovy.antlr

import groovyjarjarantlr4.v4.runtime.ParserRuleContext
import modulecheck.parsing.gradle.dsl.AndroidGradleParser
import modulecheck.parsing.gradle.dsl.AndroidGradleSettings
import modulecheck.parsing.gradle.dsl.AndroidGradleSettings.AgpBlock.AndroidBlock
import modulecheck.parsing.gradle.dsl.AndroidGradleSettings.AgpBlock.BuildFeaturesBlock
import modulecheck.parsing.gradle.dsl.Assignment
import modulecheck.utils.prefixIfNot
import modulecheck.utils.requireNotNull
import org.apache.groovy.parser.antlr4.GroovyParser.AssignmentExprAltContext
import org.apache.groovy.parser.antlr4.GroovyParser.ClosureOrLambdaExpressionContext
import org.apache.groovy.parser.antlr4.GroovyParser.NamePartContext
import org.apache.groovy.parser.antlr4.GroovyParser.PathElementContext
import org.apache.groovy.parser.antlr4.GroovyParser.PathExpressionContext
import org.apache.groovy.parser.antlr4.GroovyParser.PostfixExpressionContext
import org.apache.groovy.parser.antlr4.GroovyParserBaseVisitor
import java.io.File
import javax.inject.Inject

class GroovyAndroidGradleParser @Inject constructor() : AndroidGradleParser {

  override suspend fun parse(buildFile: File): AndroidGradleSettings = parse(buildFile) {

    val androidBlocks = mutableListOf<AndroidBlock>()
    val buildFeaturesBlocks = mutableListOf<BuildFeaturesBlock>()

    val allAssignments = mutableListOf<Assignment>()
    val buildFeaturesAssignments = mutableListOf<Assignment>()

    class AssignmentVisitor(
      private val fullText: String,
      private val blockSuppressed: List<String>
    ) : GroovyParserBaseVisitor<Unit>() {

      val assignments = mutableListOf<Assignment>()

      override fun visitAssignmentExprAlt(assignmentExpression: AssignmentExprAltContext) {
        super.visitAssignmentExprAlt(assignmentExpression)

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

        val allSuppressed = blockSuppressed.plus(
          assignmentExpression.precedingCommentNodeOrNull()
            ?.originalText()
            ?.suppressions()
            .orEmpty()
        )
          .distinct()

        assignments.add(
          Assignment(
            fullText = fullText,
            propertyFullName = propertyName,
            value = valueText,
            declarationText = assignmentText,
            suppressed = allSuppressed
          )
        )
      }
    }

    val visitor = commandExpressionVisitor(true) { ctx ->

      val android = ctx.takeIf { it.isNamed("android") }
        ?: return@commandExpressionVisitor

      val precedingCommentOrEmpty = android.precedingCommentNodeOrNull()?.originalText().orEmpty()

      val androidSuppressed = precedingCommentOrEmpty.suppressions()

      val androidLambda = android.lambdaBlock()

      val androidIsBlock = androidLambda != null

      val androidBlockVisitor = AssignmentVisitor(
        ctx.originalText().prefixIfNot(precedingCommentOrEmpty).trim(),
        androidSuppressed
      )

      android.accept(androidBlockVisitor)

      allAssignments += androidBlockVisitor.assignments

      if (androidIsBlock) {

        val lambdaContent = androidLambda?.closureContent()
          ?.originalText()
          .requireNotNull()

        androidBlocks.add(
          AndroidBlock(
            fullText = android.originalText(),
            lambdaContent = lambdaContent,
            settings = androidBlockVisitor.assignments,
            blockSuppressed = androidSuppressed
          )
        )

        android.accept(
          commandExpressionVisitor { buildFeatures ->
            if (buildFeatures.isNamed("buildFeatures")) {

              val blockSuppressed = androidSuppressed.plus(
                buildFeatures.precedingCommentNodeOrNull()
                  ?.originalText()
                  ?.suppressions()
                  .orEmpty()
              )
                .distinct()

              val blockStatementVisitor = AssignmentVisitor(ctx.originalText(), blockSuppressed)

              buildFeatures.accept(blockStatementVisitor)

              buildFeaturesAssignments += blockStatementVisitor.assignments

              buildFeatures.lambdaBlock()
                ?.closureContent()
                ?.originalText()
                ?.let { buildFeaturesLambda ->

                  buildFeaturesBlocks.add(
                    BuildFeaturesBlock(
                      fullText = android.originalText(),
                      lambdaContent = buildFeaturesLambda,
                      settings = blockStatementVisitor.assignments,
                      blockSuppressed = blockSuppressed
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

              val blockSuppressed = androidSuppressed.plus(
                buildFeatures.precedingCommentNodeOrNull()
                  ?.originalText()
                  ?.suppressions()
                  .orEmpty()
              )
                .distinct()

              val blockStatementVisitor = AssignmentVisitor(ctx.originalText(), blockSuppressed)

              buildFeatures.accept(blockStatementVisitor)

              buildFeaturesAssignments += blockStatementVisitor.assignments

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
                    settings = blockStatementVisitor.assignments,
                    blockSuppressed = blockSuppressed
                  )
                )
              }
            }
          }
        )
      }
    }

    parser.accept(visitor)

    return AndroidGradleSettings(
      assignments = allAssignments,
      androidBlocks = androidBlocks,
      buildFeaturesBlocks = buildFeaturesBlocks
    )
  }
}
