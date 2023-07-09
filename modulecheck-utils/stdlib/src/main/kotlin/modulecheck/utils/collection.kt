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

/**
 * Wraps a given item in a list.
 *
 * This is a functional style shorthand for `listOf(this)`,
 * allowing a simpler syntax for creating a list from a single item.
 *
 * @receiver The item to be wrapped in a list.
 * @return A list containing only the receiver item.
 * @since 0.12.0
 */
fun <T> T.singletonList(): List<T> = listOf(this)

/**
 * Wraps a given item in a set.
 *
 * This is a functional style shorthand for `setOf(this)`,
 * allowing a simpler syntax for creating a set from a single item.
 *
 * @receiver The item to be wrapped in a set.
 * @return A set containing only the receiver item.
 * @since 0.12.0
 */
fun <T> T.singletonSet(): Set<T> = setOf(this)

/**
 * Filters the receiver iterable and adds the matching elements to a set.
 *
 * This is a shorthand for `filterTo(destination, predicate)` where destination is a set.
 *
 * @param destination The destination set where the elements that match
 *   the predicate are placed. By default, it is an empty mutable set.
 * @param predicate A function that determines if an item should be included in the output set.
 * @receiver The iterable to be filtered.
 * @return A set containing elements from the receiver iterable that match the predicate.
 */
inline fun <T> Iterable<T>.filterToSet(
  destination: MutableSet<T> = mutableSetOf(),
  predicate: (T) -> Boolean
): Set<T> {
  return filterTo(destination, predicate)
}

/**
 * Transforms the elements of the receiver collection and adds the results to a set.
 *
 * @param destination The destination set where the transformed
 *   elements are placed. By default, it is an empty mutable set.
 * @param transform A function that maps elements of the receiver collection to the output set.
 * @receiver The collection to be transformed.
 * @return A set containing the transformed elements from the receiver collection.
 */
inline fun <C : Collection<T>, T, R> C.mapToSet(
  destination: MutableSet<R> = mutableSetOf(),
  transform: (T) -> R
): Set<R> {
  return mapTo(destination, transform)
}

/**
 * Transforms each element of the receiver iterable to an
 * iterable and flattens these iterables into a single set.
 *
 * @param destination The destination set where the transformed
 *   elements are placed. By default, it is an empty mutable set.
 * @param transform A function that maps elements of the
 *   receiver iterable to an iterable of output elements.
 * @receiver The iterable to be transformed.
 * @return A set containing the flattened transformed elements from the receiver iterable.
 */
inline fun <T, R> Iterable<T>.flatMapToSet(
  destination: MutableSet<R> = mutableSetOf(),
  transform: (T) -> Iterable<R>
): Set<R> {
  return flatMapTo(destination, transform)
}

/**
 * Transforms each element of the receiver iterable to an
 * iterable and flattens these iterables into a single set.
 *
 * @param destination The destination set where the transformed
 *   elements are placed. By default, it is an empty mutable set.
 * @param transform A function that maps elements of the
 *   receiver iterable to an iterable of output elements.
 * @receiver The sequence to be transformed.
 * @return A set containing the flattened transformed elements from the receiver iterable.
 */
inline fun <T, R> Sequence<T>.flatMapToSet(
  destination: MutableSet<R> = mutableSetOf(),
  transform: (T) -> Iterable<R>
): Set<R> {
  return flatMapTo(destination, transform)
}

/**
 * Returns a list of all elements sorted according to the specified [selectors].
 *
 * The sort is _stable_. It means that equal elements
 * preserve their order relative to each other after sorting.
 *
 * @since 0.12.0
 */
fun <T> Iterable<T>.sortedWith(vararg selectors: (T) -> Comparable<*>): List<T> {
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
 * The sort is _stable_. It means that equal elements
 * preserve their order relative to each other after sorting.
 *
 * @since 0.12.0
 */
fun <T> Sequence<T>.sortedWith(vararg selectors: (T) -> Comparable<*>): Sequence<T> {
  return sortedWith(compareBy(*selectors))
}

/**
 * Returns a list of all elements sorted according to the specified [selectors].
 *
 * The sort is _stable_. It means that equal elements
 * preserve their order relative to each other after sorting.
 *
 * @since 0.12.0
 */
fun <T> Iterable<T>.sortedWithDescending(vararg selectors: (T) -> Comparable<*>): List<T> {
  return sortedWith(*selectors).reversed()
}

/** shorthand for `values.flatten().distinct()` */
fun <K : Any, T : Any> Map<K, Collection<T>>.allValues(): List<T> {
  return values.flatten().distinct()
}

/** Creates a sequence of those [elements] which are not null */
fun <T> sequenceOfNotNull(vararg elements: T?): Sequence<T> = sequence {
  elements.forEach { element ->
    if (element != null) yield(element)
  }
}

/**
 * @return true if any element in [other] is contained
 *   within the receiver collection, otherwise returns false
 */
fun <E> Iterable<E>.containsAny(other: Iterable<Any?>): Boolean {

  return when {
    this === other -> true
    this is Set<E> && other is Set<*> -> {
      intersect(other.asSet()).isNotEmpty()
    }

    else -> {
      val thisAsSet = asSet()
      other.any { thisAsSet.contains(it) }
    }
  }
}

/**
 * shorthand for `this as? Set<E> ?: toSet()`
 *
 * @return itself if the receiver [Iterable] is already a
 *   `Set<E>`, otherwise calls `toSet()` to create a new one
 */
fun <E> Iterable<E>.asSet(): Set<E> = this as? Set<E> ?: toSet()

/**
 * shorthand for `this as? List<E> ?: toList()`
 *
 * @return itself if the receiver [Iterable] is already a
 *   `List<E>`, otherwise calls `toList()` to create a new one
 */
fun <E> Iterable<E>.asList(): List<E> = this as? List<E> ?: toList()

/**
 * shorthand for `this as? Collection<E> ?: toList()`
 *
 * @return itself if the receiver [Iterable] is already a
 *   `Collection<E>`, otherwise calls `toList()` to create a new one
 */
fun <E> Iterable<E>.asCollection(): Collection<E> = this as? Collection<E> ?: toList()

/**
 * Returns a string representation of the collection based on its size.
 *
 * @param empty A function that generates a string for when the collection is empty.
 * @param single A function that generates a string for when the collection contains
 *   a single element. The single element is passed as an argument to this function.
 * @param moreThanOne A function that generates a string for when the collection contains
 *   more than one element. The entire collection is passed as an argument to this function.
 * @return A string representation of the collection.
 */
fun <E> Collection<E>.pluralString(
  empty: () -> String,
  single: (E) -> String,
  moreThanOne: (Iterable<E>) -> String
): String = when (size) {
  0 -> empty()
  1 -> single(single())
  else -> moreThanOne(this)
}

/**
 * Finds the indices of all elements in the receiver collection that match the given predicate.
 *
 * @param predicate A function that determines if an
 *   element's index should be included in the output list.
 * @receiver The collection to be searched.
 * @return A list containing the indices of all elements that match the predicate.
 */
inline fun <E> Collection<E>.indicesOf(predicate: (E) -> Boolean): List<Int> {
  return buildList {
    for ((index, e) in this@indicesOf.withIndex()) {
      if (predicate(e)) {
        add(index)
      }
    }
  }
}

/**
 * Returns a list containing the elements from the receiver iterable up to and
 * including the first element for which the given predicate returns false.
 *
 * @param predicate A function that determines if an element should be included in the output list.
 * @receiver The iterable to be processed.
 * @return A list containing the elements from the receiver iterable up to
 *   and including the first element for which the predicate returns false.
 */
inline fun <E> Iterable<E>.takeWhileInclusive(predicate: (E) -> Boolean): List<E> {
  return buildList {
    for (e in this@takeWhileInclusive) {
      add(e)
      if (!predicate(e)) break
    }
  }
}

/**
 * This function iterates over the receiver iterable and adds each element
 * to the current chunk. Whenever the selector function returns true for an
 * element, that element concludes the current chunk and a new chunk is started.
 *
 * @param selector A function that determines when to
 *   conclude the current chunk and start a new one.
 * @receiver The iterable to be split into chunks.
 * @return A list of chunks, where each chunk is a list of elements from the receiver iterable.
 */
fun <E> Iterable<E>.chunkedBy(selector: (E) -> Boolean): List<List<E>> {
  return fold(mutableListOf<MutableList<E>>(mutableListOf())) { acc, e ->
    acc.last().add(e)
    acc.alsoIf(selector(e)) {
      it.add(mutableListOf())
    }
  }
}
