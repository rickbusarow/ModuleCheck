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

import modulecheck.parsing.element.Declared
import modulecheck.parsing.element.HasKtVisibility
import modulecheck.parsing.element.McKtDeclaredElement
import modulecheck.parsing.element.McVisibility
import modulecheck.parsing.psi.internal.requireSimpleName
import modulecheck.parsing.source.DeclaredName
import modulecheck.parsing.source.HasPackageName
import modulecheck.parsing.source.HasSimpleNames
import modulecheck.parsing.source.PackageName
import modulecheck.parsing.source.SimpleName
import modulecheck.parsing.source.asDeclaredName
import modulecheck.utils.lazy.unsafeLazy
import org.jetbrains.kotlin.lexer.KtTokens
import org.jetbrains.kotlin.psi.KtModifierListOwner

internal class VisibilityDelegate(psi: KtModifierListOwner) : HasKtVisibility {
  override val visibility: McVisibility.McKtVisibility by lazy {
    when {
      psi.hasModifier(KtTokens.PRIVATE_KEYWORD) -> McVisibility.Private
      psi.hasModifier(KtTokens.INTERNAL_KEYWORD) -> McVisibility.McKtVisibility.Internal
      psi.hasModifier(KtTokens.PROTECTED_KEYWORD) -> McVisibility.Protected
      psi.hasModifier(KtTokens.PUBLIC_KEYWORD) -> McVisibility.Public
      else -> McVisibility.Public
    }
  }
}

internal class DeclaredDelegate<T : KtModifierListOwner, E>(
  psi: T,
  parent: E
) : Declared where E : McKtDeclaredElement,
                   E : HasPackageName,
                   E : HasSimpleNames {

  override val packageName: PackageName by unsafeLazy { parent.packageName }
  override val simpleNames: List<SimpleName> by unsafeLazy {

    println(
      """ ~~~~~~~~~~~
      |parent --------- $parent
      |parent simple -- ${parent.simpleNames}
      |psi simple ----- ${psi.requireSimpleName()}
      """.trimMargin()
    )
    parent.simpleNames + psi.requireSimpleName()
  }
  override val declaredName: DeclaredName by unsafeLazy {
    simpleNames.asDeclaredName(packageName)
  }
}
