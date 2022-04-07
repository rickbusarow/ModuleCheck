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

import modulecheck.parsing.psi.internal.PsiElementResolver
import modulecheck.parsing.psi.internal.callSiteName
import modulecheck.parsing.psi.internal.getByNameOrIndex
import modulecheck.parsing.psi.internal.getChildrenOfTypeRecursive
import modulecheck.parsing.psi.internal.identifier
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
import modulecheck.parsing.source.RawAnvilAnnotatedType
import modulecheck.parsing.source.Reference
import modulecheck.parsing.source.Reference.ExplicitKotlinReference
import modulecheck.parsing.source.Reference.ExplicitReference
import modulecheck.parsing.source.UnqualifiedAndroidResourceDeclaredName
import modulecheck.parsing.source.asDeclaredName
import modulecheck.parsing.source.asExplicitKotlinReference
import modulecheck.parsing.source.asJavaDeclaredName
import modulecheck.parsing.source.asKotlinDeclaredName
import modulecheck.parsing.source.internal.NameParser
import modulecheck.parsing.source.internal.NameParser.NameParserPacket
import modulecheck.utils.LazyDeferred
import modulecheck.utils.LazySet
import modulecheck.utils.dataSource
import modulecheck.utils.lazyDeferred
import modulecheck.utils.mapToSet
import modulecheck.utils.remove
import modulecheck.utils.requireNotNull
import modulecheck.utils.toLazySet
import modulecheck.utils.unsafeLazy
import org.jetbrains.kotlin.fileClasses.javaFileFacadeFqName
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.psi.KtAnnotated
import org.jetbrains.kotlin.psi.KtAnnotationEntry
import org.jetbrains.kotlin.psi.KtCallableDeclaration
import org.jetbrains.kotlin.psi.KtClassOrObject
import org.jetbrains.kotlin.psi.KtFile
import org.jetbrains.kotlin.psi.KtFunction
import org.jetbrains.kotlin.psi.KtImportDirective
import org.jetbrains.kotlin.psi.KtNamedDeclaration
import org.jetbrains.kotlin.psi.KtObjectDeclaration
import org.jetbrains.kotlin.psi.KtPackageDirective
import org.jetbrains.kotlin.psi.classOrObjectRecursiveVisitor
import org.jetbrains.kotlin.psi.psiUtil.containingClassOrObject
import org.jetbrains.kotlin.psi.psiUtil.isTopLevelKtOrJavaMember
import org.jetbrains.kotlin.utils.addToStdlib.safeAs

class RealKotlinFile(
  val ktFile: KtFile,
  private val psiResolver: PsiElementResolver,
  private val nameParser: NameParser
) : KotlinFile {

  override val name = ktFile.name

  override val packageFqName by lazy { ktFile.packageFqName.asString() }

  // For `import com.foo as Bar`, the entry is `"Bar" to "com.foo".asExplicitKotlinReference()`
  private val _aliasMap = mutableMapOf<String, ExplicitKotlinReference>()

  private val importsStrings: Lazy<Set<String>> = lazy {

    ktFile.importDirectives.asSequence()
      .filter { it.isValidImport }
      .filter { it.identifier() != null }
      .filter { it.identifier()?.contains("*")?.not() == true }
      .filter { !operatorSet.contains(it.identifier()) }
      .filter { !componentNRegex.matches(it.identifier()!!) }
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
                _aliasMap[alias] = nameString.asExplicitKotlinReference()
              }
          }
      }
      .toSet()
  }
  override val importsLazy: Lazy<Set<Reference>> = lazy {
    importsStrings.value
      .mapToSet { it.asExplicitKotlinReference() }
  }

  val constructorInjectedParams = lazyDeferred {
    referenceVisitor.constructorInjected.mapNotNull { psiResolver.fqNameOrNull(it) }.toSet()
  }

  private val fileJavaFacadeName by lazy { ktFile.javaFileFacadeFqName.asString() }

  @Suppress("ComplexMethod")
  private fun KtNamedDeclaration.declaredNames(): List<DeclaredName> {
    val fq = fqName ?: return emptyList()

    val nameAsString = fq.asString()

    return buildList {
      fun both(name: String) {
        add(name.asDeclaredName())
      }

      fun kotlin(name: String) {
        if (!contains(name.asDeclaredName())) {
          add(name.asKotlinDeclaredName())
        }
      }

      fun java(name: String) {
        if (!contains(name.asDeclaredName())) {
          add(name.asJavaDeclaredName())
        }
      }

      when {
        nameAsString.contains(".Companion") -> {
          both(nameAsString)

          if (isStatic()) {
            both(nameAsString.remove(".Companion"))
          } else if (this@declaredNames is KtCallableDeclaration) {
            kotlin(nameAsString.remove(".Companion"))
          }
        }

        isTopLevelKtOrJavaMember() && this@declaredNames !is KtClassOrObject && !isStatic() -> {
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
          val parentFqName = containingClassOrObject?.fqName
            .requireNotNull()
            .asString()

          val jvmNames = jvmSimpleNames()

          if (this@declaredNames is KtFunction && jvmNameOrNull() == null) {
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

        else -> {
          both(nameAsString)
        }
      }
    }
  }

  override val declarations by lazy {

    ktFile.getChildrenOfTypeRecursive<KtNamedDeclaration>()
      .asSequence()
      .filterNot { it.isPrivateOrInternal() }
      .flatMap { it.declaredNames() }
      .toSet()
  }

  private val wildcardImports by lazy {

    ktFile.importDirectives.filter { it.identifier()?.contains("*") != false }
      .mapNotNull { it.importPath?.pathStr }
      .toSet()
  }

  override val apiReferences: LazyDeferred<Set<Reference>> = lazyDeferred {
    return@lazyDeferred refs.await().apiReferences
  }

  private val referenceVisitor by lazy {
    ReferenceVisitor().also { ktFile.accept(it) }
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
        packageName = packageFqName,
        imports = importsStrings.value,
        wildcardImports = wildcardImports,
        aliasedImports = _aliasMap,
        resolved = emptySet(),
        unresolved = unresolved,
        mustBeApi = mustBeApi,
        apiReferences = emptySet(),
        toExplicitReference = Reference::ExplicitKotlinReference,
        toInterpretedReference = Reference::InterpretedKotlinReference,
        stdLibNameOrNull = String::kotlinStdLibNameOrNull
      )
    )
  }

  override val references: LazySet<Reference> = listOf(
    dataSource { refs.await().resolved }
  ).toLazySet()

  override fun getScopeArguments(
    allAnnotations: List<ExplicitReference>,
    mergeAnnotations: List<ExplicitReference>
  ): ScopeArgumentParseResult {
    val mergeArguments = mutableSetOf<RawAnvilAnnotatedType>()
    val contributeArguments = mutableSetOf<RawAnvilAnnotatedType>()

    val visitor = classOrObjectRecursiveVisitor vis@{ classOrObject ->

      val typeFqName = classOrObject.fqName ?: return@vis
      val annotated = classOrObject.safeAs<KtAnnotated>() ?: return@vis

      annotated.annotationEntries.filter { annotationEntry ->
        val typeRef = annotationEntry.typeReference?.text ?: return@filter false

        allAnnotations.any { it.endsWith(typeRef) }
      }.forEach { annotationEntry ->
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

  private fun KtNamedDeclaration.isStatic(): Boolean {
    return (this as? KtCallableDeclaration)?.isJvmStatic() == true
  }

  private fun KtAnnotationEntry.toRawAnvilAnnotatedType(
    typeFqName: FqName
  ): RawAnvilAnnotatedType? {
    val valueArgument = valueArgumentList?.getByNameOrIndex(0, "scope") ?: return null

    val entryText = valueArgument.text.replace(".+[=]+".toRegex(), "") // remove named arguments
      .replace("::class", "").trim()
      .asExplicitKotlinReference()

    return RawAnvilAnnotatedType(
      declaredName = typeFqName.asDeclaredName(),
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

    val prefixes by unsafeLazy { UnqualifiedAndroidResourceDeclaredName.prefixes() }
  }
}
