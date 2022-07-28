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

package modulecheck.parsing.kotlin.compiler.internal

import org.jetbrains.kotlin.incremental.isKotlinFile
import java.io.File

/**
 * @return true if the file exists in the Java file system and has an extension of `.kt` or `.kts`,
 *   otherwise false
 * @since 0.13.0
 */
fun File.isKotlinFile(): Boolean = exists() && isKotlinFile(listOf("kts", "kt"))

/**
 * @return true if the file exists in the Java file system and has an extension of `.kts`, otherwise
 *   false
 * @since 0.13.0
 */
fun File.isKotlinScriptFile(): Boolean = exists() && isKotlinFile(listOf("kts"))

/**
 * @return true if the file exists in the Java file system and has an extension of `.kt`, otherwise
 *   false
 * @since 0.13.0
 */
fun File.isKtFile(): Boolean = exists() && isKotlinFile(listOf("kt"))
