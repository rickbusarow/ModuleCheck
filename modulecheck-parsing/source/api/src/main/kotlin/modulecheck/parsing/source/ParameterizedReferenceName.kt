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

import modulecheck.parsing.source.McName.CompatibleLanguage
import modulecheck.utils.mapToSet

/**
 * Represents a parameterized reference name within a specific language.
 * This class is used primarily to validate and format raw type names with
 * their type parameters for given languages, ensuring internal consistency.
 *
 * An example of usage:
 * ```
 * val rawTypeName = ReferenceName("List")
 * val typeParam = ReferenceName("String")
 * val parameterizedReferenceName = ParameterizedReferenceName(rawTypeName, listOf(typeParam))
 * ```
 *
 * @param name The full name of the reference, which includes raw type name and type parameters.
 * @property language The language that the reference name is compatible with.
 * @property rawTypeName The raw [ReferenceName] without any type parameters.
 * @property typeParams A list of [ReferenceName]s that are the type parameters for the raw type.
 */
class ParameterizedReferenceName private constructor(
  name: String,
  override val language: CompatibleLanguage,
  val rawTypeName: ReferenceName,
  val typeParams: List<ReferenceName>
) : ReferenceName(name) {

  init {
    check(typeParams.isNotEmpty()) { "The type parameters list cannot be empty." }
    val languages = typeParams.mapToSet { it.language } + language + rawTypeName.language

    @Suppress("MagicNumber")
    check(languages.size == 1) {
      "All languages for type parameters and the raw type must match:\n" +
        typeParams.joinToString { "${it.language.toString().padStart(10)}  --  ${it.name}" }
    }
  }

  override val segments: List<String>
    get() = rawTypeName.segments

  override val simpleName: String
    get() = rawTypeName.simpleName

  /**
   * Constructs a [ParameterizedReferenceName] with the given raw type name and type parameters.
   * The `name` is automatically composed from the raw type name and type parameters.
   *
   * @param rawTypeName The raw type name.
   * @param typeParams The type parameters.
   */
  constructor(
    rawTypeName: ReferenceName,
    typeParams: List<ReferenceName>
  ) : this(
    name = typeParams.joinToString(
      separator = ", ",
      prefix = "${rawTypeName.name}<",
      postfix = ">",
      transform = { it.name }
    ),
    language = rawTypeName.language,
    rawTypeName = rawTypeName,
    typeParams = typeParams
  )

  companion object {

    /**
     * Extension function that generates a [ParameterizedReferenceName] using the receiver
     * [ReferenceName] as the raw type and a list of [ReferenceName]s as the type parameters.
     *
     * @param typeParams The type parameters.
     * @receiver The raw type name.
     * @return The constructed [ParameterizedReferenceName].
     */
    fun ReferenceName.parameterizedBy(typeParams: List<ReferenceName>): ParameterizedReferenceName =
      ParameterizedReferenceName(
        rawTypeName = this,
        typeParams = typeParams
      )

    /**
     * Extension function that generates a [ParameterizedReferenceName]
     * using the receiver [ReferenceName] as the raw type and a
     * variable number of [ReferenceName]s as the type parameters.
     *
     * @param typeParams The type parameters.
     * @receiver The raw type name.
     * @return The constructed [ParameterizedReferenceName].
     */
    fun ReferenceName.parameterizedBy(
      vararg typeParams: ReferenceName
    ): ParameterizedReferenceName = parameterizedBy(typeParams.toList())
  }
}
