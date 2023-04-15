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

package modulecheck.parsing.kotlin.compiler

import org.jetbrains.kotlin.com.intellij.psi.PsiJavaFile
import org.jetbrains.kotlin.psi.KtFile
import java.io.File
import java.io.FileNotFoundException

/**
 * Creates Kotlin and Java Psi files using a shared [KotlinEnvironment]
 */
interface McPsiFileFactory {

  /**
   * @return a Psi `KtFile` for Kotlin files. The file extension must be `.kt` or `.kts`.
   * @throws IllegalArgumentException if the [file] is an unsupported extension
   * @throws FileNotFoundException if the [file] does not exist in the Java file system
   */
  suspend fun createKotlin(file: File): KtFile

  /**
   * @return a Psi `PsiJavaFile` for Java files. The file extension must be `.java`.
   * @throws IllegalArgumentException if the [file] is an unsupported extension
   * @throws FileNotFoundException if the [file] does not exist in the Java file system
   */
  suspend fun createJava(file: File): PsiJavaFile

  /** Creates an instance of [McPsiFileFactory] */
  fun interface Factory {

    /**
     * @return an instance of [McPsiFileFactory] for the given [kotlinEnvironment]
     */
    fun create(kotlinEnvironment: KotlinEnvironment): McPsiFileFactory
  }
}
