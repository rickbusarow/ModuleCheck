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

import modulecheck.parsing.psi.internal.PsiElementResolver
import modulecheck.parsing.psi.internal.getByNameOrIndex
import modulecheck.parsing.psi.internal.getChildrenOfTypeRecursive
import modulecheck.parsing.psi.internal.identifier
import modulecheck.parsing.psi.internal.isPrivateOrInternal
import modulecheck.parsing.source.AnvilScopeNameEntry
import modulecheck.parsing.source.KotlinFile
import modulecheck.parsing.source.KotlinFile.ScopeArgumentParseResult
import modulecheck.parsing.source.RawAnvilAnnotatedType
import modulecheck.parsing.source.asDeclarationName
import modulecheck.utils.LazyDeferred
import modulecheck.utils.awaitAll
import modulecheck.utils.lazyDeferred
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.psi.KtAnnotated
import org.jetbrains.kotlin.psi.KtAnnotationEntry
import org.jetbrains.kotlin.psi.KtFile
import org.jetbrains.kotlin.psi.KtNamedDeclaration
import org.jetbrains.kotlin.psi.classOrObjectRecursiveVisitor
import org.jetbrains.kotlin.utils.addToStdlib.safeAs

class RealKotlinFile(
  val ktFile: KtFile,
  private val psiResolver: PsiElementResolver
) : KotlinFile {

  override val name = ktFile.name

  override val packageFqName by lazy { ktFile.packageFqName.asString() }

  override val imports by lazy {

    ktFile.importDirectives
      .asSequence()
      .filter { it.isValidImport }
      .filter { it.identifier() != null }
      .filter { it.identifier()?.contains("*")?.not() == true }
      .filter { !operatorSet.contains(it.identifier()) }
      .filter { !componentNRegex.matches(it.identifier()!!) }
      .mapNotNull { importDirective ->
        importDirective
          .importPath
          ?.pathStr
      }
      .toSet()
  }

  val constructorInjectedParams = lazyDeferred {
    referenceVisitor.constructorInjected
      .mapNotNull { psiResolver.fqNameOrNull(it) }
      .toSet()
  }

  override val declarations by lazy {

    ktFile.getChildrenOfTypeRecursive<KtNamedDeclaration>()
      .asSequence()
      .filterNot { it.isPrivateOrInternal() }
      .mapNotNull { it.fqName }
      .map { it.asString().replace(".Companion", "").asDeclarationName() }
      .toSet()
  }

  override val wildcardImports by lazy {

    ktFile.importDirectives
      .filter { it.identifier()?.contains("*") != false }
      .mapNotNull { it.importPath?.pathStr }
      .toSet()
  }

  override val apiReferences: LazyDeferred<Set<String>> = lazyDeferred {

    val apiRefsAsStrings = referenceVisitor.apiReferences.map { it.text }

    val replacedWildcards = wildcardImports.flatMap { wildcardImport ->

      apiRefsAsStrings.map { apiReference ->
        wildcardImport.replace("*", apiReference)
      }
    }

    val (resolved, unresolved) = apiRefsAsStrings.map { reference ->
      imports.firstOrNull { it.endsWith(reference) } ?: reference
    }.partition { it in imports }

    val simple = unresolved + unresolved.map {
      ktFile.packageFqName.asString() + "." + it
    }

    (resolved + simple + replacedWildcards).toSet()
  }

  private val referenceVisitor by lazy {
    ReferenceVisitor(this)
      .also { ktFile.accept(it) }
  }

  private val typeReferences = lazyDeferred {
    referenceVisitor.typeReferences
      // .filterNot { it.isPartOf<KtImportDirective>() }
      // .filterNot { it.isPartOf<KtPackageDirective>() }
      // .mapNotNull { it.fqNameOrNull(project, sourceSetName)?.asString() }
      .map { it.text }
      .toSet()
  }

  private val callableReferences = lazyDeferred {
    referenceVisitor.callableReferences
      // .filterNot { it.isPartOf<KtImportDirective>() }
      // .filterNot { it.isPartOf<KtPackageDirective>() }
      // .mapNotNull { it.fqNameOrNull(project, sourceSetName)?.asString() }
      .map { it.text }
      .toSet()
  }

  private val qualifiedExpressions = lazyDeferred {
    referenceVisitor.qualifiedExpressions
      // .filterNot { it.isPartOf<KtImportDirective>() }
      // .filterNot { it.isPartOf<KtPackageDirective>() }
      // .mapNotNull { it.fqNameOrNull(project, sourceSetName)?.asString() }
      .map { it.text }
      .toSet()
  }

  override val maybeExtraReferences = lazyDeferred {
    val allOther = listOf(
      typeReferences,
      callableReferences,
      qualifiedExpressions
    )
      .awaitAll()
      .flatten()
      .toSet()

    allOther + allOther.map {
      "$packageFqName.$it"
    } + wildcardImports.flatMap { wildcardImport ->

      allOther.map { referenceString ->
        wildcardImport.replace("*", referenceString)
      }
    }
      .toSet()
  }

  override fun getScopeArguments(
    allAnnotations: Set<String>,
    mergeAnnotations: Set<String>
  ): ScopeArgumentParseResult {
    val mergeArguments = mutableSetOf<RawAnvilAnnotatedType>()
    val contributeArguments = mutableSetOf<RawAnvilAnnotatedType>()

    val visitor = classOrObjectRecursiveVisitor vis@{ classOrObject ->

      val typeFqName = classOrObject.fqName ?: return@vis
      val annotated = classOrObject.safeAs<KtAnnotated>() ?: return@vis

      annotated
        .annotationEntries
        .filter { annotationEntry ->
          val typeRef = annotationEntry.typeReference?.text ?: return@filter false

          allAnnotations.any { it.endsWith(typeRef) }
        }
        .forEach { annotationEntry ->
          val typeRef = annotationEntry.typeReference!!.text

          val raw = annotationEntry.toRawAnvilAnnotatedType(typeFqName) ?: return@forEach

          if (mergeAnnotations.any { it.endsWith(typeRef) }) {
            mergeArguments.add(raw)
          } else {
            contributeArguments.add(raw)
          }
        }
    }

    ktFile.accept(visitor)

    return ScopeArgumentParseResult(
      mergeArguments = mergeArguments,
      contributeArguments = contributeArguments
    )
  }

  private fun KtAnnotationEntry.toRawAnvilAnnotatedType(
    typeFqName: FqName
  ): RawAnvilAnnotatedType? {
    val valueArgument = valueArgumentList
      ?.getByNameOrIndex(0, "scope")
      ?: return null

    val entryText = valueArgument
      .text
      .replace(".+[=]+".toRegex(), "") // remove named arguments
      .replace("::class", "")
      .trim()

    return RawAnvilAnnotatedType(
      declarationName = typeFqName.asDeclarationName(),
      anvilScopeNameEntry = AnvilScopeNameEntry(entryText)
    )
  }

  private companion object {
    val operatorSet = setOf(
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
    val componentNRegex = Regex("component\\d+")
  }
}
