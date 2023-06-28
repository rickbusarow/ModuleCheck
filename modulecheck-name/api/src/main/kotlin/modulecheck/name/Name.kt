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

  /** ex: 'com.example.Subject' has the segments ['com', 'example', 'Subject'] */
  val segments: List<String>

  /** The simplest name. For an inner class like `com.example.Outer.Inner`, this will be 'Inner'. */
  val simpleName: SimpleName
    get() = segments.last().asSimpleName()

  /** The simplest name. For an inner class like `com.example.Outer.Inner`, this will be 'Inner'. */
  val simpleNameString: String
    get() = segments.last()

  override fun compareTo(other: Name): Int {
    return compareValuesBy(this, other, { it.asString }, { it::class.qualifiedName })
  }
}

sealed interface TypeName : Name, HasSimpleNames {
  val nullable: Boolean

  fun copy(nullable: Boolean): TypeName
}

class ClassName(
  override val packageName: PackageName,
  override val simpleNames: List<SimpleName>,
  override val nullable: Boolean
) : TypeName, NameWithPackageName {
  override fun copy(nullable: Boolean): ClassName {
    return ClassName(
      packageName = packageName,
      simpleNames = simpleNames,
      nullable = nullable
    )
  }
}

class ParameterizedTypeName(
  val rawType: ClassName,
  val typeParameters: List<TypeName>
) : TypeName, NameWithPackageName {

  override val packageName: PackageName get() = rawType.packageName
  override val simpleNames: List<SimpleName> get() = rawType.simpleNames
  override val nullable: Boolean get() = rawType.nullable

  override fun copy(nullable: Boolean): ParameterizedTypeName {
    return ParameterizedTypeName(
      rawType = rawType.copy(nullable),
      typeParameters = typeParameters
    )
  }
}
