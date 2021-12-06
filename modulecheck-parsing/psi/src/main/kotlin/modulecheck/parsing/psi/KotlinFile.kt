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

import javassist.bytecode.stackmap.TypeData.ClassName
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

@JvmField public val ANY: ClassName = ClassName("kotlin", "Any")
@JvmField public val ARRAY: ClassName = ClassName("kotlin", "Array")
@JvmField public val UNIT: ClassName = ClassName("kotlin", "Unit")
@JvmField public val BOOLEAN: ClassName = ClassName("kotlin", "Boolean")
@JvmField public val BYTE: ClassName = ClassName("kotlin", "Byte")
@JvmField public val SHORT: ClassName = ClassName("kotlin", "Short")
@JvmField public val INT: ClassName = ClassName("kotlin", "Int")
@JvmField public val LONG: ClassName = ClassName("kotlin", "Long")
@JvmField public val CHAR: ClassName = ClassName("kotlin", "Char")
@JvmField public val FLOAT: ClassName = ClassName("kotlin", "Float")
@JvmField public val DOUBLE: ClassName = ClassName("kotlin", "Double")
@JvmField public val STRING: ClassName = ClassName("kotlin", "String")
@JvmField public val CHAR_SEQUENCE: ClassName = ClassName("kotlin", "CharSequence")
@JvmField public val COMPARABLE: ClassName = ClassName("kotlin", "Comparable")
@JvmField public val THROWABLE: ClassName = ClassName("kotlin", "Throwable")
@JvmField public val ANNOTATION: ClassName = ClassName("kotlin", "Annotation")
@JvmField public val NOTHING: ClassName = ClassName("kotlin", "Nothing")
@JvmField public val NUMBER: ClassName = ClassName("kotlin", "Number")
@JvmField public val ITERABLE: ClassName = ClassName("kotlin.collections", "Iterable")
@JvmField public val COLLECTION: ClassName = ClassName("kotlin.collections", "Collection")
@JvmField public val LIST: ClassName = ClassName("kotlin.collections", "List")
@JvmField public val SET: ClassName = ClassName("kotlin.collections", "Set")
@JvmField public val MAP: ClassName = ClassName("kotlin.collections", "Map")
@JvmField public val MAP_ENTRY: ClassName = MAP.nestedClass("Entry")
@JvmField public val MUTABLE_ITERABLE: ClassName = ClassName("kotlin.collections", "MutableIterable")
@JvmField public val MUTABLE_COLLECTION: ClassName = ClassName("kotlin.collections", "MutableCollection")
@JvmField public val MUTABLE_LIST: ClassName = ClassName("kotlin.collections", "MutableList")
@JvmField public val MUTABLE_SET: ClassName = ClassName("kotlin.collections", "MutableSet")
@JvmField public val MUTABLE_MAP: ClassName = ClassName("kotlin.collections", "MutableMap")
@JvmField public val MUTABLE_MAP_ENTRY: ClassName = MUTABLE_MAP.nestedClass("Entry")
@JvmField public val BOOLEAN_ARRAY: ClassName = ClassName("kotlin", "BooleanArray")
@JvmField public val BYTE_ARRAY: ClassName = ClassName("kotlin", "ByteArray")
@JvmField public val CHAR_ARRAY: ClassName = ClassName("kotlin", "CharArray")
@JvmField public val SHORT_ARRAY: ClassName = ClassName("kotlin", "ShortArray")
@JvmField public val INT_ARRAY: ClassName = ClassName("kotlin", "IntArray")
@JvmField public val LONG_ARRAY: ClassName = ClassName("kotlin", "LongArray")
@JvmField public val FLOAT_ARRAY: ClassName = ClassName("kotlin", "FloatArray")
@JvmField public val DOUBLE_ARRAY: ClassName = ClassName("kotlin", "DoubleArray")
@JvmField public val ENUM: ClassName = ClassName("kotlin", "Enum")
@JvmField public val U_BYTE: ClassName = ClassName("kotlin", "UByte")
@JvmField public val U_SHORT: ClassName = ClassName("kotlin", "UShort")
@JvmField public val U_INT: ClassName = ClassName("kotlin", "UInt")
@JvmField public val U_LONG: ClassName = ClassName("kotlin", "ULong")
@JvmField public val U_BYTE_ARRAY: ClassName = ClassName("kotlin", "UByteArray")
@JvmField public val U_SHORT_ARRAY: ClassName = ClassName("kotlin", "UShortArray")
@JvmField public val U_INT_ARRAY: ClassName = ClassName("kotlin", "UIntArray")
@JvmField public val U_LONG_ARRAY: ClassName = ClassName("kotlin", "ULongArray")
