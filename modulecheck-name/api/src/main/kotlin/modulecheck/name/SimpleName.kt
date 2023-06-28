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

package modulecheck.name

/**
 * A name which is not fully qualified, like `Foo` in `com.example.Foo`
 *
 * @property asString the string value of this name
 */
@JvmInline
value class SimpleName(override val asString: String) : Name {

  init {
    require(asString.matches("""^([a-zA-Z_$][a-zA-Z\d_$]*)|(`.*`)$""".toRegex())) {
      "SimpleName names must be valid Java identifier " +
        "without a dot qualifier or whitespace.  This name was: `$asString`"
    }
  }

  override val segments: List<String>
    get() = listOf(asString)

  override val simpleName: SimpleName
    get() = this

  override val simpleNameString: String
    get() = asString

  companion object {
    /** shorthand for `joinToString(".") { it.name.trim() }` */
    fun List<SimpleName>.asString(): String = joinToString(".") { it.asString.trim() }

    /** wraps this String in a [SimpleName] */
    fun String.asSimpleName(): SimpleName = SimpleName(this)

    /**
     * Removes the prefix of [packageName]'s value and a subsequent period,
     * then splits the remainder by dots and returns that list as [SimpleName]
     *
     * example: `com.example.Outer.Inner` becomes `[Outer, Inner]`
     */
    fun String.stripPackageNameFromFqName(packageName: PackageName): List<SimpleName> {
      return removePrefix("${packageName.asString}.").split('.')
        .map { it.asSimpleName() }
    }
  }
}

/** Convenience interface for providing a [SimpleName]. */
interface HasSimpleNames {
  /** The contained [SimpleNames][SimpleName] */
  val simpleNames: List<SimpleName>

  /**
   * If the collection in [simpleNames] has more than one name, this value will be the last.
   *
   * example: Given a full name of `com.example.Outer.Inner`, with
   * the [simpleNames] `[Outer, Inner]`, this value will be `Inner`.
   */
  val simplestName: SimpleName
    get() = simpleNames.last()

  companion object {

    internal fun HasSimpleNames.checkSimpleNames() {
      check(simpleNames.isNotEmpty()) {
        "`simpleNames` must have at least one name, but this list is empty."
      }
    }
  }
}
