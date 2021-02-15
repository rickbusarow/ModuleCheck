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

package modulecheck.psi

import modulecheck.psi.internal.isPrivateOrInternal
import org.jetbrains.kotlin.psi.KtNamedDeclaration
import org.jetbrains.kotlin.psi.KtTreeVisitorVoid

class DeclarationVisitor : KtTreeVisitorVoid() {

  val declarations: MutableSet<String> = mutableSetOf()

  override fun visitNamedDeclaration(declaration: KtNamedDeclaration) {
    if (!declaration.isPrivateOrInternal()) {
      declaration.fqName?.let { declarations.add(it.asString()) }
    }

    super.visitNamedDeclaration(declaration)
  }
}
