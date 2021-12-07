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

import modulecheck.parsing.JvmFile
import modulecheck.parsing.psi.internal.fqNameOrNull
import modulecheck.project.McProject
import modulecheck.project.SourceSetName
import modulecheck.utils.awaitAll
import modulecheck.utils.lazyDeferred
import org.jetbrains.kotlin.psi.KtFile
import org.jetbrains.kotlin.resolve.BindingContext

class KotlinFile(
  project: McProject,
  val ktFile: KtFile,
  val bindingContext: BindingContext,
  val sourceSetName: SourceSetName
) : JvmFile(project) {

  override val name = ktFile.name

  override val packageFqName by lazy { ktFile.packageFqName.asString() }

  private val importDirectives by lazy {
    usedImportsVisitor
      .usedImports()
      .toSet()
  }

  override val imports by lazy {
    importDirectives
      .mapNotNull { importDirective ->
        importDirective
          .importPath
          ?.pathStr
      }
      .toSet()
  }

  val constructorInjectedParams = lazyDeferred {
    referenceVisitor.constructorInjected
      .mapNotNull { it.fqNameOrNull(project, sourceSetName) }
      .toSet()
  }

  override val declarations by lazy {

    val v = DeclarationVisitor()
    ktFile.accept(v)
    v.declarations
  }

  override val wildcardImports by lazy {
    ktFile
      .importDirectives
      .mapNotNull { importDirective ->
        importDirective
          .importPath
          ?.pathStr
      }
      .filter { it.endsWith('*') }
      .toSet()
  }

  val apiReferences = lazyDeferred {
    // referenceVisitor.apiReferences
    //   .mapNotNull { it.fqNameOrNull(project, sourceSetName) }
    //   // .map { FqName(it.text) }
    //   .toSet()

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

  private val usedImportsVisitor by lazy {
    UsedImportsVisitor(bindingContext)
      .also { ktFile.accept(it) }
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
}

/*
internal val kotlinTypes = listOf(
  "kotlin.Any",
  "kotlin.Array",
  "kotlin.Unit",
  "kotlin.Boolean",
  "kotlin.Byte",
  "kotlin.Short",
  "kotlin.Int",
  "kotlin.Long",
  "kotlin.Char",
  "kotlin.Float",
  "kotlin.Double",
  "kotlin.String",
  "kotlin.CharSequence",
  "kotlin.Comparable",
  "kotlin.Throwable",
  "kotlin.Annotation",
  "kotlin.Nothing",
  "kotlin.Number",
  "kotlin.collections.Iterable",
  "kotlin.collections.Collection",
  "kotlin.collections.List",
  "kotlin.collections.Set",
  "kotlin.collections.Map",
  "kotlin.collections.Map.Entry",
  "kotlin.collections.MutableIterable",
  "kotlin.collections.MutableCollection",
  "kotlin.collections.MutableList",
  "kotlin.collections.MutableSet",
  "kotlin.collections.MutableMap",
  "kotlin.collections.MutableMap.Entry",
  "kotlin.BooleanArray",
  "kotlin.ByteArray",
  "kotlin.CharArray",
  "kotlin.ShortArray",
  "kotlin.IntArray",
  "kotlin.LongArray",
  "kotlin.FloatArray",
  "kotlin.DoubleArray",
  "kotlin.Enum",
  "kotlin.UByte",
  "kotlin.UShort",
  "kotlin.UInt",
  "kotlin.ULong",
  "kotlin.UByteArray",
  "kotlin.UShortArray",
  "kotlin.UIntArray",
  "kotlin.ULongArray"
)
*/
