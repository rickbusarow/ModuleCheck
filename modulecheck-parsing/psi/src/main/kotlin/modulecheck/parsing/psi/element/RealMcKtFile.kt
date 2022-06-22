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

package modulecheck.parsing.psi.element

import kotlinx.coroutines.flow.fold
import kotlinx.coroutines.flow.toSet
import modulecheck.parsing.source.DeclaredName
import modulecheck.parsing.source.JavaSpecificDeclaredName
import modulecheck.parsing.source.PackageName
import modulecheck.parsing.source.ReferenceName
import modulecheck.parsing.source.ReferenceName.ExplicitKotlinReferenceName
import modulecheck.parsing.source.ReferenceName.ExplicitReferenceName
import modulecheck.parsing.source.asExplicitKotlinReference
import modulecheck.parsing.source.element.McAnnotation
import modulecheck.parsing.source.element.McFile
import modulecheck.parsing.source.element.McFile.McKtFile
import modulecheck.parsing.source.element.McFile.McKtFile.ScopeArgumentParseResult
import modulecheck.parsing.source.element.McFunction
import modulecheck.parsing.source.element.McProperty
import modulecheck.parsing.source.element.McType.McConcreteType.McKtConcreteType
import modulecheck.parsing.source.element.McVisibility
import modulecheck.utils.lazy.LazySet
import modulecheck.utils.lazy.LazySet.DataSource
import modulecheck.utils.lazy.LazySet.DataSource.Priority.HIGH
import modulecheck.utils.lazy.dataSource
import modulecheck.utils.lazy.lazySet
import modulecheck.utils.lazy.unsafeLazy
import modulecheck.utils.requireNotNull
import org.jetbrains.kotlin.fileClasses.javaFileFacadeFqName
import org.jetbrains.kotlin.psi.KtFile
import org.jetbrains.kotlin.psi.KtImportDirective
import java.io.File

class RealMcKtFile(
  private val parsingContext: ParsingContext,
  override val javaFile: File,
  override val psi: KtFile
) : McKtFile {

  override val annotations: LazySet<McAnnotation> = lazySet {
    psi.fileAnnotationList
    TODO("Not yet implemented")
  }
  override val declaredTypes: LazySet<McKtConcreteType> = lazySet {
    psi.mcKtConcreteTypesDirect(
      parsingContext = parsingContext, containingFile = this, parent = this
    )
  }
  override val declaredTypesAndInnerTypes: LazySet<McKtConcreteType> = lazySet {
    declaredTypes.fold(setOf()) { acc, type ->
      acc + type + type.innerTypesRecursive.toSet()
    }
  }
  override val containingFile: McFile.McKtFile get() = this

  private val fileJavaFacadeName by lazy { psi.javaFileFacadeFqName.asString() }

  private val importParser by unsafeLazy { ImportParser(psi.importDirectives) }

  // For `import com.foo as Bar`, the entry is `"Bar" to "com.foo".asExplicitKotlinReference()`
  override val importAliases by lazy {
    importParser.aliasMap
  }

  override val imports: DataSource<ExplicitReferenceName> = dataSource(priority = HIGH) {
    importParser.imports
  }
  override val wildcardImports: DataSource<String> = dataSource {
    importParser.wildcards
  }

  override val simpleName: String by lazy { fileJavaFacadeName.split(".").last() }
  override val declaredName: DeclaredName by lazy {
    JavaSpecificDeclaredName(simpleName, packageName)
  }

  override val packageName: PackageName by lazy { PackageName(psi.packageFqName.asString()) }
  override val visibility: McVisibility
    get() = TODO("Not yet implemented")

  override val topLevelFunctions: LazySet<McFunction>
    get() = TODO("Not yet implemented")
  override val topLevelProperties: LazySet<McProperty>
    get() = TODO("Not yet implemented")
  override val apiReferences: List<LazySet.DataSource<ReferenceName>> = emptyList()
  override val references: List<LazySet.DataSource<ReferenceName>> = emptyList()
  override val declarations: List<LazySet.DataSource<DeclaredName>> = emptyList()

  override suspend fun getAnvilScopeArguments(
    allAnnotations: List<ExplicitReferenceName>,
    mergeAnnotations: List<ExplicitReferenceName>
  ): ScopeArgumentParseResult {
    TODO("Not yet implemented")
  }
}

private class ImportParser(private val importDirectives: List<KtImportDirective>) {

  private val _aliasMap = mutableMapOf<String, ReferenceName.ExplicitKotlinReferenceName>()

  val aliasMap: Map<String, ExplicitKotlinReferenceName> by unsafeLazy {
    imports
    _aliasMap
  }

  val imports: Set<ExplicitKotlinReferenceName> by lazy {

    importDirectives
      .asSequence()
      .filter { it.isValidImport }
      .filter { !it.isAllUnder }
      .filter { it.importPath != null }
      .map { directive ->
        directive.importPath.requireNotNull()
          .pathStr
          .asExplicitKotlinReference()
          .also { it.maybeCacheAlias(directive) }
      }
      .toSet()
  }

  val wildcards: Set<String> by lazy {

    importDirectives
      .asSequence()
      .filter { it.isValidImport }
      .filter { it.isAllUnder }
      .filter { it.importPath != null }
      .map { directive ->
        directive.importPath.requireNotNull()
          .pathStr
      }
      .toSet()
  }

  private fun ExplicitKotlinReferenceName.maybeCacheAlias(
    importDirective: KtImportDirective
  ) {
    val explicitReference = this

    // Map an alias to its actual name, so that it can be looked up/inlined while resolving
    importDirective.alias
      // The KtImportAlias is `as Foo`.  It has three children:
      // [LeafPsiElement, PsiWhiteSpace, LeafPsiElement], which are [`as`, ` `, `Foo`]
      // respectively.
      ?.lastChild
      ?.text
      ?.let { alias ->
        _aliasMap[alias] = explicitReference
      }
  }
}
