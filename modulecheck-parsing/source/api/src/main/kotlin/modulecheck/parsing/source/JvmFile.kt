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

import modulecheck.utils.lazy.LazyDeferred

sealed interface JvmFile : HasReferences {
  val name: String
  val packageName: PackageName

  /** All declared names within this file */
  val declarations: Set<QualifiedDeclaredName>

  val importsLazy: Lazy<Set<ReferenceName>>
  val apiReferences: LazyDeferred<Set<ReferenceName>>
}

interface KotlinFile : JvmFile {

  /** A weird, dated function for getting Anvil scope arguments */
  suspend fun getAnvilScopeArguments(
    allAnnotations: List<ReferenceName>,
    mergeAnnotations: List<ReferenceName>
  ): ScopeArgumentParseResult

  data class ScopeArgumentParseResult(
    val mergeArguments: Set<RawAnvilAnnotatedType>,
    val contributeArguments: Set<RawAnvilAnnotatedType>
  )
}

interface JavaFile : JvmFile
