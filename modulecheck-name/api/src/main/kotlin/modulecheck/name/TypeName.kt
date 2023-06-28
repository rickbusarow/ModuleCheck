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

import modulecheck.name.PackageName.Companion.asPackageName
import modulecheck.name.SimpleName.Companion.asSimpleName
import modulecheck.utils.lazy.unsafeLazy
import modulecheck.utils.pluralString

/** either a [ClassName] or a [TypeParameter] */
sealed interface TypeName : Name, HasSimpleNames {
  /** */
  val nullable: Boolean

  /** @return a new instance of [TypeName] with nullability set to true. */
  fun makeNullable(): TypeName

  /** @return a new instance of [TypeName] with nullability set to false. */
  fun makeNotNullable(): TypeName
}

/**
 * Represents a class name in the Kotlin language. It includes the
 * package name, simple names, type arguments, and nullability.
 *
 * @property packageName The package name of the class.
 * @property simpleNames The list of simple names of the class.
 * @property typeArguments The list of type arguments of the class.
 * @property nullable Indicates if the class name is nullable.
 */
data class ClassName(
  override val packageName: PackageName,
  override val simpleNames: List<SimpleName>,
  val typeArguments: List<TypeName>,
  override val nullable: Boolean
) : TypeName, NameWithPackageName {

  /** ex: `com.example.MyGenericType<out T: SomeType>` */
  val asStringWithTypeParameters: String by unsafeLazy {
    if (typeArguments.isEmpty()) {
      asString
    } else {
      asString.plus(
        typeArguments.joinToString(
          separator = ", ",
          prefix = "<",
          postfix = ">"
        ) { it.asString }
      )
    }
  }

  constructor(
    packageName: String,
    vararg simpleNames: String,
    typeArguments: List<TypeName> = emptyList(),
    nullable: Boolean = false
  ) : this(
    packageName = packageName.asPackageName(),
    simpleNames = simpleNames.map { it.asSimpleName() },
    typeArguments = typeArguments,
    nullable = nullable
  )

  /** @return a new instance of [ClassName] with nullability set to true. */
  override fun makeNullable(): TypeName = copy(nullable = true)

  /** @return a new instance of [ClassName] with nullability set to false. */
  override fun makeNotNullable(): TypeName = copy(nullable = false)

  /**
   * @param typeArguments The type arguments to parameterize the class name with.
   * @return a new instance of [ClassName] with the provided type arguments.
   */
  fun parameterizedBy(vararg typeArguments: TypeName): ClassName =
    copy(typeArguments = typeArguments.toList())
}

/**
 * examples:
 * ```
 * T : CharSequence
 * T
 * /*...*/ T /*...*/ where T: Bar, T: Baz
 * ```
 *
 * @property simpleName the simple name given to the generic, like `T` or `OutputT`
 * @property bounds empty if it's a simple generic
 * @property nullable `<T?>` vs `<T>`
 * @property variance The variance of the type parameter, can be either `IN`, `OUT`, or `null`.
 */
data class TypeParameter(
  override val simpleName: SimpleName,
  val bounds: List<TypeName>,
  override val nullable: Boolean,
  val variance: Variance?
) : TypeName {

  override val segments: List<String> get() = listOf(simpleName.asString)
  override val simpleNames: List<SimpleName> get() = listOf(simpleName)
  override val asString: String by unsafeLazy {
    bounds.pluralString(
      empty = { simpleName.asString },
      single = { "${simpleName.asString} : ${it.asString}" },
      moreThanOne = { simpleName.asString }
    )
  }

  constructor(
    name: SimpleName,
    vararg bounds: TypeName
  ) : this(simpleName = name, bounds = bounds.toList(), nullable = false, variance = null)

  /** @return a new instance of [TypeParameter] with nullability set to true. */
  override fun makeNullable(): TypeName = copy(nullable = true)

  /** @return a new instance of [TypeParameter] with nullability set to false. */
  override fun makeNotNullable(): TypeName = copy(nullable = false)

  /** Represents the variance of a type parameter in the Kotlin language. */
  enum class Variance {
    OUT,
    IN
  }
}
