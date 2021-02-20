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

package modulecheck.core.internal

import modulecheck.core.files.JavaFile
import modulecheck.core.files.KotlinFile
import modulecheck.psi.internal.asKtFile
import modulecheck.psi.internal.asKtFileOrNull
import org.jetbrains.kotlin.incremental.isJavaFile
import org.jetbrains.kotlin.incremental.isKotlinFile
import org.jetbrains.kotlin.resolve.BindingContext
import java.io.File

fun Sequence<File>.ktFiles() = filter { it.isFile }
  .mapNotNull { it.asKtFileOrNull() }

fun File.jvmFiles(bindingContext: BindingContext) = walkTopDown()
  .files()
  .mapNotNull { file ->
    when {
      file.isKotlinFile(listOf("kt")) -> {
        KotlinFile(
          file.asKtFile(),
          bindingContext
        )
      }
      file.isJavaFile() -> JavaFile(file)
      else -> null
    }
  }.toList()

fun Collection<File>.jvmFiles(
  bindingContext: BindingContext
) = flatMap { it.jvmFiles(bindingContext) }

fun FileTreeWalk.dirs(): Sequence<File> = asSequence().filter { it.isDirectory }
fun FileTreeWalk.files(): Sequence<File> = asSequence().filter { it.isFile }
