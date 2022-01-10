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

@file:Suppress("TooManyFunctions")

package modulecheck.parsing.psi.internal

import org.intellij.lang.annotations.Language
import org.jetbrains.kotlin.idea.KotlinLanguage
import org.jetbrains.kotlin.incremental.isKotlinFile
import org.jetbrains.kotlin.psi.KtFile
import java.io.File
import java.io.FileNotFoundException

fun Collection<File>.ktFiles(): List<KtFile> = asSequence()
  .mapNotNull { it.asKtFileOrNull() }
  .toList()

fun File.isKotlinFile(): Boolean = exists() && isKotlinFile(listOf("kts", "kt"))
fun File.isKotlinScriptFile(): Boolean = exists() && isKotlinFile(listOf("kts"))
fun File.isKtFile(): Boolean = exists() && isKotlinFile(listOf("kt"))

fun File.asKotlinScriptFileOrNull(): KtFile? =
  if (exists() && isKotlinScriptFile()) asKtFile() else null

fun File.asKtFileOrNull(): KtFile? =
  if (exists() && isKtFile()) asKtFile() else null

fun File.asKtFile(): KtFile =
  (psiFileFactory.createFileFromText(name, KotlinLanguage.INSTANCE, readText()) as? KtFile)
    ?: throw FileNotFoundException("could not find file $this")

fun KtFile(
  @Language("kotlin")
  content: String
): KtFile = KtFile(name = "Source.kt", content = content)

fun KtFile(
  name: String,
  @Language("kotlin")
  content: String
): KtFile = psiFileFactory.createFileFromText(
  name,
  KotlinLanguage.INSTANCE,
  content.trimIndent()
) as KtFile
