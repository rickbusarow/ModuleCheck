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

package modulecheck.gradle.internal

import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.psi.KtImportDirective
import org.jetbrains.kotlin.resolve.ImportPath

fun createImport(fqname: String) =
  psiElementFactory.createImportDirective(ImportPath(FqName(fqname), false))

fun List<KtImportDirective>.sortWeighted(): List<KtImportDirective> =
  sortedWith(compareBy({ it.isAlias }, { it.isJava }, { it.text }))

val KtImportDirective.isAlias: Boolean
  get() = (text.contains(" as "))

val KtImportDirective.isJava: Boolean
  get() = importedFqName?.asString()
    ?.let { it.startsWith("java") || it.startsWith("kotlin.") } ?: false
