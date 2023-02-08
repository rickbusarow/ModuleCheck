/*
 * Copyright (C) 2021-2023 Rick Busarow
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

import org.jetbrains.kotlin.com.intellij.psi.PsiElement
import org.jetbrains.kotlin.psi.KtTreeVisitorVoid

internal fun everythingPrinter() = object : KtTreeVisitorVoid() {

  private val parentNameMap = mutableMapOf<PsiElement, String>()

  override fun visitElement(element: PsiElement) {

    val thisName = element::class.java.simpleName // + element.extendedTypes()
    val parentName = element.parentName()

    println(
      """ ******************************** -- $thisName  -- parent: $parentName
      |${element.text}
      |_________________________________________________________________________________
      """.trimMargin()
    )
    super.visitElement(element)
  }

  private fun PsiElement.parentName() = parent?.let { parent ->

    parentNameMap.getOrPut(parent) {
      val typeCount = parentNameMap.keys.count { it::class == parent::class }

      val simpleName = parent::class.java.simpleName

      val start = if (typeCount == 0) {
        simpleName
      } else {
        "$simpleName (${typeCount + 1})"
      }

      start // + parent.extendedTypes()
    }
  }

  @Suppress("UnusedPrivateMember")
  private fun PsiElement.extendedTypes(): String {
    return this::class.supertypes
      .ifEmpty { return "" }
      .joinToString(prefix = " [ ", postfix = " ] ") { kType ->
        kType.toString()
          .replace("""org\.jetbrains[a-zA-Z\\.]*\.psi\.(?:stubs\.)?""".toRegex(), "")
          .replace("org.jetbrains.kotlin.com.intellij.navigation.", "")
          .replace("impl.source.tree.", "")
      }
  }
}

internal fun PsiElement.printEverything() {
  accept(everythingPrinter())
}
