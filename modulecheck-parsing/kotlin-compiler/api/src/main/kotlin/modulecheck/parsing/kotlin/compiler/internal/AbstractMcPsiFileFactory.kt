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

package modulecheck.parsing.kotlin.compiler.internal

import modulecheck.parsing.kotlin.compiler.McPsiFileFactory
import modulecheck.utils.lazy.LazyDeferred
import org.jetbrains.kotlin.cli.jvm.compiler.KotlinCoreEnvironment
import org.jetbrains.kotlin.com.intellij.psi.PsiFile
import org.jetbrains.kotlin.com.intellij.psi.PsiJavaFile
import org.jetbrains.kotlin.incremental.isJavaFile
import org.jetbrains.kotlin.psi.KtFile
import java.io.File
import java.io.FileNotFoundException

/**
 * Base class for an [McPsiFileFactory] implementation
 *
 * @since 0.13.0
 */
abstract class AbstractMcPsiFileFactory : McPsiFileFactory {

  /**
   * wrapper around "core" settings like Kotlin version, source files, and classpath files (external
   * dependencies)
   *
   * @since 0.13.0
   */
  abstract val coreEnvironment: LazyDeferred<KotlinCoreEnvironment>

  protected abstract suspend fun create(file: File): PsiFile

  override suspend fun createKotlin(file: File): KtFile {
    if (!file.exists()) throw FileNotFoundException("could not find file $file")
    if (!file.isKotlinFile()) {
      throw IllegalArgumentException(
        "the file's extension must be either `.kt` or `.kts`, but it was `${file.extension}`."
      )
    }

    return create(file) as KtFile
  }

  override suspend fun createJava(file: File): PsiJavaFile {

    if (!file.isJavaFile()) {
      throw IllegalArgumentException(
        "the file's extension must be `.java`, but it was `${file.extension}`."
      )
    }

    return create(file) as PsiJavaFile
  }
}
