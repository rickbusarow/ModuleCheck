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

package modulecheck.parsing.element.resolve

import modulecheck.parsing.element.McFile
import modulecheck.parsing.element.resolve.NameParser2.NameParser2Packet
import modulecheck.parsing.kotlin.compiler.KotlinEnvironment
import modulecheck.parsing.source.McName.CompatibleLanguage
import modulecheck.parsing.source.QualifiedDeclaredName
import modulecheck.parsing.source.ReferenceName
import modulecheck.utils.lazy.LazyDeferred
import modulecheck.utils.lazy.lazyDeferred

/**
 * Provides a context for parsing and resolving elements in a module check system.
 * This class is designed to work with any type `T` that represents a symbol in the
 * system. It uses a [NameParser2] to parse names, a [SymbolResolver] to resolve
 * symbols, and a [KotlinEnvironment] to provide a context for Kotlin language features.
 *
 * @property nameParser The parser used to parse names in the system.
 * @property symbolResolver The resolver used to resolve symbols in the system.
 * @property language The language that is compatible with the system.
 * @property kotlinEnvironmentDeferred A deferred [KotlinEnvironment]
 *   that provides a context for Kotlin language features.
 * @property stdLibNameOrNull A function that takes a [ReferenceName] and returns a
 *   [QualifiedDeclaredName] from the standard library, or null if no such name exists.
 */
class McElementContext<T>(
  val nameParser: NameParser2,
  val symbolResolver: SymbolResolver<T>,
  val language: CompatibleLanguage,
  val kotlinEnvironmentDeferred: LazyDeferred<KotlinEnvironment>,
  val stdLibNameOrNull: ReferenceName.() -> QualifiedDeclaredName?
) {

  /**
   * A deferred binding context obtained from the [KotlinEnvironment].
   * This context is used to resolve bindings in the system.
   */
  val bindingContextDeferred = lazyDeferred {
    kotlinEnvironmentDeferred.await()
      .bindingContextDeferred.await()
  }

  /**
   * Resolves the declared name of a symbol in the system. This method is not yet implemented.
   *
   * @param symbol The symbol whose declared name is to be resolved.
   * @return The declared name of the symbol, or null if the symbol does not have a declared name.
   */
  suspend fun declaredNameOrNull(symbol: T): QualifiedDeclaredName? {
    TODO()
  }

  /**
   * Resolves a reference name in a given file. This method
   * uses the [nameParser] to parse the reference name.
   *
   * @param file The file in which the reference name is to be resolved.
   * @param toResolve The reference name to resolve.
   * @return The resolved reference name, or null if the reference name could not be resolved.
   */
  suspend fun resolveReferenceNameOrNull(file: McFile, toResolve: ReferenceName): ReferenceName? {

    return nameParser.parse(
      NameParser2Packet(
        file = file,
        toResolve = toResolve,
        referenceLanguage = language,
        stdLibNameOrNull = stdLibNameOrNull
      )
    )
  }
}

/**
 * Represents a resolver that can resolve symbols in the system. The type
 * `T` represents the type of the symbols that this resolver can handle.
 */
fun interface SymbolResolver<T> {
  /**
   * Resolves the declared name of a symbol in the system.
   *
   * @param symbol The symbol whose declared name is to be resolved.
   * @return The declared name of the symbol, or null if the symbol does not have a declared name.
   */
  suspend fun declaredNameOrNull(symbol: T): QualifiedDeclaredName?
}
