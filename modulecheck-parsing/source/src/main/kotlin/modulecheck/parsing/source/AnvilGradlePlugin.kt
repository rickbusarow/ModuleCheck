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

import net.swiftzer.semver.SemVer
import org.jetbrains.kotlin.name.FqName

data class AnvilGradlePlugin(
  val version: SemVer,
  val generateDaggerFactories: Boolean
)

data class AnvilAnnotatedType(
  val contributedTypeDeclaration: DeclarationName,
  val contributedScope: AnvilScopeName
)

data class RawAnvilAnnotatedType(
  val declarationName: DeclarationName,
  val anvilScopeNameEntry: AnvilScopeNameEntry
)

data class AnvilScopeName(val fqName: FqName) {
  override fun toString(): String = fqName.asString()
}

data class AnvilScopeNameEntry(val name: String)
