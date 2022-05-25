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
import modulecheck.utils.lazy.unsafeLazy
import org.jetbrains.kotlin.psi.KtAnnotated
import org.jetbrains.kotlin.psi.KtCallableDeclaration
import org.jetbrains.kotlin.psi.KtFunction
import org.jetbrains.kotlin.psi.KtLiteralStringTemplateEntry
import org.jetbrains.kotlin.psi.KtNamedDeclaration
import org.jetbrains.kotlin.psi.KtParameter
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

/**
 * Returns any custom names defined by `@JvmName(...)`, the default setter/getter names if it's a
 * property, or the same names as used by Kotlin for anything else.
 */
@Suppress("ComplexMethod")
internal fun KtNamedDeclaration.jvmSimpleNames(): Set<String> {

  val identifier = nameAsSafeName.identifier

  val isPrefixMatchOrNull by unsafeLazy {
    // matches a name which starts with `is`, followed by something other than a lowercase letter.
    """^is([^a-z].*)""".toRegex().find(identifier)
  }

  return when (this) {
    is KtFunction -> {
      setOf(jvmNameOrNull() ?: isPrefixMatchOrNull?.value ?: nameAsSafeName.asString())
    }

    is KtProperty,
    is KtParameter -> {

      // const properties can't have JvmName annotations
      if (isConst()) return emptySet()

      // a Kotlin property `isAProperty` has a java setter of `setAProperty(...)`
      fun isPrefixSetterOrNull() = isPrefixMatchOrNull?.let {
        "set${it.destructured.component1()}"
      }

      val suffix by unsafeLazy { nameAsSafeName.identifier.capitalize() }

      buildSet {

        val get = (this@jvmSimpleNames as? KtProperty)?.getter?.jvmNameOrNull()
          ?: isPrefixMatchOrNull?.value
          ?: "get$suffix"

        add(get)

        val mutable = (this@jvmSimpleNames as? KtProperty)?.isVar
          ?: (this@jvmSimpleNames as KtParameter).isMutable

        if (mutable) {
          val set = (this@jvmSimpleNames as? KtProperty)?.setter?.jvmNameOrNull()
            ?: isPrefixSetterOrNull()
            ?: "set$suffix"

          add(set)
        }
      }
    }
    else -> {
      setOf(nameAsSafeName.asString())
    }
  }
}
