/*
 * Copyright (C) 2021-2023 Rick Busarow
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

import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.runBlocking
import modulecheck.parsing.psi.internal.PsiElementResolver
import modulecheck.parsing.psi.internal.callSiteName
import modulecheck.parsing.psi.internal.fqNameSafe
import modulecheck.parsing.psi.internal.getByNameOrIndex
import modulecheck.parsing.psi.internal.getChildrenOfTypeRecursive
import modulecheck.parsing.psi.internal.identifier
import modulecheck.parsing.psi.internal.isCompanionObject
import modulecheck.parsing.psi.internal.isInCompanionObject
import modulecheck.parsing.psi.internal.isJvmStatic
import modulecheck.parsing.psi.internal.isPartOf
import modulecheck.parsing.psi.internal.isPrivateOrInternal
import modulecheck.parsing.psi.internal.isQualifiedPropertyOrCallExpression
import modulecheck.parsing.psi.internal.jvmNameOrNull
import modulecheck.parsing.psi.internal.jvmSimpleNames
import modulecheck.parsing.source.AnvilScopeNameEntry
import modulecheck.parsing.source.DeclaredName
import modulecheck.parsing.source.KotlinFile
import modulecheck.parsing.source.KotlinFile.ScopeArgumentParseResult
import modulecheck.parsing.source.McName.CompatibleLanguage.KOTLIN
import modulecheck.parsing.source.PackageName
import modulecheck.parsing.source.QualifiedDeclaredName
import modulecheck.parsing.source.RawAnvilAnnotatedType
import modulecheck.parsing.source.ReferenceName
import modulecheck.parsing.source.SimpleName.Companion.stripPackageNameFromFqName
import modulecheck.parsing.source.asDeclaredName
import modulecheck.parsing.source.internal.NameParser
import modulecheck.parsing.source.internal.NameParser.NameParserPacket
import modulecheck.utils.lazy.LazyDeferred
import modulecheck.utils.lazy.LazySet
import modulecheck.utils.lazy.dataSource
import modulecheck.utils.lazy.lazyDeferred
import modulecheck.utils.lazy.toLazySet
import modulecheck.utils.mapToSet
import modulecheck.utils.remove
import modulecheck.utils.requireNotNull
import org.jetbrains.kotlin.fileClasses.javaFileFacadeFqName
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.psi.KtAnnotated
import org.jetbrains.kotlin.psi.KtAnnotationEntry
import org.jetbrains.kotlin.psi.KtCallableDeclaration
import org.jetbrains.kotlin.psi.KtClassOrObject
import org.jetbrains.kotlin.psi.KtFile
import org.jetbrains.kotlin.psi.KtFunction
import org.jetbrains.kotlin.psi.KtImportDirective
import org.jetbrains.kotlin.psi.KtModifierListOwner
import org.jetbrains.kotlin.psi.KtNamedDeclaration
import org.jetbrains.kotlin.psi.KtObjectDeclaration
import org.jetbrains.kotlin.psi.KtPackageDirective
import org.jetbrains.kotlin.psi.KtParameter
import org.jetbrains.kotlin.psi.KtProperty
import org.jetbrains.kotlin.psi.classOrObjectRecursiveVisitor
import org.jetbrains.kotlin.psi.psiUtil.containingClassOrObject
import org.jetbrains.kotlin.psi.psiUtil.isTopLevelKtOrJavaMember
import org.jetbrains.kotlin.psi.psiUtil.parents
import java.io.File

class RealKotlinFile(
  override val file: File,
  override val psi: KtFile,
  private val psiResolver: PsiElementResolver,
  private val nameParser: NameParser
) : KotlinFile {

  override val name: String = psi.name

  override val packageName: PackageName by lazy { PackageName(psi.packageFqName.asString()) }

  // For `import com.foo as Bar`, the entry is `"Bar" to "com.foo".asExplicitKotlinReference()`
  private val _aliasMap = mutableMapOf<String, ReferenceName>()

  private val importsStrings: Lazy<Set<String>> = lazy {

    psi.importDirectives.asSequence()
      .filter { it.isValidImport }
      .filter { it.identifier() != null }
      .filter { it.identifier()?.contains("*")?.not() == true }
      .filter { !operatorSet.contains(it.identifier()) }
      .filter {
        @Suppress("UnsafeCallOnNullableType")
        !componentNRegex.matches(it.identifier()!!)
      }
      .mapNotNull { importDirective ->
        importDirective.importPath?.pathStr
          ?.also { nameString ->

            // Map aliases to their actual names, so that they can be looked up while resolving
            importDirective.alias
              // The KtImportAlias is `as Foo`.  It has three children:
              // [LeafPsiElement, PsiWhiteSpace, LeafPsiElement], which are [`as`, ` `, `Foo`]
              // respectively.
              ?.lastChild
              ?.text?.let { alias ->
                _aliasMap[alias] = ReferenceName(nameString, KOTLIN)
              }
          }
      }
      .toSet()
  }
  override val importsLazy: Lazy<Set<ReferenceName>> = lazy {
    importsStrings.value
      .mapToSet { ReferenceName(it, KOTLIN) }
  }

  val constructorInjectedParams: LazyDeferred<Set<QualifiedDeclaredName>> = lazyDeferred {
    referenceVisitor.constructorInjected.mapNotNull { psiResolver.declaredNameOrNull(it) }.toSet()
  }

  private val fileJavaFacadeName by lazy { psi.javaFileFacadeFqName.asString() }

  @Suppress("ComplexMethod")
  private fun KtNamedDeclaration.declaredNames(): List<QualifiedDeclaredName> {
    val fq = fqNameSafe() ?: return emptyList()

    val nameAsString = fq.asString()

    return buildList {

      fun kotlin(name: String) {
        val declared = DeclaredName.kotlin(
          packageName,
          name.stripPackageNameFromFqName(packageName)
        )
        if (!contains(declared)) {
          add(declared)
        }
      }

      fun both(name: String) {

        val kotlinOnly = nameAsString.contains("`.*`".toRegex())

        if (kotlinOnly) {
          kotlin(name)
        } else {

          val declared = name.stripPackageNameFromFqName(packageName)
            .asDeclaredName(packageName)
          add(declared)
        }
      }

      fun java(name: String) {
        val declared = DeclaredName.java(
          packageName,
          name.stripPackageNameFromFqName(packageName)
        )
        if (!contains(declared)) {
          add(declared)
        }
      }

      val psi = this@declaredNames

      fun parseCompanionObjectDeclarations(companionName: String) {
        both(nameAsString)

        if (isStatic()) {
          both(nameAsString.remove(".$companionName"))
        } else if (psi is KtCallableDeclaration) {
          kotlin(nameAsString.remove(".$companionName"))
        }
      }

      when {
        psi.isCompanionObject() -> {
          parseCompanionObjectDeclarations(psi.name ?: "Companion")
        }

        psi.isInCompanionObject() -> {
          val companion = containingClassOrObject as KtObjectDeclaration
          parseCompanionObjectDeclarations(companion.name ?: "Companion")
        }

        isTopLevelKtOrJavaMember() && psi !is KtClassOrObject && !isStatic() -> {
          kotlin(nameAsString)

          jvmSimpleNames().forEach {
            java("$fileJavaFacadeName.$it")
          }
        }

        // object non-static properties or functions
        isPartOf<KtObjectDeclaration>() && !isStatic() -> {
          val parentObjectOrNull = containingClassOrObject

          if (parentObjectOrNull == null) {
            both(nameAsString)
            java("$nameAsString.INSTANCE")
          } else {
            kotlin(nameAsString)

            val parentFqName = parentObjectOrNull.fqName.requireNotNull().asString()
            jvmSimpleNames().forEach {
              java("$parentFqName.INSTANCE.$it")
            }
          }
        }

        // object static properties
        isPartOf<KtObjectDeclaration>() && isStatic() -> {
          val parentFqName = containingClassOrObject?.fqNameSafe()
            .requireNotNull()
            .asString()

          val jvmNames = jvmSimpleNames()

          if (psi is KtFunction && psi.jvmNameOrNull() == null) {
            both(nameAsString)
          } else {
            kotlin(nameAsString)
          }

          jvmNames.forEach {
            java("$parentFqName.$it")
            // The IDE gives warnings about "static member [...] accessed via instance reference"
            // and hides this name from code completion, but it's technically still functional.
            java("$parentFqName.INSTANCE.$it")
          }
        }

        psi is KtParameter || (psi is KtProperty && !psi.isTopLevelKtOrJavaMember()) -> {

          kotlin(nameAsString)

          val parentFqName = containingClassOrObject.requireNotNull { text }
            .fqNameSafe()
            .requireNotNull()
            .asString()

          jvmSimpleNames().forEach {
            java("$parentFqName.$it")
          }
        }

        else -> {
          both(nameAsString)
        }
      }
    }
  }

  override val declarations: Set<QualifiedDeclaredName> by lazy {

    psi.getChildrenOfTypeRecursive<KtNamedDeclaration>()
      .asSequence()
      .filterNot { it.isPrivateOrInternal() }
      .filterNot {
        it.parents.filterIsInstance<KtModifierListOwner>()
          .any { upstream -> upstream.isPrivateOrInternal() }
      }
      .flatMap { it.declaredNames() }
      .toSet()
  }

  private val wildcardImports by lazy {

    psi.importDirectives.filter { it.identifier()?.contains("*") != false }
      .mapNotNull { it.importPath?.pathStr }
      .toSet()
  }

  override val apiReferences: LazyDeferred<Set<ReferenceName>> = lazyDeferred {

    refs.await().apiReferenceNames
  }

  private val referenceVisitor by lazy {
    ReferenceVisitor().also { psi.accept(it) }
  }

  private val typeReferences = lazyDeferred {
    referenceVisitor.typeReferences
      .asSequence()
      .plus(referenceVisitor.callableReferences)
      .plus(referenceVisitor.qualifiedExpressions)
      .filterNot { it.isPartOf<KtImportDirective>() }
      .filterNot { it.isPartOf<KtPackageDirective>() }
      .filterNot { it.isQualifiedPropertyOrCallExpression() }
      .toSet()
  }

  private val refs = lazyDeferred {

    val mustBeApi = referenceVisitor.apiReferences.mapToSet { it.callSiteName() }

    val unresolved = typeReferences.await().mapToSet { it.callSiteName() }

    nameParser.parse(
      NameParserPacket(
        packageName = packageName,
        imports = importsStrings.value,
        wildcardImports = wildcardImports,
        aliasedImports = _aliasMap,
        resolved = emptySet(),
        unresolved = unresolved,
        mustBeApi = mustBeApi,
        apiReferenceNames = emptySet(),
        referenceLanguage = KOTLIN,
        stdLibNameOrNull = String::kotlinStdLibNameOrNull
      )
    )
  }

  override val references: LazySet<ReferenceName> = listOf(
    dataSource { refs.await().resolved }
  ).toLazySet()

  override suspend fun getAnvilScopeArguments(
    allAnnotations: List<ReferenceName>,
    mergeAnnotations: List<ReferenceName>
  ): ScopeArgumentParseResult {
    val mergeArguments = mutableSetOf<RawAnvilAnnotatedType>()
    val contributeArguments = mutableSetOf<RawAnvilAnnotatedType>()

    val visitor = classOrObjectRecursiveVisitor vis@{ classOrObject ->

      val typeFqName = classOrObject.fqNameSafe() ?: return@vis
      val annotated = classOrObject as?KtAnnotated ?: return@vis

      annotated.annotationEntries.filter { annotationEntry ->
        val typeRef = annotationEntry.typeReference?.text ?: return@filter false

        allAnnotations.any { it.endsWith(typeRef) }
      }.forEach { annotationEntry ->
        val typeRef = annotationEntry.typeReference.requireNotNull().text

        runBlocking {
          val raw = annotationEntry.toRawAnvilAnnotatedType(typeFqName)
            ?: return@runBlocking

          if (mergeAnnotations.any { it.endsWith(typeRef) }) {
            mergeArguments.add(raw)
          } else {
            contributeArguments.add(raw)
          }
        }
      }
    }

    psi.accept(visitor)

    return ScopeArgumentParseResult(
      mergeArguments = mergeArguments,
      contributeArguments = contributeArguments
    )
  }

  private fun KtNamedDeclaration.isStatic(): Boolean {
    return (this as? KtCallableDeclaration)?.isJvmStatic() == true
  }

  private suspend fun KtAnnotationEntry.toRawAnvilAnnotatedType(
    typeFqName: FqName
  ): RawAnvilAnnotatedType? {
    val valueArgument = valueArgumentList?.getByNameOrIndex(0, "scope") ?: return null

    val entryText = ReferenceName(
      valueArgument.text
        // remove the names for arguments
        .remove(".+=+".toRegex())
        .remove("::class")
        .trim(),
      KOTLIN
    )

    val resolvedScope = this@RealKotlinFile.references
      .firstOrNull { ref -> ref.endsWith(entryText) }
      ?: entryText

    return RawAnvilAnnotatedType(
      declaredName = typeFqName.asDeclaredName(packageName),
      anvilScopeNameEntry = AnvilScopeNameEntry(resolvedScope)
    )
  }

  internal companion object {
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
