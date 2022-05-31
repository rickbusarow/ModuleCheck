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

import modulecheck.model.dependency.ProjectDependency
import modulecheck.parsing.gradle.dsl.InvokesConfigurationNames
import modulecheck.parsing.gradle.dsl.ProjectAccessor
import modulecheck.parsing.gradle.dsl.buildFileInvocationText
import modulecheck.parsing.gradle.model.MavenCoordinates
import modulecheck.parsing.gradle.model.ProjectPath
import modulecheck.parsing.gradle.model.asConfigurationName
import modulecheck.parsing.psi.internal.asKtFile
import modulecheck.parsing.psi.internal.getChildrenOfTypeRecursive
import modulecheck.parsing.psi.internal.nameSafe
import modulecheck.reporting.logging.McLogger
import org.jetbrains.kotlin.com.intellij.psi.PsiWhiteSpace
import org.jetbrains.kotlin.psi.KtAnnotatedExpression
import org.jetbrains.kotlin.psi.KtBlockExpression
import org.jetbrains.kotlin.psi.KtCallExpression
import org.jetbrains.kotlin.psi.KtDotQualifiedExpression
import org.jetbrains.kotlin.psi.KtLiteralStringTemplateEntry
import org.jetbrains.kotlin.psi.KtStringTemplateExpression
import org.jetbrains.kotlin.psi.KtTreeVisitorVoid
import org.jetbrains.kotlin.psi.psiUtil.findDescendantOfType
import org.jetbrains.kotlin.psi.psiUtil.getChildOfType
import javax.inject.Inject

class KotlinDependenciesBlockParser @Inject constructor(
  private val logger: McLogger,
  private val projectDependency: ProjectDependency.Factory
) {

  @Suppress("ReturnCount")
  suspend fun parse(
    invokesConfigurationNames: InvokesConfigurationNames
  ): List<KotlinDependenciesBlock> {

    val file = invokesConfigurationNames.buildFile.asKtFile()

    val blocks = file.getChildrenOfTypeRecursive<KtCallExpression>()
      .filter { it.nameSafe() == "dependencies" }
      .filterNot { it.inBuildscript() }
      .mapNotNull { fullBlock ->

        val blockSuppressed = (fullBlock.parent as? KtAnnotatedExpression)
          ?.suppressedNames()
          .orEmpty()

        val fullText = fullBlock.text

        val contentBlock = fullBlock.findDescendantOfType<KtBlockExpression>()
          ?: return@mapNotNull null

        val contentString = contentBlock.text

        val blockWhiteSpace = (contentBlock.prevSibling as? PsiWhiteSpace)?.text
          ?.trimStart('\n', '\r')
          ?: ""

        val block = KotlinDependenciesBlock(
          logger = logger,
          fullText = fullText,
          lambdaContent = blockWhiteSpace + contentString,
          suppressAll = blockSuppressed,
          configurationNameTransform = { it.buildFileInvocationText(invokesConfigurationNames) },
          projectDependency = projectDependency
        )

        contentBlock.children
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

        block
      }

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

    val (projectAccessor, moduleRef) = moduleNamePair
    val projectPath = ProjectPath.from(moduleRef)
    val accessor = ProjectAccessor.from(projectAccessor, projectPath)
    block.addModuleStatement(
      configName = configName.asConfigurationName(),
      parsedString = text,
      projectPath = projectPath,
      projectAccessor = accessor,
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

    val (projectAccessor, moduleRef) = testFixturesModuleNamePair

    val projectPath = ProjectPath.from(moduleRef)
    val accessor = ProjectAccessor.from(projectAccessor, projectPath)

    block.addModuleStatement(
      configName = configName.asConfigurationName(),
      parsedString = text,
      projectPath = projectPath,
      projectAccessor = accessor,
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
    ?.let { projectAccessorCallExpression ->

      val projectAccessor = projectAccessorCallExpression.text

      projectAccessorCallExpression
        .valueArguments                                   // [path = ":foo:bar"]
        .firstOrNull()                                    // path = ":foo:bar"
        ?.getChildOfType<KtStringTemplateExpression>()    // ":foo:bar"
        ?.getChildOfType<KtLiteralStringTemplateEntry>()  // :foo:bar
        ?.text
        ?.let { moduleRef -> projectAccessor to moduleRef }
    }
}

internal fun KtCallExpression.getTypeSafeModuleNameOrNull(): Pair<String, String>? {
  return this                                       // implementation(projects.foo.bar)
    .valueArguments                                 // [projects.foo.bar]
    .firstOrNull()                                  // projects.foo.bar
    ?.getChildOfType<KtDotQualifiedExpression>()    // projects.foo.bar
    ?.let { projectAccessorCallExpression ->

      val projectAccessor = projectAccessorCallExpression.text

      projectAccessorCallExpression.text
        ?.takeIf { it.startsWith("projects.") }
        ?.removePrefix("projects.")
        ?.let { moduleRef -> projectAccessor to moduleRef }
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
    ?.let { projectAccessorCallExpression ->

      val projectAccessor = projectAccessorCallExpression.text

      projectAccessorCallExpression
        .valueArguments                                   // [path = ":foo:bar"]
        .firstOrNull()                                    // path = ":foo:bar"
        ?.getChildOfType<KtStringTemplateExpression>()    // ":foo:bar"
        ?.getChildOfType<KtLiteralStringTemplateEntry>()  // :foo:bar
        ?.text
        ?.let { moduleRef -> projectAccessor to moduleRef }
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
    ?.let { projectAccessorCallExpression ->

      val projectAccessor = projectAccessorCallExpression.text

      projectAccessorCallExpression
        .text
        ?.takeIf { it.startsWith("projects.") }
        ?.removePrefix("projects.")
        ?.let { moduleRef -> projectAccessor to moduleRef }
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
