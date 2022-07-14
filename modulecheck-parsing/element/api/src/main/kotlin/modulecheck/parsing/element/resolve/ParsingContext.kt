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

package modulecheck.parsing.element.resolve

import modulecheck.parsing.element.McFile
import modulecheck.parsing.element.resolve.NameParser2.NameParser2Packet
import modulecheck.parsing.kotlin.compiler.KotlinEnvironment
import modulecheck.parsing.source.McName.CompatibleLanguage
import modulecheck.parsing.source.QualifiedDeclaredName
import modulecheck.parsing.source.ReferenceName

class ParsingContext<T>(
  val nameParser: NameParser2,
  val symbolResolver: SymbolResolver<T>,
  val language: CompatibleLanguage,
  val kotlinEnvironment: KotlinEnvironment,
  val stdLibNameOrNull: ReferenceName.() -> QualifiedDeclaredName?
) {

  val bindingContextDeferred = kotlinEnvironment.bindingContextDeferred

  suspend fun declaredNameOrNull(symbol: T): QualifiedDeclaredName? {
    TODO()
  }

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

fun interface SymbolResolver<T> {
  suspend fun declaredNameOrNull(symbol: T): QualifiedDeclaredName?
}
