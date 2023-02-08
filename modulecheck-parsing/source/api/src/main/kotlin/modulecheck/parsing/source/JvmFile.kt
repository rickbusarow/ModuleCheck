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

package modulecheck.parsing.source

import kotlinx.serialization.Serializable
import modulecheck.utils.lazy.LazyDeferred
import org.jetbrains.kotlin.com.intellij.psi.PsiFile
import org.jetbrains.kotlin.com.intellij.psi.PsiJavaFile
import org.jetbrains.kotlin.psi.KtFile
import java.io.File

@Serializable
sealed interface JvmFile : HasReferences {
  /**
   * The [java.io.File] version of this file
   *
   * @since 0.12.0
   */
  val file: File

  /**
   * The simple name of this file, with extension. Like `App.java` or `App.kt`.
   *
   * @since 0.12.0
   */
  val name: String

  /**
   * the package name of this file, or [PackageName.DEFAULT] if a package is not declared
   *
   * @since 0.12.0
   */
  val packageName: PackageName

  /**
   * All declared names within this file
   *
   * @since 0.12.0
   */
  val declarations: Set<QualifiedDeclaredName>

  /**
   * The Kotlin compiler version of this file. It will either be a [KtFile] or [PsiJavaFile]
   *
   * @since 0.12.0
   */
  val psi: PsiFile

  val importsLazy: Lazy<Set<ReferenceName>>
  val apiReferences: LazyDeferred<Set<ReferenceName>>
}

interface KotlinFile : JvmFile {
  override val psi: KtFile

  /**
   * A weird, dated function for getting Anvil scope arguments
   *
   * @since 0.12.0
   */
  suspend fun getAnvilScopeArguments(
    allAnnotations: List<ReferenceName>,
    mergeAnnotations: List<ReferenceName>
  ): ScopeArgumentParseResult

  data class ScopeArgumentParseResult(
    val mergeArguments: Set<RawAnvilAnnotatedType>,
    val contributeArguments: Set<RawAnvilAnnotatedType>
  )
}

interface JavaFile : JvmFile {
  /**
   * The [PsiJavaFile] version of this file
   *
   * @since 0.12.0
   */
  override val psi: PsiJavaFile
}
