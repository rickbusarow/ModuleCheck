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

import kotlinx.coroutines.flow.asFlow
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.filterIsInstance
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import modulecheck.model.sourceset.SourceSetName
import modulecheck.parsing.element.resolve.NameParser2.NameParser2Packet
import modulecheck.parsing.psi.internal.DeclarationsProvider
import modulecheck.parsing.source.AndroidDataBindingReferenceName
import modulecheck.parsing.source.AndroidRReferenceName
import modulecheck.parsing.source.PackageName.Companion.asPackageName
import modulecheck.parsing.source.QualifiedDeclaredName
import modulecheck.parsing.source.ReferenceName
import modulecheck.parsing.source.ReferenceName.Companion.asReferenceName
import modulecheck.parsing.source.internal.AndroidDataBindingNameProvider
import modulecheck.parsing.source.internal.AndroidRNameProvider

/**
 * Implementation of [ParsingInterceptor2] that handles the resolution of reference
 * names, considering different name providers and declaration providers. It concatenates
 * the names to form fully qualified names and ensures proper resolution, especially
 * for Android specific resources like R files and data binding declarations.
 *
 * @property androidRNameProvider Provider to supply Android R file names.
 * @property dataBindingNameProvider Provider to supply data binding declarations.
 * @property declarationsProvider Provider to supply local and external declarations.
 * @property sourceSetName Name of the source set being parsed.
 */
class ConcatenatingParsingInterceptor2(
  private val androidRNameProvider: AndroidRNameProvider,
  private val dataBindingNameProvider: AndroidDataBindingNameProvider,
  private val declarationsProvider: DeclarationsProvider,
  private val sourceSetName: SourceSetName
) : ParsingInterceptor2 {
  /**
   * Intercepts the parsing operation and tries to resolve
   * the [ReferenceName] in the given chain packet.
   *
   * Resolution occurs in the following order:
   * 1. Checks if the reference is an imported reference.
   * 2. If not, checks if it is a standard library name.
   * 3. If not, it checks for local declarations.
   * 4. If all these fail, it tries to infer the reference.
   *
   * The first successful resolution is returned, or if no
   * resolution is found, it proceeds to the next interceptor.
   *
   * @param chain The current parsing operation.
   * @return The resolved [ReferenceName], or `null` if no resolution can be found.
   */
  override suspend fun intercept(chain: ParsingInterceptor2.Chain): ReferenceName? {

    val packet = chain.packet

    val file = packet.file

    val packageName = file.packageName

    val toResolve = packet.toResolve

    val dataBindingDeclarations = dataBindingNameProvider.get()
    val androidRNames = androidRNameProvider.getAll()
    val localAndroidROrNull = androidRNameProvider.getLocalOrNull()

    return packet.importedReferenceOrNull(toResolve)
      ?.let { imported ->

        when {
          toResolve.equals(localAndroidROrNull) -> {
            AndroidRReferenceName(file.packageName, packet.referenceLanguage)
          }

          androidRNames.contains(imported) -> {
            AndroidRReferenceName(
              imported.segments.dropLast(1).joinToString(".").asPackageName(),
              packet.referenceLanguage
            )
          }

          dataBindingDeclarations.contains(imported) -> {
            AndroidDataBindingReferenceName(imported.name, packet.referenceLanguage)
          }

          else -> imported
        }
      }
      ?: packet.stdLibNameOrNull(toResolve)?.asReferenceName(packet.referenceLanguage)
      ?: declarationsProvider
        .getWithUpstream(
          sourceSetName = sourceSetName,
          packageNameOrNull = packageName
        )
        .filterIsInstance<QualifiedDeclaredName>()
        .firstOrNull { it.isTopLevel && it.endsWithSimpleName(toResolve.referenceFirstName()) }
        ?.name
        ?.asReferenceName(packet.referenceLanguage)
      ?: packet.resolveInferredOrNull(toResolve)
      ?: chain.proceed(packet)
  }

  /**
   * Tries to infer a [ReferenceName] by concatenating it with all possible wildcard imports.
   * The function then checks if any of these concatenated names exist in the set of all
   * declarations. If it finds a match, the [ReferenceName] is considered resolved and is returned.
   *
   * @param toResolve The [ReferenceName] to resolve.
   * @return The inferred [ReferenceName] if found, `null` otherwise.
   */
  private suspend fun NameParser2Packet.resolveInferredOrNull(
    toResolve: ReferenceName
  ): ReferenceName? {

    val fullyQualifiedAndWildcard = flow {
      // no import
      emit(toResolve)

      // concat with any wildcard imports
      val concatenated = file.wildcardImports.get()
        .asFlow()
        .map { it.removeSuffix(".*") }
        .map {
          "$it.$toResolve".asReferenceName(referenceLanguage)
        }

      emitAll(concatenated)
    }

    val allDeclarations = declarationsProvider
      .getWithUpstream(
        sourceSetName = sourceSetName,
        packageNameOrNull = null
      )

    return fullyQualifiedAndWildcard
      .firstOrNull { allDeclarations.contains(it) }
  }

  /**
   * Checks if the given [ReferenceName] to resolve is part of the
   * imported references in the file. If a match is found, the function
   * returns the imported [ReferenceName]. Otherwise, it returns `null`.
   *
   * @param toResolve The [ReferenceName] to resolve.
   * @return The imported [ReferenceName] if found, `null` otherwise.
   */
  private suspend fun NameParser2Packet.importedReferenceOrNull(
    toResolve: ReferenceName
  ): ReferenceName? {
    return file.imports
      .get()
      .firstNotNullOfOrNull { importReference ->

        val matched = importReference.endsWith(toResolve.referenceFirstName())

        val referenceStart = toResolve.referenceFirstName()

        when {
          // Given a simple name without a qualifier and a matching import, like:
          // toResolve : "Foo"
          // import: "com.example.Foo"
          // ... just return the import.
          matched && referenceStart == toResolve.name -> importReference
          // If it's matched but the name to resolve is qualified, then remove the part that matched
          // and concatenate.
          // toResolve: Foo.Bar
          // import: com.example.Foo
          // withoutStart = .Bar
          // concatenated = com.example.Foo.Bar
          matched -> {
            val withoutStart = toResolve.segments.drop(1).joinToString(".")
            "$importReference.$withoutStart".asReferenceName(referenceLanguage)
          }

          else -> null
        }
      }
  }

  /**
   * Retrieves the first name (preceding the first dot) from a `String` representing a
   * fully-qualified name. For instance, in "com.example.myApp", the first name would be "com".
   *
   * @return The first name in the qualified name.
   */
  fun String.referenceFirstName(): String = split('.').first()

  /**
   * Retrieves the last name (following the final dot) from a `String` representing a
   * fully-qualified name. For instance, in "com.example.myApp", the last name would be "myApp".
   *
   * @return The last name in the qualified name.
   */
  fun String.referenceLastName(): String = split('.').last()

  /**
   * Retrieves the first name (preceding the first dot) from a [ReferenceName].
   * For instance, in "com.example.myApp", the first name would be "com".
   *
   * @return The first name in the reference name.
   */
  fun ReferenceName.referenceFirstName(): String = segments.first()

  /**
   * Retrieves the last name (following the final dot) from a [ReferenceName].
   * For instance, in "com.example.myApp", the last name would be "myApp".
   *
   * @return The last name in the reference name.
   */
  fun ReferenceName.referenceLastName(): String = segments.last()
}
