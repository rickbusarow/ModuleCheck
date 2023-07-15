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

package modulecheck.utils

import org.intellij.lang.annotations.Language
import kotlin.text.replace
import kotlin.text.replace as matchResultReplace

/**
 * code golf for `replace(regex.toRegex(), replacement)`
 *
 * @since 0.12.0
 */
fun String.replaceRegex(
  @Language("regexp")
  regex: String,
  replacement: String
): String = replace(
  regex = regex.toRegex(),
  replacement = replacement
)

inline fun String.replaceDestructured(
  regex: Regex,
  crossinline transform: (group1: String) -> String
): String {
  return matchResultReplace(regex) { matchResult ->
    transform(matchResult.destructured.component1())
  }
}

inline fun String.replaceDestructured(
  regex: Regex,
  crossinline transform: (group1: String, group2: String) -> String
): String {
  return matchResultReplace(regex) { matchResult ->
    transform(
      matchResult.destructured.component1(),
      matchResult.destructured.component2()
    )
  }
}

inline fun String.replaceDestructured(
  regex: Regex,
  crossinline transform: (group1: String, group2: String, group3: String) -> String
): String {
  return matchResultReplace(regex) { matchResult ->
    transform(
      matchResult.destructured.component1(),
      matchResult.destructured.component2(),
      matchResult.destructured.component3()
    )
  }
}

inline fun String.replaceDestructured(
  regex: Regex,
  crossinline transform: (group1: String, group2: String, group3: String, group4: String) -> String
): String {
  return matchResultReplace(regex) { matchResult ->
    transform(
      matchResult.destructured.component1(),
      matchResult.destructured.component2(),
      matchResult.destructured.component3(),
      matchResult.destructured.component4()
    )
  }
}

inline fun String.replaceDestructured(
  regex: Regex,
  crossinline transform: (
    group1: String,
    group2: String,
    group3: String,
    group4: String,
    group5: String
  ) -> String
): String {
  return matchResultReplace(regex) { matchResult ->
    transform(
      matchResult.destructured.component1(),
      matchResult.destructured.component2(),
      matchResult.destructured.component3(),
      matchResult.destructured.component4(),
      matchResult.destructured.component5()
    )
  }
}

/**
 * Constructs a `Regex` instance by applying the provided `builder` function
 * within the context of a `RegexBuilderScope`. This enables the creation
 * of complex regular expressions in a more readable and maintainable way.
 *
 * @param builder A lambda function with `RegexBuilderScope` as its receiver.
 * @return The constructed `Regex` instance.
 */
inline fun regex(builder: RegexBuilder.() -> Unit): Regex {
  return RegexBuilder()
    .apply(builder)
    .stringBuilder
    .toString()
    .toRegex()
}

/** Supports the [regex] builder function. */
class RegexBuilder {

  @PublishedApi
  internal val stringBuilder: StringBuilder = StringBuilder()

  /**
   * Appends a pattern to the regex builder, injecting the language in the IDE.
   * @return The current `RegexBuilderScope` instance
   */
  fun append(@Language("regexp") pattern: String): RegexBuilder = apply {
    stringBuilder.append(pattern)
  }

  /**
   * The same as [append], except without the language injection. This is for special cases where
   * the IDE incorrectly flags the pattern -- hopefully just because it doesn't have enough context.
   *
   * Before using this, try restructuring the different components of the
   * builder invocation so that the [pattern] argument has enough context.
   */
  fun appendWithoutInjection(pattern: String): RegexBuilder = apply {
    stringBuilder.append(pattern)
  }

  /**
   * Appends the OR operator (`|`) to the regex builder.
   *
   * @return The current `RegexBuilderScope` instance
   */
  fun or(): RegexBuilder = apply {
    stringBuilder.append("|")
  }

  /**
   * Groups a segment of a regular expression, with the `groupStart` and
   * `groupEnd` parameters marking the start and end of the group respectively.
   *
   * The [builder] parameter is applied in the middle.
   *
   * @param groupStart The opening characters of the group.
   * @param groupEnd The closing characters of the group.
   * @param builder Defines the group content in between the start and end.
   * @return The current `RegexBuilderScope` instance
   */
  inline fun grouped(
    groupStart: String,
    groupEnd: String,
    builder: RegexBuilder.() -> Unit
  ): RegexBuilder = apply {
    append(groupStart)
    builder()
    append(groupEnd)
  }

  /**
   * This just provides an indent around related parts of the pattern. Defines
   * a section of the regular expression string. The `builder` function
   * is applied to the `RegexBuilderScope` to define the section content.
   *
   * @param builder A lambda function with `RegexBuilderScope`
   *   as its receiver, used to define the section content.
   * @return The current `RegexBuilderScope` instance, enabling function chaining.
   */
  inline fun section(builder: RegexBuilder.() -> Unit): RegexBuilder = apply(builder)

  /** overload for building a regex inside a regex builder */
  inline fun regex(builder: RegexBuilder.() -> Unit): String {
    return RegexBuilder()
      .apply(builder)
      .stringBuilder
      .toString()
  }

  /**
   * Combines multiple builders into a single builder, separated by the OR operator (|).
   * @param builders The builders to combine.
   * @return The current `RegexBuilder` instance.
   */
  fun anyOf(vararg builders: RegexBuilder.() -> Unit): RegexBuilder {
    return applyEachIndexed(builders) { index, builder ->
      builder()
      if (index != builders.lastIndex) {
        appendWithoutInjection("|")
      }
    }
  }
}
