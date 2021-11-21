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
import modulecheck.parsing.ModuleRef
import modulecheck.parsing.asConfigurationName
import org.jetbrains.kotlin.com.intellij.psi.PsiWhiteSpace
import org.jetbrains.kotlin.psi.KtAnnotatedExpression
import org.jetbrains.kotlin.psi.KtBlockExpression
import org.jetbrains.kotlin.psi.KtCallExpression
import org.jetbrains.kotlin.psi.KtDotQualifiedExpression
import org.jetbrains.kotlin.psi.KtFile
import org.jetbrains.kotlin.psi.KtLiteralStringTemplateEntry
import org.jetbrains.kotlin.psi.KtNameReferenceExpression
import org.jetbrains.kotlin.psi.KtStringTemplateExpression
import org.jetbrains.kotlin.psi.KtTreeVisitorVoid
import org.jetbrains.kotlin.psi.callExpressionRecursiveVisitor
import org.jetbrains.kotlin.psi.psiUtil.findDescendantOfType
import org.jetbrains.kotlin.psi.psiUtil.getChildOfType
import org.jetbrains.kotlin.psi.psiUtil.parents

class KotlinDependencyBlockParser {

  @Suppress("ReturnCount")
  fun parse(file: KtFile): List<KotlinDependenciesBlock> {
    var blockWhiteSpace: String? = null

    val blocks = mutableListOf<KotlinDependenciesBlock>()

    fun blockVisitor(
      blockSuppressed: List<String>
    ) = blockExpressionRecursiveVisitor { blockExpression ->

      val block = KotlinDependenciesBlock(
        contentString = (blockWhiteSpace ?: "") + blockExpression.text,
        suppressAll = blockSuppressed
      )

      blockExpression
        .children
        .forEach { element ->

          when (element) {
            is KtAnnotatedExpression -> {
              val suppressed = element.suppressedNames()

              element.getChildOfType<KtCallExpression>()?.parseStatements(block, suppressed)
            }
            is KtCallExpression -> {
              element.parseStatements(block, listOf())
            }
          }
        }

      blocks.add(block)
    }

    val callVisitor = callExpressionRecursiveVisitor { expression ->

      if (expression.getChildOfType<KtNameReferenceExpression>()?.text == "dependencies") {

        val blockSuppressed = (expression.parent as? KtAnnotatedExpression)
          ?.suppressedNames()
          .orEmpty()

        // recursively look for an enclosing KtCallExpression parent (`buildscript { ... }`)
        // then walk down to find its name reference (`buildscript`)
        val parentExpressionName = expression.parents
          .filterIsInstance<KtCallExpression>()
          .firstOrNull()
          ?.getChildOfType<KtNameReferenceExpression>()
          ?.text

        // skip the dependencies block inside buildscript
        if (parentExpressionName == "buildscript") {
          return@callExpressionRecursiveVisitor
        }

        expression.findDescendantOfType<KtBlockExpression>()?.let {
          blockWhiteSpace = (it.prevSibling as? PsiWhiteSpace)?.text?.trimStart('\n', '\r')
          blockVisitor(blockSuppressed).visitBlockExpression(it)
        }
      }
    }

    file.accept(callVisitor)

    return blocks
  }
}

private fun KtCallExpression.parseStatements(
  block: KotlinDependenciesBlock,
  suppressed: List<String>
) {
  val configName = calleeExpression!!
    .text
    .replace("\"", "")

  val moduleNamePair = getStringModuleNameOrNull()
    ?: getTypeSafeModuleNameOrNull()

  if (moduleNamePair != null) {

    val (moduleAccess, moduleRef) = moduleNamePair
    block.addModuleStatement(
      configName = configName.asConfigurationName(),
      parsedString = text,
      moduleRef = ModuleRef.from(moduleRef),
      moduleAccess = moduleAccess,
      suppressed = suppressed
    )
    return
  }

  val mavenCoordinates = getMavenCoordinatesOrNull()

  if (mavenCoordinates != null) {
    block.addNonModuleStatement(
      configName = configName.asConfigurationName(),
      parsedString = text,
      coordinates = mavenCoordinates,
      suppressed = suppressed
    )
    return
  }

  val testFixturesModuleNamePair = getStringTestFixturesModuleNameOrNull()
    ?: getTypeSafeTestFixturesModuleNameOrNull()

  if (testFixturesModuleNamePair != null) {

    val (moduleAccess, moduleRef) = testFixturesModuleNamePair

    block.addModuleStatement(
      configName = configName.asConfigurationName(),
      parsedString = text,
      moduleRef = ModuleRef.from(moduleRef),
      moduleAccess = moduleAccess,
      suppressed = suppressed
    )
    return
  }

  block.addUnknownStatement(
    configName = configName.asConfigurationName(),
    parsedString = text,
    argument = getUnknownArgumentOrNull() ?: "",
    suppressed = suppressed
  )
}

/* ktlint-disable no-multi-spaces */

internal fun KtCallExpression.getStringModuleNameOrNull(): Pair<String, String>? {
  return this                                             // implementation(project(path = ":foo:bar"))
    .valueArguments                                       // [project(path = ":foo:bar")]
    .firstOrNull()                                        // project(path = ":foo:bar")
    ?.getChildOfType<KtCallExpression>()                  // project(path = ":foo:bar")
    ?.let { moduleAccessCallExpression ->

      val moduleAccess = moduleAccessCallExpression.text

      moduleAccessCallExpression
        .valueArguments                                   // [path = ":foo:bar"]
        .firstOrNull()                                    // path = ":foo:bar"
        ?.getChildOfType<KtStringTemplateExpression>()    // ":foo:bar"
        ?.getChildOfType<KtLiteralStringTemplateEntry>()  // :foo:bar
        ?.text
        ?.let { moduleRef -> moduleAccess to moduleRef }
    }
}

internal fun KtCallExpression.getTypeSafeModuleNameOrNull(): Pair<String, String>? {
  return this                                       // implementation(projects.foo.bar)
    .valueArguments                                 // [projects.foo.bar]
    .firstOrNull()                                  // projects.foo.bar
    ?.getChildOfType<KtDotQualifiedExpression>()    // projects.foo.bar
    ?.let { moduleAccessCallExpression ->

      val moduleAccess = moduleAccessCallExpression.text

      moduleAccessCallExpression.text
        ?.takeIf { it.startsWith("projects.") }
        ?.removePrefix("projects.")
        ?.let { moduleRef -> moduleAccess to moduleRef }
    }
}

internal fun KtCallExpression.getStringTestFixturesModuleNameOrNull(): Pair<String, String>? {
  return this                                             // implementation(testFixtures(project(path = ":foo:bar")))
    .valueArguments                                       // [testFixtures(project(path = ":foo:bar"))]
    .firstOrNull()                                        // testFixtures(project(path = ":foo:bar"))
    ?.getChildOfType<KtCallExpression>()                  // testFixtures(project(path = ":foo:bar"))
    ?.valueArguments                                      // [project(path = ":foo:bar")]
    ?.firstOrNull()                                       // project(path = ":foo:bar")
    ?.getChildOfType<KtCallExpression>()                  // project(path = ":foo:bar")
    ?.let { moduleAccessCallExpression ->

      val moduleAccess = moduleAccessCallExpression.text

      moduleAccessCallExpression
        .valueArguments                                   // [path = ":foo:bar"]
        .firstOrNull()                                    // path = ":foo:bar"
        ?.getChildOfType<KtStringTemplateExpression>()    // ":foo:bar"
        ?.getChildOfType<KtLiteralStringTemplateEntry>()  // :foo:bar
        ?.text
        ?.let { moduleRef -> moduleAccess to moduleRef }
    }
}

internal fun KtCallExpression.getTypeSafeTestFixturesModuleNameOrNull(): Pair<String, String>? {
  return this                                       // implementation(testFixtures(projects.foo.bar))
    .valueArguments                                 // [testFixtures(projects.foo.bar)]
    .firstOrNull()                                  // testFixtures(projects.foo.bar)
    ?.getChildOfType<KtCallExpression>()            // testFixtures(projects.foo.bar)
    ?.valueArguments                                // [projects.foo.bar]
    ?.firstOrNull()                                 // projects.foo.bar
    ?.getChildOfType<KtDotQualifiedExpression>()    // projects.foo.bar
    ?.let { moduleAccessCallExpression ->

      val moduleAccess = moduleAccessCallExpression.text

      moduleAccessCallExpression
        .text
        ?.takeIf { it.startsWith("projects.") }
        ?.removePrefix("projects.")
        ?.let { moduleRef -> moduleAccess to moduleRef }
    }
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

@Suppress("MaxLineLength")
internal fun KtCallExpression.getUnknownArgumentOrNull(): String? {
  return this                                         // implementation(libs.ktlint)
    .valueArguments                                   // [libs.ktlint]
    .firstOrNull()                                    // libs.ktlint
    ?.text                                            // libs.ktlint
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

internal fun KtAnnotatedExpression.suppressedNames(): List<String> = annotationEntries
  .filter { it.typeReference?.text == "Suppress" || it.typeReference?.text == "SuppressWarnings" }
  .flatMap { it.valueArgumentList?.arguments.orEmpty() }
  .mapNotNull {
    it.getChildOfType<KtStringTemplateExpression>() // "Unused"
      ?.getChildOfType<KtLiteralStringTemplateEntry>() // Unused
      ?.text
  }
