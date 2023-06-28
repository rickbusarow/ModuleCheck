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

package modulecheck.parsing.psi.internal

import modulecheck.name.ClassName
import modulecheck.name.PackageName
import modulecheck.name.PackageName.Companion.asPackageName
import modulecheck.name.SimpleName.Companion.asSimpleName
import modulecheck.name.TypeName
import modulecheck.parsing.source.McName.CompatibleLanguage.KOTLIN
import modulecheck.parsing.source.ParameterizedReferenceName
import modulecheck.parsing.source.ReferenceName
import modulecheck.parsing.source.ReferenceName.Companion.asReferenceName
import modulecheck.utils.requireNotNull
import org.jetbrains.kotlin.descriptors.containingPackage
import org.jetbrains.kotlin.js.descriptorUtils.getKotlinTypeFqName
import org.jetbrains.kotlin.resolve.descriptorUtil.fqNameUnsafe
import org.jetbrains.kotlin.types.KotlinType
import org.jetbrains.kotlin.types.checker.SimpleClassicTypeSystemContext.isNullableType
import org.jetbrains.kotlin.types.getAbbreviatedType

/**
 * Ensures that the receiver [KotlinType] is not null and converts it
 * to a [ReferenceName]. Throws an exception if the receiver is null.
 *
 * @return The [ReferenceName] representation of the receiver [KotlinType].
 * @throws IllegalArgumentException If the receiver is null.
 */
fun KotlinType?.requireReferenceName(): ReferenceName {

  return requireNotNull { "The receiver type is null" }
    .asReferenceName()
}

/**
 * Converts the receiver [KotlinType] to a [ReferenceName].
 * Handles type aliases and parameterized types.
 *
 * @return The [ReferenceName] representation of the receiver [KotlinType].
 */
fun KotlinType.asReferenceName(): ReferenceName {

  // handle type aliases first.  An AbbreviatedType is an aliased type.  If it's an import alias,
  // the abbreviation is null.  If it's a typealias, the abbreviation is the type of the typealias
  // and that's what we want.
  val abbreviatedReferenceOrNull = getAbbreviatedType()?.abbreviation
    ?.asReferenceName()
  if (abbreviatedReferenceOrNull != null) {
    return abbreviatedReferenceOrNull
  }

  val rawType = getKotlinTypeFqName(false).asReferenceName(KOTLIN)

  return when {
    arguments.isNotEmpty() -> ParameterizedReferenceName(
      rawTypeName = rawType,
      typeParams = arguments.map { it.type.asReferenceName() }
    )

    else -> rawType
  }
}

/**
 * Ensures that the receiver [KotlinType] is not null and converts
 * it to a [TypeName]. Throws an exception if the receiver is null.
 *
 * @return The [TypeName] representation of the receiver [KotlinType].
 * @throws IllegalArgumentException If the receiver is null.
 */
fun KotlinType?.requireTypeName(): TypeName {

  return requireNotNull { "The receiver type is null" }
    .asTypeName()
}

/**
 * Converts the receiver [KotlinType] to a [TypeName]. Handles type aliases and constructs
 * a [ClassName] with package name, simple names, type arguments, and nullability.
 *
 * @return The [TypeName] representation of the receiver [KotlinType].
 */
fun KotlinType.asTypeName(): TypeName {

  // handle type aliases first.  An AbbreviatedType is an aliased type.  If it's an import alias,
  // the abbreviation is null.  If it's a typealias, the abbreviation is the type of the typealias
  // and that's what we want.
  val abbreviatedTypeNameOrNull = getAbbreviatedType()?.abbreviation?.asTypeName()
  if (abbreviatedTypeNameOrNull != null) {
    return abbreviatedTypeNameOrNull
  }

  val descriptor = requireNotNull(constructor.declarationDescriptor) {
    "declarationDescriptor is null for constructor = $constructor with ${constructor.javaClass}"
  }

  val packageFqName = descriptor.containingPackage()
  val packageName = packageFqName?.asString()?.asPackageName()
    ?: PackageName.DEFAULT

  val packageSegmentCount = if (packageFqName != null) {
    packageFqName.asString().count { it == '.' } + 1
  } else {
    0
  }

  val simpleNames = descriptor.fqNameUnsafe
    .pathSegments()
    .drop(packageSegmentCount)
    .map { it.asString().asSimpleName() }

  /**
   * Constructs a [ClassName] with the package name, simple names,
   * type arguments, and nullability of the receiver [KotlinType].
   *
   * @return The [ClassName] representation of the receiver [KotlinType].
   */
  return ClassName(
    packageName = packageName,
    simpleNames = simpleNames,
    typeArguments = arguments.map { it.type.asTypeName() },
    nullable = isNullableType()
  )
}
