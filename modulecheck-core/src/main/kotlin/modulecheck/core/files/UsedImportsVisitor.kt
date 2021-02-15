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

package modulecheck.core.files

import org.jetbrains.kotlin.kdoc.psi.impl.KDocTag
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.psi.*
import org.jetbrains.kotlin.psi.psiUtil.getChildrenOfType
import org.jetbrains.kotlin.resolve.BindingContext
import org.jetbrains.kotlin.resolve.descriptorUtil.fqNameOrNull
import org.jetbrains.kotlin.resolve.descriptorUtil.getImportableDescriptor

internal fun KtImportDirective.identifier() = this.importPath?.importedName?.identifier

// adapted from Detekt's UnusedImportsVisitor
// https://github.com/detekt/detekt/blob/master/detekt-rules-style/src/main/kotlin/io/gitlab/arturbosch/detekt/rules/style/UnusedImports.kt
class UsedImportsVisitor(
  private val bindingContext: BindingContext
) : KtTreeVisitorVoid() {
  private var currentPackage: FqName? = null
  private var imports: List<KtImportDirective>? = null
  val namedReferences = mutableSetOf<KtReferenceExpression>()
  var fqNames = listOf<FqName>()
  private val namedReferencesInKDoc = mutableSetOf<String>()

  fun usedImports(): List<KtImportDirective> {
    fun KtImportDirective.isFromSamePackage() =
      importedFqName?.parent() == currentPackage && alias == null

    @Suppress("ReturnCount")
    fun KtImportDirective.isUsed(): Boolean {
      val namedReferencesAsString = namedReferences.map { it.text.trim('`') }
      if (aliasName in (namedReferencesInKDoc + namedReferencesAsString)) return true

      val identifier = identifier()
      if (identifier in namedReferencesInKDoc) return true

      return if (bindingContext == BindingContext.EMPTY) {
        identifier in namedReferencesAsString
      } else {
        fqNames = namedReferences.mapNotNull { referenceExpression ->
          val descriptor =
            bindingContext[BindingContext.SHORT_REFERENCE_TO_COMPANION_OBJECT, referenceExpression]
              ?: bindingContext[BindingContext.REFERENCE_TARGET, referenceExpression]
          descriptor?.getImportableDescriptor()?.fqNameOrNull()
        }
        importPath?.fqName?.let { it !in fqNames } != false
      }
    }

    return imports?.filter { it.isFromSamePackage() || it.isUsed() }.orEmpty()
  }

  override fun visitPackageDirective(directive: KtPackageDirective) {
    currentPackage = directive.fqName
    super.visitPackageDirective(directive)
  }

  @Suppress("UnsafeCallOnNullableType")
  override fun visitImportList(importList: KtImportList) {
    imports = importList.imports.asSequence().filter { it.isValidImport }
      .filter { it.identifier()?.contains("*")?.not() == true }
      .filter { it.identifier() != null }
      .filter { !operatorSet.contains(it.identifier()) }
      .filter { !componentNRegex.matches(it.identifier()!!) }.toList()
    super.visitImportList(importList)
  }

  override fun visitReferenceExpression(expression: KtReferenceExpression) {
    expression
      .takeIf { !it.isPartOf<KtImportDirective>() && !it.isPartOf<KtPackageDirective>() }
      ?.takeIf { it.children.isEmpty() }
      ?.run { namedReferences.add(this) }
    super.visitReferenceExpression(expression)
  }

  override fun visitDeclaration(dcl: KtDeclaration) {
    val kdoc = dcl.docComment?.getDefaultSection()

    kdoc?.getChildrenOfType<KDocTag>()
      ?.map { it.text }
      ?.forEach { handleKDoc(it) }

    kdoc?.getContent()?.let {
      handleKDoc(it)
    }
    super.visitDeclaration(dcl)
  }

  private fun handleKDoc(content: String) {
    kotlinDocReferencesRegExp.findAll(content, 0)
      .map { it.groupValues[1] }
      .forEach { namedReferencesInKDoc.add(it.split(".")[0]) }
    kotlinDocBlockTagReferenceRegExp.find(content)?.let {
      val str = it.groupValues[2].split(whiteSpaceRegex)[0]
      namedReferencesInKDoc.add(str.split(".")[0])
    }
  }

  companion object {
    private val operatorSet = setOf(
      "compareTo",
      "contains",
      "dec",
      "div",
      "divAssign",
      "equals",
      "get",
      "getValue",
      "inc",
      "invoke",
      "iterator",
      "minus",
      "minusAssign",
      "mod",
      "modAssign",
      "not",
      "plus",
      "plusAssign",
      "provideDelegate",
      "rangeTo",
      "set",
      "setValue",
      "times",
      "timesAssign",
      "unaryMinus",
      "unaryPlus"
    )

    private val kotlinDocReferencesRegExp = Regex("\\[([^]]+)](?!\\[)")
    private val kotlinDocBlockTagReferenceRegExp = Regex("^@(see|throws|exception) (.+)")
    private val whiteSpaceRegex = Regex("\\s+")
    private val componentNRegex = Regex("component\\d+")
  }
}
