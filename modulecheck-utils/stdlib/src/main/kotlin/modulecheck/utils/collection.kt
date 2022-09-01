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

package modulecheck.utils

/**
 * functional style shorthand for `listOf(this)`
 *
 * @since 0.12.0
 */
fun <T> T.singletonList() = listOf(this)

/**
 * functional style shorthand for `setOf(this)`
 *
 * @since 0.12.0
 */
fun <T> T.singletonSet() = setOf(this)

/**
 * shorthand for `filterTo(destination, predicate)`
 *
 * @since 0.13.0
 */
inline fun <T> Iterable<T>.filterToSet(
  destination: MutableSet<T> = mutableSetOf(),
  predicate: (T) -> Boolean
): Set<T> {
  return filterTo(destination, predicate)
}

inline fun <C : Collection<T>, T, R> C.mapToSet(
  destination: MutableSet<R> = mutableSetOf(),
  transform: (T) -> R
): Set<R> {
  return mapTo(destination, transform)
}

inline fun <T, R> Iterable<T>.flatMapToSet(
  destination: MutableSet<R> = mutableSetOf(),
  transform: (T) -> Iterable<R>
): Set<R> {
  return flatMapTo(destination, transform)
}

inline fun <T, R> Sequence<T>.flatMapToSet(
  destination: MutableSet<R> = mutableSetOf(),
  transform: (T) -> Iterable<R>
): Set<R> {
  return flatMapTo(destination, transform)
}

/**
 * Returns a list of all elements sorted according to the specified [selectors].
 *
 * The sort is _stable_. It means that equal elements preserve their order relative to each other
 * after sorting.
 *
 * @since 0.12.0
 */
public fun <T> Iterable<T>.sortedWith(vararg selectors: (T) -> Comparable<*>): List<T> {
  if (this is Collection) {
    if (size <= 1) return this.toList()
    @Suppress("UNCHECKED_CAST")
    return (toTypedArray<Any?>() as Array<T>).apply { sortWith(compareBy(*selectors)) }.asList()
  }
  return toMutableList().apply { sortWith(compareBy(*selectors)) }
}

/**
 * Returns a list of all elements sorted according to the specified [selectors].
 *
 * The sort is _stable_. It means that equal elements preserve their order relative to each other
 * after sorting.
 *
 * @since 0.12.0
 */
public fun <T> Sequence<T>.sortedWith(vararg selectors: (T) -> Comparable<*>): Sequence<T> {
  return sortedWith(compareBy(*selectors))
}

/**
 * Returns a list of all elements sorted according to the specified [selectors].
 *
 * The sort is _stable_. It means that equal elements preserve their order relative to each other
 * after sorting.
 *
 * @since 0.12.0
 */
public fun <T> Iterable<T>.sortedWithDescending(vararg selectors: (T) -> Comparable<*>): List<T> {
  return sortedWith(*selectors).reversed()
}

/**
 * shorthand for `values.flatten().distinct()`
 *
 * @since 0.13.0
 */
fun <K : Any, T : Any> Map<K, Collection<T>>.allValues(): List<T> {
  return values.flatten().distinct()
}

/**
 * Creates a sequence of those [elements] which are not null
 *
 * @since 0.13.0
 */
fun <T> sequenceOfNotNull(
  vararg elements: T?
): Sequence<T> = sequence {
  elements.forEach { element ->
    if (element != null) yield(element)
  }
}

/**
 * @return true if any element in [other] is contained within the receiver collection, otherwise
 *     returns false
 * @since 0.13.0
 */
fun <E> Iterable<E>.containsAny(other: Iterable<Any?>): Boolean {

  return when {
    this === other -> true
    this is Set<E> && other is Set<*> -> {
      intersect(other.toSetOrSelf()).isNotEmpty()
    }

    else -> {
      val thisAsSet = toSetOrSelf()
      other.any { thisAsSet.contains(it) }
    }
  }
}

/**
 * shorthand for `this as? Set<E> ?: toSet()`
 *
 * @return itself if the receiver [Iterable] is already a `Set<E>`, otherwise calls `toSet()` to
 *     create a new one
 * @since 0.13.0
 */
internal fun <E> Iterable<E>.toSetOrSelf(): Set<E> {
  return this as? Set<E> ?: toSet()
}
