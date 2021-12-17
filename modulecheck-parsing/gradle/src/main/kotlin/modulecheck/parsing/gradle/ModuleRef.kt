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

package modulecheck.parsing.gradle

import modulecheck.utils.capitalize
import java.util.Locale

sealed interface ModuleRef {

  val value: String

  @JvmInline
  value class StringRef(override val value: String) : ModuleRef {
    init {
      require(value.startsWith(':')) {
        "The StringRef `value` parameter should be the traditional Gradle path, " +
          "starting with ':'.  Do not use the camel-cased type-safe project accessor.  " +
          "This argument was '$value'."
      }
    }

    fun toTypeSafe(): TypeSafeRef {
      return TypeSafeRef(value.typeSafeName())
    }
  }

  @JvmInline
  value class TypeSafeRef(override val value: String) : ModuleRef

  companion object {
    fun from(rawString: String): ModuleRef = if (rawString.startsWith(':')) {
      StringRef(rawString)
    } else {
      TypeSafeRef(rawString)
    }
  }
}

internal val projectSplitRegex = "[.\\-_]".toRegex()

/**
 * Takes a conventional Gradle project path (":core:jvm") and returns the type-safe accessor name.
 *
 * `:core` becomes `core`
 * `:core:jvm` becomes `core.jvm`
 * `:core-testing` becomes `coreTesting`
 * `:base:ui:navigation` becomes `base.ui.navigation`
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
