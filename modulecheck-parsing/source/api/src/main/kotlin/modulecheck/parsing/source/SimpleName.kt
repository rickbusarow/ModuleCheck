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

@JvmInline
/**
 * A name which is not fully qualified, like `Foo` in `com.example.Foo`
 *
 * @property name the string value of this name
 * @since 0.12.0
 */
value class SimpleName(val name: String) : Comparable<SimpleName> {

  init {
    require(name.matches("""^([a-zA-Z_$][a-zA-Z\d_$]*)|(`.*`)$""".toRegex())) {
      "SimpleName names must be valid Java identifier " +
        "without a dot qualifier or whitespace.  This name was: `$name`"
    }
  }

  override fun compareTo(other: SimpleName): Int = name.compareTo(other.name)

  companion object {
    /**
     * shorthand for `joinToString(".") { it.name }`
     *
     * @since 0.12.0
     */
    fun List<SimpleName>.asString() = joinToString(".") { it.name }

    /**
     * wraps this String in a [SimpleName]
     *
     * @since 0.12.0
     */
    fun String.asSimpleName() = SimpleName(this)

    /**
     * Removes the prefix of [packageName]'s value and a subsequent period, then splits the
     * remainder by dots and returns that list as [SimpleName]
     *
     * example: `com.example.Outer.Inner` becomes `[Outer, Inner]`
     *
     * @since 0.12.0
     */
    fun String.stripPackageNameFromFqName(packageName: PackageName): List<SimpleName> {
      return removePrefix("${packageName.name}.").split('.')
        .map { it.asSimpleName() }
    }
  }
}

/**
 * Convenience interface for providing a [SimpleName].
 *
 * @since 0.13.0
 */
interface HasSimpleNames {
  /**
   * The contained [SimpleNames][SimpleName]
   *
   * @since 0.12.0
   */
  val simpleNames: List<SimpleName>

  /**
   * If the collection in [simpleNames] has more than one name, this value will be the last.
   *
   * example: Given a full name of `com.example.Outer.Inner`, with the [simpleNames] `[Outer,
   * Inner]`, this value will be `Inner`.
   *
   * @since 0.12.0
   */
  val simplestName: SimpleName
    get() = simpleNames.last()

  companion object {

    internal fun HasSimpleNames.checkSimpleNames() {
      require(simpleNames.isNotEmpty()) {
        "`simpleNames` must have at least one name, but this list is empty."
      }
    }
  }
}
