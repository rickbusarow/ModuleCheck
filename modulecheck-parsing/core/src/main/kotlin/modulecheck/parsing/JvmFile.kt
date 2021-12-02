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

package modulecheck.parsing

import modulecheck.project.AndroidMcProject
import modulecheck.project.DeclarationName
import modulecheck.project.McProject
import modulecheck.utils.LazyDeferred
import modulecheck.utils.lazyDeferred

abstract class JvmFile(private val project: McProject) {
  abstract val name: String
  abstract val packageFqName: String
  abstract val imports: Set<String>
  abstract val declarations: Set<DeclarationName>

  override fun toString(): String {
    return """${this::class.simpleName}(
      |packageFqName='$packageFqName',
      |
      |importDirectives=$imports,
      |
      |declarations=$declarations
      |
      |)""".trimMargin()
  }

  abstract val wildcardImports: Set<String>
  abstract val maybeExtraReferences: LazyDeferred<Set<String>>

  val androidRReferences = lazyDeferred {
    val rFqName = (project as? AndroidMcProject)
      ?.androidRFqNameOrNull
      ?: return@lazyDeferred emptySet<String>()

    val packagePrefix = (project as? AndroidMcProject)
      ?.androidPackageOrNull
      ?.let { "$it." }
      ?: return@lazyDeferred emptySet<String>()

    maybeExtraReferences.await()
      .filter { it.startsWith(rFqName) }
      .plus(imports.filter { it.startsWith(rFqName) })
      .map { it.removePrefix(packagePrefix) }
      .toSet()
  }

  companion object
}
