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

package modulecheck.parsing.source

import modulecheck.utils.LazyDeferred
import org.jetbrains.kotlin.name.FqName

interface JvmFile {
  val name: String
  val packageFqName: String
  val imports: Set<String>
  val declarations: Set<DeclarationName>

  val wildcardImports: Set<String>
  val maybeExtraReferences: LazyDeferred<Set<String>>
}

interface KotlinFile : JvmFile {

  val apiReferences: LazyDeferred<Set<String>>

  fun getScopeArguments(
    allAnnotations: Set<String>,
    mergeAnnotations: Set<String>
  ): ScopeArgumentParseResult

  data class ScopeArgumentParseResult(
    val mergeArguments: Set<RawAnvilAnnotatedType>,
    val contributeArguments: Set<RawAnvilAnnotatedType>
  )
}

interface JavaFile : JvmFile {
  val apiReferences: Set<FqName>
}

enum class JavaVersion {
  VERSION_1_1,
  VERSION_1_2,
  VERSION_1_3,
  VERSION_1_4,
  VERSION_1_5,
  VERSION_1_6,
  VERSION_1_7,
  VERSION_1_8,
  VERSION_1_9,
  VERSION_1_10,
  VERSION_11,
  VERSION_12,
  VERSION_13,
  VERSION_14,
  VERSION_15,
  VERSION_16,
  VERSION_17,
  VERSION_18,
  VERSION_19,
  VERSION_20,
  VERSION_HIGHER;
}
