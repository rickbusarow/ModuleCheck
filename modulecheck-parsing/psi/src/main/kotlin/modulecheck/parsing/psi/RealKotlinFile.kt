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

import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.asFlow
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.flatMapMerge
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.toSet
import modulecheck.parsing.psi.internal.PsiElementResolver
import modulecheck.parsing.psi.internal.findAnnotation
import modulecheck.parsing.psi.internal.findAnnotationArgument
import modulecheck.parsing.psi.internal.getByNameOrIndex
import modulecheck.parsing.psi.internal.getChildrenOfTypeRecursive
import modulecheck.parsing.psi.internal.hasAnnotation
import modulecheck.parsing.psi.internal.identifier
import modulecheck.parsing.psi.internal.isPartOf
import modulecheck.parsing.psi.internal.isPrivateOrInternal
import modulecheck.parsing.source.AnvilBindingReference
import modulecheck.parsing.source.AnvilBoundType
import modulecheck.parsing.source.AnvilScope
import modulecheck.parsing.source.AnvilScopeName
import modulecheck.parsing.source.JvmFile.ScopeArgumentParseResult
import modulecheck.parsing.source.KotlinFile
import modulecheck.parsing.source.RawAnvilAnnotatedType
import modulecheck.parsing.source.asDeclarationName
import modulecheck.utils.LazyDeferred
import modulecheck.utils.awaitAll
import modulecheck.utils.lazyDeferred
import modulecheck.utils.mapAsyncNotNull
import modulecheck.utils.requireNotNull
import modulecheck.utils.safeAs
import org.jetbrains.kotlin.com.intellij.psi.PsiElement
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.psi.KtAnnotated
import org.jetbrains.kotlin.psi.KtAnnotationEntry
import org.jetbrains.kotlin.psi.KtCallableDeclaration
import org.jetbrains.kotlin.psi.KtClassLiteralExpression
import org.jetbrains.kotlin.psi.KtClassOrObject
import org.jetbrains.kotlin.psi.KtFile
import org.jetbrains.kotlin.psi.KtFunction
import org.jetbrains.kotlin.psi.KtImportDirective
import org.jetbrains.kotlin.psi.KtNameReferenceExpression
import org.jetbrains.kotlin.psi.KtNamedDeclaration
import org.jetbrains.kotlin.psi.KtPackageDirective
import org.jetbrains.kotlin.psi.KtProperty
import org.jetbrains.kotlin.psi.KtTypeReference
import org.jetbrains.kotlin.psi.classOrObjectRecursiveVisitor
import org.jetbrains.kotlin.psi.psiUtil.containingClassOrObject
import org.jetbrains.kotlin.psi.psiUtil.getChildOfType

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

  override val constructorInjectedTypes = lazyDeferred {
    referenceVisitor.constructorInjected
      .mapNotNull { it.fqNameOrNull() }
      .toSet()
  }

  override val memberInjectedTypes = lazyDeferred {
    referenceVisitor.memberInjected
      .mapNotNull { it.fqNameOrNull() }
      .toSet()
  }

  val classesAndInnerClasses by lazy {
    ktFile.getChildrenOfTypeRecursive<KtClassOrObject>()
  }

  override val simpleBoundTypes = lazyDeferred {
    referenceVisitor.boundByInject
      .mapNotNull { clazz ->
        clazz.fqName?.let { AnvilBoundType(it) }
      }
      .toSet()
  }

  private val contributesToClasses = lazyDeferred {

    classesAndInnerClasses
      .mapNotNull { clazz ->
        val annotationEntry = clazz.findAnnotation(this, FqNames.contributesTo)
          ?: return@mapNotNull null

        val scope = annotationEntry
          .findAnnotationArgument<KtClassLiteralExpression>(name = "scope", index = 0)
          ?.let {
            it.fqNameOrNull()
            // The PsiElement in question here will always be a class reference, so if it can't be
            // resolved, it's either a third-party type which is imported via a wildcard, or it's
            // a stdlib type which doesn't need an import, like `Unit::class`.  Falling back to just
            // using the class's simple name should be fine.
              ?: FqName(it.getChildOfType<KtNameReferenceExpression>().requireNotNull().text)
          }
          ?.let { AnvilScopeName(it) }
          ?: return@mapNotNull null

        AnvilScopedClassOrObject(clazz, scope)
      }
  }

  private val contributedModulesToComponents = lazyDeferred {

    contributesToClasses.await()
      .partition { it.clazz.hasAnnotation(this, FqNames.module) }
  }

  private val contributedModules = lazyDeferred {

    contributedModulesToComponents.await().first
  }

  private val contributedComponents = lazyDeferred {

    contributedModulesToComponents.await().second
  }

  override val componentBindingReferences = lazyDeferred {

    contributedComponents.await()
      .flatMap { (clazz, scope) ->
        clazz.getChildrenOfTypeRecursive<KtProperty>()
          // In case of nesting classes/interfaces,
          // only use properties from their directly containing class.
          .filter { it.containingClassOrObject == clazz }
          .mapNotNull { property ->
            property.getChildOfType<KtTypeReference>()
              ?.fqNameOrNull()
              ?.let { referencedType -> AnvilBindingReference(referencedType, scope) }
          }
      }
  }

  override val moduleBindingReferences = lazyDeferred {

    contributedModules.await()
      .asFlow()
      .scopedCallablesWithAnnotation(FqNames.binds)
      .mapAsyncNotNull { (callableDeclaration, scope) ->

        callableDeclaration.realBoundTypeOrNull()
          ?.fqNameOrNull()
          ?.let { AnvilBindingReference(it, scope) }
      }
      .toSet()
  }

  override val boundTypes = lazyDeferred {

    contributedModules.await()
      .asFlow()
      .scopedCallablesWithAnnotation(FqNames.binds)
      .mapAsyncNotNull { (callableDeclaration, scope) ->
        val realType = callableDeclaration.realBoundTypeOrNull()?.fqNameOrNull()
          ?: return@mapAsyncNotNull null
        val boundType = callableDeclaration.typeReference?.fqNameOrNull()
          ?: return@mapAsyncNotNull null

        AnvilBoundType(
          boundType = boundType, realType = realType, scopeOrNull = scope
        )
      }
      .toSet()
  }

  private fun KtCallableDeclaration.realBoundTypeOrNull(): KtTypeReference? {
    return receiverTypeReference
      ?: (this as? KtFunction)?.valueParameters
        ?.singleOrNull()
        ?.typeReference
  }

  @OptIn(FlowPreview::class)
  private fun Flow<AnvilScopedClassOrObject>.scopedCallablesWithAnnotation(
    annotationFqName: FqName
  ): Flow<AnvilScopedCallable> = flatMapMerge { (clazz, scope) ->
    clazz.getChildrenOfTypeRecursive<KtProperty>()
      .plus(clazz.getChildrenOfTypeRecursive<KtFunction>())
      .asFlow()
      // In case of nesting classes/interfaces,
      // only use properties from their directly containing class.
      .filter { it.containingClassOrObject == clazz }
      .filter { it.hasAnnotation(this@RealKotlinFile, annotationFqName) }
      .map { AnvilScopedCallable(it, scope) }
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
      .filter { it.importPath?.isAllUnder == true }
      .mapNotNull { it.importPath?.fqName?.asString() }
      .toSet()
  }

  override val apiReferences: LazyDeferred<Set<String>> = lazyDeferred {

    val apiRefsAsStrings = referenceVisitor.apiReferences.map { it.text }

    val (resolved, unresolved) = apiRefsAsStrings.map { reference ->
      imports.firstOrNull { it.endsWith(reference) } ?: reference
    }.partition { it in imports }

    val replacedWildcards = wildcardImports.flatMap { wildcardImport ->

      unresolved.map { apiReference ->
        "$wildcardImport.$apiReference"
      }
    }

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
      .filterNot { it.isPartOf<KtImportDirective>() }
      .filterNot { it.isPartOf<KtPackageDirective>() }
      // .mapNotNull { it.fqNameOrNull(project, sourceSetName)?.asString() }
      .map { it.text }
      .toSet()
  }

  private val callableReferences = lazyDeferred {
    referenceVisitor.callableReferences
      .filterNot { it.isPartOf<KtImportDirective>() }
      .filterNot { it.isPartOf<KtPackageDirective>() }
      // .mapNotNull { it.fqNameOrNull(project, sourceSetName)?.asString() }
      .map { it.text }
      .toSet()
  }

  private val qualifiedExpressions = lazyDeferred {
    referenceVisitor.qualifiedExpressions
      .filterNot { it.isPartOf<KtImportDirective>() }
      .filterNot { it.isPartOf<KtPackageDirective>() }
      // .mapNotNull { it.fqNameOrNull(project, sourceSetName)?.asString() }
      .map { it.text }
      .toSet()
  }

  override val maybeExtraReferences = lazyDeferred {

    val unresolved = listOf(
      typeReferences,
      callableReferences,
      qualifiedExpressions
    )
      .awaitAll()
      .flatten()
      .map { reference -> imports.firstOrNull { it.endsWith(reference) } ?: reference }
      .filterNot { it in imports }

    val all = unresolved + unresolved.flatMap { reference ->
      wildcardImports.map { "$it.$reference" } + "$packageFqName.$reference"
    }

    all.toSet()
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
      anvilScope = AnvilScope(entryText)
    )
  }

  internal data class AnvilScopedClassOrObject(
    val clazz: KtClassOrObject,
    val scope: AnvilScopeName
  ) {
    override fun toString(): String = "scope -- $scope  -- class --${clazz.fqName}"
  }

  internal data class AnvilScopedCallable(
    val callable: KtCallableDeclaration,
    val scope: AnvilScopeName
  ) {
    override fun toString(): String = "scope -- $scope  -- class --${callable.fqName}"
  }

  private suspend fun PsiElement.fqNameOrNull(): FqName? = psiResolver.fqNameOrNull(this)

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
