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

package modulecheck.parsing.psi

import modulecheck.parsing.psi.internal.getChildrenOfTypeRecursive
import modulecheck.parsing.psi.internal.nameSafe
import org.jetbrains.kotlin.com.intellij.psi.PsiElement
import org.jetbrains.kotlin.psi.KtCallExpression
import org.jetbrains.kotlin.psi.psiUtil.parents

fun PsiElement.inBuildscript(): Boolean {
  return parents.filterIsInstance<KtCallExpression>()
    .any { it.nameSafe() == "buildscript" }
}

fun PsiElement.androidBlocks(): List<KtCallExpression> {
  return getChildrenOfTypeRecursive<KtCallExpression>()
    .filter { it.nameSafe() == "android" }
}

fun PsiElement.buildFeaturesBlocks(): List<KtCallExpression> {

  val nested = getChildrenOfTypeRecursive<KtCallExpression>()
    .filter { it.nameSafe() == "buildFeatures" }

  val qualified = getChildrenOfTypeRecursive<KtCallExpression>()
    .filter {
      it.nameSafe()?.matches("android\\s*\\.\\s*buildFeatures".toRegex()) == true
    }

  return nested + qualified
}
