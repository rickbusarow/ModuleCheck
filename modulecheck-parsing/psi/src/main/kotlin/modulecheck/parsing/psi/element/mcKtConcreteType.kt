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

import modulecheck.parsing.source.element.McFile.McKtFile
import modulecheck.parsing.source.element.McKtElement
import modulecheck.parsing.source.element.McType.McConcreteType.McKtConcreteType
import org.jetbrains.kotlin.psi.KtClass
import org.jetbrains.kotlin.psi.KtClassOrObject
import org.jetbrains.kotlin.psi.KtElement
import org.jetbrains.kotlin.psi.KtObjectDeclaration
import org.jetbrains.kotlin.psi.psiUtil.getChildrenOfType

internal fun McKtConcreteType(
  parsingContext: ParsingContext,
  containingFile: McKtFile,
  clazz: KtClassOrObject,
  parent: McKtElement
): McKtConcreteType? =
  when (clazz) {
    is KtClass -> if (clazz.isInterface()) {
      RealMcKtInterface(
        parsingContext = parsingContext,
        containingFile = containingFile,
        psi = clazz,
        parent = parent
      )
    } else {
      RealMcKtClass(
        parsingContext = parsingContext,
        containingFile = containingFile,
        psi = clazz,
        parent = parent
      )
    }

    is KtObjectDeclaration -> if (clazz.isCompanion()) {
      RealMcKtCompanionObject(
        parsingContext = parsingContext,
        containingFile = containingFile,
        psi = clazz,
        parent = parent
      )
    } else {
      require(!clazz.isObjectLiteral())
      RealMcKtObject(
        parsingContext = parsingContext,
        containingFile = containingFile,
        psi = clazz,
        parent = parent
      )
    }

    else -> null
  }

internal fun KtElement.mcKtConcreteTypesDirect(
  parsingContext: ParsingContext,
  containingFile: McKtFile,
  parent: McKtElement
): Set<McKtConcreteType> {

  return getChildrenOfType<KtClassOrObject>()
    .mapNotNull { clazz ->

      McKtConcreteType(
        parsingContext = parsingContext,
        containingFile = containingFile,
        clazz = clazz,
        parent = parent
      )
    }.toSet()
}
