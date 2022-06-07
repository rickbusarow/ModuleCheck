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

package modulecheck.parsing.psi

import modulecheck.parsing.gradle.dsl.AndroidGradleParser
import modulecheck.parsing.gradle.dsl.AndroidGradleSettings
import modulecheck.parsing.gradle.dsl.AndroidGradleSettings.AgpBlock.AndroidBlock
import modulecheck.parsing.gradle.dsl.AndroidGradleSettings.AgpBlock.BuildFeaturesBlock
import modulecheck.parsing.gradle.dsl.Assignment
import modulecheck.parsing.psi.internal.asKtFile
import modulecheck.parsing.psi.internal.getChildrenOfTypeRecursive
import org.jetbrains.kotlin.com.intellij.psi.PsiElement
import org.jetbrains.kotlin.psi.KtAnnotatedExpression
import org.jetbrains.kotlin.psi.KtBinaryExpression
import org.jetbrains.kotlin.psi.KtBlockExpression
import org.jetbrains.kotlin.psi.KtConstantExpression
import org.jetbrains.kotlin.psi.KtDotQualifiedExpression
import org.jetbrains.kotlin.psi.KtExpression
import org.jetbrains.kotlin.psi.KtNameReferenceExpression
import org.jetbrains.kotlin.psi.psiUtil.findDescendantOfType
import org.jetbrains.kotlin.psi.psiUtil.getChildOfType
import org.jetbrains.kotlin.psi.psiUtil.getChildrenOfType
import org.jetbrains.kotlin.psi.psiUtil.parents
import java.io.File
import javax.inject.Inject

class KotlinAndroidGradleParser @Inject constructor() : AndroidGradleParser {

  override fun parse(buildFile: File): AndroidGradleSettings {

    val file = buildFile.asKtFile()

    val androidQualifiedSettings = file.getChildrenOfTypeRecursive<KtBinaryExpression>()
      .filter { it.findDescendantOfType<KtNameReferenceExpression>()?.text == "android" }
      .mapNotNull { it.toAssignmentOrNull(it.text, it.suppressedNames()) }

    val androidQualified = file.getChildrenOfTypeRecursive<KtDotQualifiedExpression>()
      .filter { it.getChildOfType<KtNameReferenceExpression>()?.text == "android" }

    val androidBlocksPsi = file.androidBlocks()

    val androidBlocks = androidBlocksPsi
      .mapNotNull { androidBlock ->

        val blockSuppressed = androidBlock.suppressedNames()

        val fullText = androidBlock.text

        val block = androidBlock.getChildrenOfTypeRecursive<KtBlockExpression>()
          .firstOrNull()

        val content = block?.text ?: return@mapNotNull null

        val settings = androidBlock.mapAssignments(fullText, blockSuppressed)

        AndroidBlock(fullText, content, settings, blockSuppressed)
      }

    val buildFeaturesBlocks = (androidBlocksPsi + androidQualified)
      .flatMap { android: KtExpression ->

        android.buildFeaturesBlocks()
          .mapNotNull { buildFeaturesBlock ->

            val blockSuppressed = buildFeaturesBlock.suppressedNames()

            val fullText = android.text

            val contentBlock = buildFeaturesBlock.getChildrenOfTypeRecursive<KtBlockExpression>()
              .firstOrNull()
              ?: return@mapNotNull null

            val content = contentBlock.text

            val allSuppressed = buildFeaturesBlock.parents.filterIsInstance<KtAnnotatedExpression>()
              .flatMap { it.suppressedNames() }
              .toList()

            val settings = contentBlock.mapAssignments(fullText, allSuppressed)

            BuildFeaturesBlock(fullText, content, settings, blockSuppressed)
          }
      }

    val allAssignments = (androidBlocks + buildFeaturesBlocks)
      .flatMap { it.settings }
      .plus(androidQualifiedSettings)
      .distinct()

    return AndroidGradleSettings(
      assignments = allAssignments,
      androidBlocks = androidBlocks,
      buildFeaturesBlocks = buildFeaturesBlocks
    )
  }

  private fun PsiElement.mapAssignments(
    fullText: String,
    blockSuppressed: List<String>
  ): List<Assignment> {
    return getChildrenOfTypeRecursive<KtBinaryExpression>()
      .mapNotNull { it.toAssignmentOrNull(fullText, blockSuppressed) }
  }

  private fun KtBinaryExpression.toAssignmentOrNull(
    fullText: String,
    blockSuppressed: List<String>
  ): Assignment? {

    val suppressed = blockSuppressed.plus(suppressedNames()).distinct()

    val propertyFullName = getChildOfType<KtNameReferenceExpression>()?.text
      ?: getChildOfType<KtDotQualifiedExpression>()
        ?.getChildrenOfType<KtNameReferenceExpression>()
        ?.lastOrNull()
        ?.text
      ?: return null
    val value = getChildOfType<KtConstantExpression>()?.text ?: return null

    return Assignment(
      fullText = fullText,
      propertyFullName = propertyFullName,
      value = value,
      declarationText = text,
      suppressed = suppressed
    )
  }
}
