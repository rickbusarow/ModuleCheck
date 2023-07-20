/*
 * Copyright (C) 2021-2023 Rick Busarow
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

/**
 * @property version the Anvil version. It is always a semantic
 *   version, but it may be suffixed with a Kotlin version.
 * @property generateDaggerFactories if true, Anvil will
 *   generate Dagger's factories in addition to the binding code.
 */
data class AnvilGradlePlugin(
  val version: SemVer,
  val generateDaggerFactories: Boolean
)

data class AnvilAnnotatedType(
  val contributedTypeDeclaration: QualifiedDeclaredName,
  val contributedScope: AnvilScopeName
)

data class RawAnvilAnnotatedType(
  val declaredName: QualifiedDeclaredName,
  val anvilScopeNameEntry: AnvilScopeNameEntry
)

data class AnvilScopeName(val fqName: FqName) {
  override fun toString(): String = fqName.asString()
}

data class AnvilScopeNameEntry(val name: ReferenceName)
