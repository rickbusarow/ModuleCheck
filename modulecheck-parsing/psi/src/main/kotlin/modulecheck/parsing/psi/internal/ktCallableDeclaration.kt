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

package modulecheck.parsing.psi.internal

import modulecheck.parsing.psi.FqNames
import modulecheck.utils.capitalize
import modulecheck.utils.unsafeLazy
import org.jetbrains.kotlin.psi.KtAnnotated
import org.jetbrains.kotlin.psi.KtCallableDeclaration
import org.jetbrains.kotlin.psi.KtFunction
import org.jetbrains.kotlin.psi.KtLiteralStringTemplateEntry
import org.jetbrains.kotlin.psi.KtNamedDeclaration
import org.jetbrains.kotlin.psi.KtProperty
import org.jetbrains.kotlin.psi.KtPropertyAccessor
import org.jetbrains.kotlin.psi.KtValueArgumentList

fun KtCallableDeclaration.isJvmStatic(): Boolean {
  return hasAnnotation(FqNames.jvmStatic)
}

fun KtProperty.isJvmField(): Boolean {
  return hasAnnotation(FqNames.jvmField)
}

fun KtFunction.jvmNameOrNull(): String? = annotatedJvmNameOrNull()

fun KtPropertyAccessor.jvmNameOrNull(): String? = annotatedJvmNameOrNull()

private fun KtAnnotated.annotatedJvmNameOrNull(): String? {
  return annotationEntries
    .firstOrNull { it.shortName?.asString() == "JvmName" }
    ?.getChildrenOfTypeRecursive<KtValueArgumentList>()
    ?.single()
    ?.getChildrenOfTypeRecursive<KtLiteralStringTemplateEntry>()
    ?.single()
    ?.text
}

internal fun KtPropertyAccessor.jvmSimpleName(): String {
  jvmNameOrNull()?.let { return it }

  val prefix = if (isGetter) "get" else "set"

  val suffix = property.nameAsSafeName.identifier.capitalize()

  return prefix + suffix
}

/**
 * Returns any custom names defined by `@JvmName(...)`, the default setter/getter names if it's a
 * property, or the same names as used by Kotlin for anything else.
 */
internal fun KtNamedDeclaration.jvmSimpleNames(): Set<String> {
  return when (this) {
    is KtFunction -> {
      setOf(jvmNameOrNull() ?: nameAsSafeName.asString())
    }
    is KtProperty -> {

      // const properties can't have JvmName annotations
      if (isConst()) return emptySet()

      val suffix by unsafeLazy { nameAsSafeName.identifier.capitalize() }

      setOfNotNull(
        getter?.jvmNameOrNull() ?: "get$suffix",
        if (isVar) {
          setter?.jvmNameOrNull() ?: "set$suffix"
        } else null
      )
    }
    else -> {
      setOf(nameAsSafeName.asString())
    }
  }
}
