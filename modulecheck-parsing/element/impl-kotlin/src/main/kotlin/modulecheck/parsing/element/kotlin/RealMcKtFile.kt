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

package modulecheck.parsing.element.kotlin

import kotlinx.coroutines.flow.fold
import kotlinx.coroutines.flow.toSet
import modulecheck.parsing.element.McAnnotation
import modulecheck.parsing.element.McFile
import modulecheck.parsing.element.McFile.McKtFile
import modulecheck.parsing.element.McFile.McKtFile.ScopeArgumentParseResult
import modulecheck.parsing.element.McFunction
import modulecheck.parsing.element.McProperty
import modulecheck.parsing.element.McType.McConcreteType.McKtConcreteType
import modulecheck.parsing.element.McVisibility
import modulecheck.parsing.source.DeclaredName
import modulecheck.parsing.source.JavaSpecificDeclaredName
import modulecheck.parsing.source.PackageName
import modulecheck.parsing.source.ReferenceName
import modulecheck.utils.lazy.LazySet
import modulecheck.utils.lazy.dataSource
import modulecheck.utils.lazy.lazySet
import modulecheck.utils.lazy.unsafeLazy
import org.jetbrains.kotlin.fileClasses.javaFileFacadeFqName
import org.jetbrains.kotlin.psi.KtFile
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

  override val imports: LazySet.DataSource<ReferenceName.ExplicitReferenceName> =
    dataSource(priority = LazySet.DataSource.Priority.HIGH) {
      importParser.imports
    }
  override val wildcardImports: LazySet.DataSource<String> = dataSource {
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
    allAnnotations: List<ReferenceName.ExplicitReferenceName>,
    mergeAnnotations: List<ReferenceName.ExplicitReferenceName>
  ): ScopeArgumentParseResult {
    TODO("Not yet implemented")
  }
}
