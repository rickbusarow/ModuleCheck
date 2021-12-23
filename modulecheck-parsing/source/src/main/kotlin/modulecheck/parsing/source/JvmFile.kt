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

  fun getScopeArguments(
    allAnnotations: Set<String>,
    mergeAnnotations: Set<String>
  ): ScopeArgumentParseResult

  data class ScopeArgumentParseResult(
    val mergeArguments: Set<RawAnvilAnnotatedType>,
    val contributeArguments: Set<RawAnvilAnnotatedType>
  )
}

interface KotlinFile : JvmFile {

  val apiReferences: LazyDeferred<Set<String>>
  val constructorInjectedTypes: LazyDeferred<Set<FqName>>
  val memberInjectedTypes: LazyDeferred<Set<FqName>>
  val componentBindingReferences: LazyDeferred<List<AnvilBindingReference>>
  val moduleBindingReferences: LazyDeferred<Set<AnvilBindingReference>>
  val boundTypes: LazyDeferred<Set<AnvilBoundType>>

  val simpleBoundTypes: LazyDeferred<Set<AnvilBoundType>>
}

interface JavaFile : JvmFile
