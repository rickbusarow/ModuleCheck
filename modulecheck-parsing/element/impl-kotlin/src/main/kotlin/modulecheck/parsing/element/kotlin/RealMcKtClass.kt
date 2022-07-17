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
import modulecheck.parsing.element.McFile.McKtFile
import modulecheck.parsing.element.McFunction.McKtFunction
import modulecheck.parsing.element.McKtElement
import modulecheck.parsing.element.McProperty.McKtProperty
import modulecheck.parsing.element.McType
import modulecheck.parsing.element.McType.McConcreteType.McKtConcreteType
import modulecheck.parsing.element.McType.McConcreteType.McKtConcreteType.McKtClass
import modulecheck.parsing.element.McType.McConcreteType.McKtConcreteType.McKtCompanionObject
import modulecheck.parsing.element.McType.McConcreteType.McKtConcreteType.McKtInterface
import modulecheck.parsing.element.McType.McConcreteType.McKtConcreteType.McKtObject
import modulecheck.parsing.element.McType.McConcreteType.McKtType
import modulecheck.parsing.element.resolve.ParsingContext
import modulecheck.parsing.source.DeclaredName
import modulecheck.parsing.source.PackageName
import modulecheck.parsing.source.SimpleName
import modulecheck.parsing.source.SimpleName.Companion.stripPackageNameFromFqName
import modulecheck.utils.lazy.LazySet
import modulecheck.utils.lazy.lazySet
import modulecheck.utils.lazy.unsafeLazy
import modulecheck.utils.mapToSet
import modulecheck.utils.requireNotNull
import org.jetbrains.kotlin.com.intellij.psi.PsiElement
import org.jetbrains.kotlin.psi.KtClass
import org.jetbrains.kotlin.psi.KtClassOrObject
import org.jetbrains.kotlin.psi.KtObjectDeclaration

abstract class AbstractMcKtConcreteType internal constructor(
  private val parsingContext: ParsingContext<PsiElement>,
  override val containingFile: McKtFile,
  override val psi: KtClassOrObject
) : McKtConcreteType,
  McKtType {
  override val simpleNames: List<SimpleName> by unsafeLazy {
    psi.fqName.requireNotNull()
      .asString()
      .stripPackageNameFromFqName(containingFile.packageName)
  }
  override val declaredName: DeclaredName by lazy {
    DeclaredName.kotlin(
      containingFile.packageName,
      simpleNames
    )
  }

  override val innerTypes: LazySet<McKtConcreteType> = lazySet {
    psi.body
      ?.mcKtConcreteTypesDirect(parsingContext, containingFile, parent)
      .orEmpty()
  }
  override val annotations: LazySet<McAnnotation> = lazySet {
    psi.annotations(parsingContext, this)
  }
  override val innerTypesRecursive: LazySet<McKtConcreteType> = lazySet {
    innerTypes.fold(setOf()) { acc, type ->
      acc + type + type.innerTypesRecursive.toSet()
    }
  }
  override val properties: LazySet<McKtProperty> = lazySet {
    psi.body?.properties
      .orEmpty()
      .mapToSet {
        RealMcKtMemberProperty(
          parsingContext = parsingContext, psi = it, parent = this
        )
      }
      .plus(
        psi.primaryConstructor
          ?.valueParameters
          .orEmpty()
          .filter { it.hasValOrVar() }
          .map { RealMcKtConstructorProperty(parsingContext, it, this) }
      )
  }
  override val functions: LazySet<McKtFunction> = lazySet {
    psi.body?.functions
      .orEmpty()
      .mapToSet {
        RealMcKtFunction(parsingContext = parsingContext, psi = it, parent = this)
      }
  }
  override val superTypes: LazySet<McType> = lazySet { TODO("Not yet implemented") }
  override val typeParameters: LazySet<McType.McTypeParameter> = lazySet {
    TODO("Not yet implemented")
  }
  override val packageName: PackageName
    get() = containingFile.packageName

  final override fun toString(): String {
    return "${this::class.java.simpleName}(name = `${declaredName.name}`, " +
      "containingFile=${containingFile.file.path}, " +
      "psi=${psi::class.simpleName})"
  }
}

data class RealMcKtClass(
  private val parsingContext: ParsingContext<PsiElement>,
  override val containingFile: McKtFile,
  override val psi: KtClass,
  override val parent: McKtElement
) : AbstractMcKtConcreteType(parsingContext, containingFile, psi), McKtClass {
  override val primaryConstructor: McKtFunction?
    get() = TODO("Not yet implemented")
  override val constructors: LazySet<McKtFunction>
    get() = TODO("Not yet implemented")
}

data class RealMcKtInterface(
  private val parsingContext: ParsingContext<PsiElement>,
  override val containingFile: McKtFile,
  override val psi: KtClass,
  override val parent: McKtElement
) : AbstractMcKtConcreteType(parsingContext, containingFile, psi), McKtInterface

data class RealMcKtCompanionObject(
  private val parsingContext: ParsingContext<PsiElement>,
  override val containingFile: McKtFile,
  override val psi: KtObjectDeclaration,
  override val parent: McKtElement
) : AbstractMcKtConcreteType(parsingContext, containingFile, psi), McKtCompanionObject

data class RealMcKtObject(
  private val parsingContext: ParsingContext<PsiElement>,
  override val containingFile: McKtFile,
  override val psi: KtObjectDeclaration,
  override val parent: McKtElement
) : AbstractMcKtConcreteType(parsingContext, containingFile, psi), McKtObject
