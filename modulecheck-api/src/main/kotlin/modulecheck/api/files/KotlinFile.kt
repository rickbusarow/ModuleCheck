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

package modulecheck.api.files

import modulecheck.api.JvmFile
import modulecheck.psi.DeclarationVisitor
import modulecheck.psi.ReferenceVisitor
import modulecheck.psi.UsedImportsVisitor
import org.jetbrains.kotlin.psi.KtFile
import org.jetbrains.kotlin.resolve.BindingContext

class KotlinFile(
  val ktFile: KtFile,
  val bindingContext: BindingContext
) : JvmFile() {

  override val name = ktFile.name

  override val packageFqName by lazy { ktFile.packageFqName.asString() }

  override val imports by lazy {

    usedImportsVisitor
      .usedImports()
      .mapNotNull { importDirective ->
        importDirective
          .importPath
          ?.pathStr
      }
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

  val apiReferences by lazy {

    val replacedWildcards = wildcardImports.flatMap { wildardImport ->

      referenceVisitor.apiReferences.map { apiReference ->
        wildardImport.replace("*", apiReference)
      }
    }
      .toSet()

    val simple = referenceVisitor.apiReferences + referenceVisitor.apiReferences.map {
      ktFile.packageFqName.asString() + "." + it
    }

    val imported = imports.filter { imp ->
      referenceVisitor.apiReferences.any { ref ->
        imp.endsWith(ref)
      }
    }

    imported + simple + replacedWildcards
  }

  private val usedImportsVisitor by lazy {
    UsedImportsVisitor(bindingContext)
      .also { ktFile.accept(it) }
  }

  private val referenceVisitor by lazy {
    ReferenceVisitor()
      .also { ktFile.accept(it) }
  }

  private val typeReferences by lazy {
    referenceVisitor.typeReferences
      .mapNotNull { tr ->
        CHILD_PARAMETERS_REGEX.find(tr)?.value
      }
      .filterNot { it in imports }
      .toSet()
  }

  private val callableReferences by lazy {
    referenceVisitor.callableReferences
      .mapNotNull { tr ->
        CHILD_PARAMETERS_REGEX.find(tr)?.value
      }
      .toSet()
  }

  private val qualifiedExpressions by lazy {
    referenceVisitor.qualifiedExpressions
      .mapNotNull { tr ->
        CHILD_PARAMETERS_REGEX.find(tr)?.value
      }
      .toSet()
  }

  override val maybeExtraReferences by lazy {

    val allOther = typeReferences + callableReferences + qualifiedExpressions

    allOther + allOther.map {
      ktFile.packageFqName.asString() + "." + it
    } + wildcardImports.flatMap { wi ->

      allOther.map { tr ->
        wi.replace("*", tr)
      }
    }
      .toSet()
  }

  companion object {
    private val CHILD_PARAMETERS_REGEX = """^[a-zA-Z\d._`]*""".toRegex()
  }
}
