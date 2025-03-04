/*
 * Copyright (C) 2021-2025 Rick Busarow
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

package modulecheck.parsing.kotlin.compiler.internal

import java.io.File

/**
 * @return true if the file exists in the Java file system
 *   and has an extension of `.kt` or `.kts`, otherwise false
 */
fun File.isKotlinFile(): Boolean = exists() && extension.let { it == "kt" || it == "kts" }

/**
 * @return true if the file exists in the Java file system
 *   and has an extension of `.kts`, otherwise false
 */
fun File.isKotlinScriptFile(): Boolean = exists() && extension == "kts"

/**
 * @return true if the file exists in the Java file
 *   system and has an extension of `.kt`, otherwise false
 */
fun File.isKtFile(): Boolean = exists() && extension == "kt"
