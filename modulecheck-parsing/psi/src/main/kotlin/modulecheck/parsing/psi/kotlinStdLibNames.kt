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

package modulecheck.parsing.psi

import modulecheck.parsing.source.Reference.ExplicitKotlinReference
import modulecheck.parsing.source.asExplicitKotlinReference

internal fun String.kotlinStdLibNameOrNull(): ExplicitKotlinReference? {

  return sequenceOf(
    "kotlin.$this",
    "kotlin.collections.$this"
  )
    .firstOrNull { it in kotlinStdLibNames }
    ?.asExplicitKotlinReference()
}

internal val kotlinStdLibNames = setOf(
  "kotlin.Any",
  "kotlin.Array",
  "kotlin.Unit",
  "kotlin.Boolean",
  "kotlin.Byte",
  "kotlin.Short",
  "kotlin.Int",
  "kotlin.Long",
  "kotlin.Char",
  "kotlin.Float",
  "kotlin.Double",
  "kotlin.String",
  "kotlin.CharSequence",
  "kotlin.Comparable",
  "kotlin.Throwable",
  "kotlin.Annotation",
  "kotlin.Nothing",
  "kotlin.Number",
  "kotlin.collections.Iterable",
  "kotlin.collections.Collection",
  "kotlin.collections.List",
  "kotlin.collections.Set",
  "kotlin.collections.Map",
  "kotlin.collections.Map.Entry",
  "kotlin.collections.MutableIterable",
  "kotlin.collections.MutableCollection",
  "kotlin.collections.MutableList",
  "kotlin.collections.MutableSet",
  "kotlin.collections.MutableMap",
  "kotlin.collections.MutableMap.Entry",
  "kotlin.BooleanArray",
  "kotlin.ByteArray",
  "kotlin.CharArray",
  "kotlin.ShortArray",
  "kotlin.IntArray",
  "kotlin.LongArray",
  "kotlin.FloatArray",
  "kotlin.DoubleArray",
  "kotlin.Enum",
  "kotlin.UByte",
  "kotlin.UShort",
  "kotlin.UInt",
  "kotlin.ULong",
  "kotlin.UByteArray",
  "kotlin.UShortArray",
  "kotlin.UIntArray",
  "kotlin.ULongArray",

  "kotlin.Exception",
  "kotlin.Error",
  "kotlin.RuntimeException",
  "kotlin.IllegalArgumentException",
  "kotlin.IllegalStateException",
  "kotlin.IndexOutOfBoundsException",
  "kotlin.UnsupportedOperationException",
  "kotlin.ArithmeticException",
  "kotlin.NumberFormatException",
  "kotlin.NullPointerException",
  "kotlin.ClassCastException",
  "kotlin.AssertionError",
  "kotlin.NoSuchElementException",
  "kotlin.ConcurrentModificationException",
  "kotlin.Comparator"
)
