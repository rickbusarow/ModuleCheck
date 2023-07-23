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

package modulecheck.model.dependency

import modulecheck.utils.capitalize
import modulecheck.utils.lazy.unsafeLazy
import java.util.Locale

/**
 * A Gradle project path, which could be either of:
 *
 * - **StringProjectPath**: ":some-module-group:api"
 * - **TypeSafeProjectPath**: "someModuleGroup.api"
 */
sealed class ProjectPath : Identifier {

  /**
   * the raw value of this path
   *
   * - **StringProjectPath**: ":some-module-group:api"
   * - **TypeSafeProjectPath**: "someModuleGroup.api"
   *
   * @since 0.12.0
   */
  abstract val value: String

  /**
   * @see value
   * @since 0.12.0
   */
  override val name: String by unsafeLazy { value }

  val typeSafeValue: String by lazy {
    when (this) {
      is StringProjectPath -> value.typeSafeName()
      is TypeSafeProjectPath -> value
    }
  }

  override fun equals(other: Any?): Boolean {
    if (this === other) return true
    if (other !is ProjectPath) return false

    if (value == other.value) return true

    // All comparisons are done using the type-safe value, because it's deterministic.
    return typeSafeValue == other.typeSafeValue
  }

  override fun hashCode(): Int {
    return typeSafeValue.hashCode()
  }

  override fun toString(): String = value

  abstract fun pathValue(typeSafeProjectPathResolver: TypeSafeProjectPathResolver): String

  /**
   * @return a [StringProjectPath] representation of this path.
   *   Returns this instance if it is already a string path.
   */
  abstract fun asStringProjectPath(
    typeSafeProjectPathResolver: TypeSafeProjectPathResolver
  ): StringProjectPath

  /**
   * @return a [TypeSafeProjectPath] representation of this path.
   *   Returns this instance if it is already a type-safe path.
   */
  abstract fun asTypeSafeProjectPath(): TypeSafeProjectPath
  class StringProjectPath(override val value: String) : ProjectPath() {
    init {
      require(value.startsWith(':')) {
        "The StringRef `value` parameter should be the traditional Gradle path, " +
          "starting with ':'.  Do not use the camel-cased type-safe project accessor.  " +
          "This argument was '$value'."
      }
    }

    override fun asStringProjectPath(
      typeSafeProjectPathResolver: TypeSafeProjectPathResolver
    ): StringProjectPath = this

    override fun pathValue(typeSafeProjectPathResolver: TypeSafeProjectPathResolver): String = value

    override fun asTypeSafeProjectPath(): TypeSafeProjectPath =
      TypeSafeProjectPath(value.typeSafeName())
  }

  class TypeSafeProjectPath(override val value: String) : ProjectPath() {

    override fun asStringProjectPath(
      typeSafeProjectPathResolver: TypeSafeProjectPathResolver
    ): StringProjectPath = StringProjectPath(pathValue(typeSafeProjectPathResolver))

    override fun pathValue(typeSafeProjectPathResolver: TypeSafeProjectPathResolver): String {
      return typeSafeProjectPathResolver
        .resolveStringProjectPath(this)
        .pathValue(typeSafeProjectPathResolver)
    }

    override fun asTypeSafeProjectPath(): TypeSafeProjectPath = this
  }

  companion object {
    fun from(rawString: String): ProjectPath = if (rawString.trim().startsWith(':')) {
      StringProjectPath(rawString)
    } else {
      TypeSafeProjectPath(rawString)
    }
  }
}

internal val projectSplitRegex = "[.\\-_]".toRegex()

/**
 * Takes a conventional Gradle project path (":core:jvm") and returns the type-safe accessor name.
 *
 * - `:core` becomes `core`
 * - `:core:jvm` becomes `core.jvm`
 * - `:core-testing` becomes `coreTesting`
 * - `:base:ui:navigation` becomes `base.ui.navigation`
 *
 * @since 0.12.0
 */
internal fun String.typeSafeName(): String = split(projectSplitRegex)
  .filterNot { it.isBlank() }
  .mapIndexed { index, s ->
    if (index == 0) {
      s.propertyAccessName()
    } else {
      s.capitalize(Locale.getDefault())
    }
  }
  .joinToString("")
  .replace(":", ".")
  .removePrefix(".")

private fun String.propertyAccessName(): String {
  if (isBlank()) return this

  val chars = toCharArray()

  if (!chars.first().isUpperCase()) return this

  val caps = chars
    .takeWhile { it.isUpperCase() }
    .dropLast(1)

  val lower = caps
    .joinToString("") {
      it.toString().lowercase()
    }

  return this.replaceFirst(this.substring(0, caps.size), lower)
}
