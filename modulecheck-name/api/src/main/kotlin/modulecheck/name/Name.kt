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

import modulecheck.name.SimpleName.Companion.asSimpleName
import java.io.Serializable

/**
 * Fundamentally, this is a version of `Name` or `FqName` (such as Kotlin's
 * [Name][org.jetbrains.kotlin.name.Name] and [FqName][org.jetbrains.kotlin.name.FqName])
 * with syntactic sugar for complex matching requirements.
 */
sealed interface Name : Comparable<Name>, Serializable {

  /** The raw String value of this name, such as `com.example.lib1.Lib1Class`. */
  val asString: String

  /** The simplest name. For an inner class like `com.example.Outer.Inner`, this will be 'Inner'. */
  val simpleName: SimpleName
    get() = (this as? HasSimpleNames)?.simpleNames?.last()
      ?: asString.split('.').last().asSimpleName()

  /** The simplest name. For an inner class like `com.example.Outer.Inner`, this will be 'Inner'. */
  val simpleNameString: String
    get() = simpleName.asString

  override fun compareTo(other: Name): Int {
    return compareValuesBy(this, other, { it.asString }, { it::class.qualifiedName })
  }
}
