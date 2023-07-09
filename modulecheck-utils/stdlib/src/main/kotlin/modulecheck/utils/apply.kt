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
 * Applies the given block to each element in the iterable and returns the receiver object.
 *
 * @param elements The iterable collection of elements to apply the block to.
 * @param block The block of code to apply to each element.
 * @return The receiver object after applying the block to each element.
 */
inline fun <T, E> T.applyEach(elements: Iterable<E>, block: T.(e: E) -> Unit): T = apply {
  for (element in elements) {
    block(element)
  }
}

/**
 * Applies the given block to each element in the iterable and returns the receiver object.
 *
 * @param elements The iterable collection of elements to apply the block to.
 * @param block The block of code to apply to each element.
 * @return The receiver object after applying the block to each element.
 */
inline fun <T, E> T.applyEachIndexed(
  elements: Iterable<E>,
  block: T.(index: Int, e: E) -> Unit
): T = apply {
  for ((index, element) in elements.withIndex()) {
    block(index, element)
  }
}

/**
 * Applies the given block to each element in the iterable and returns the receiver object.
 *
 * @param elements The iterable collection of elements to apply the block to.
 * @param block The block of code to apply to each element.
 * @return The receiver object after applying the block to each element.
 */
inline fun <T, E> T.applyEachIndexed(elements: Array<E>, block: T.(index: Int, e: E) -> Unit): T =
  apply {
    for ((index, element) in elements.withIndex()) {
      block(index, element)
    }
  }

/**
 * Applies the given blocks to the receiver object.
 *
 * shorthand for `for (block in blocks) apply(block)`
 *
 * @param blocks The blocks of code to apply to each element.
 * @return The receiver object after applying the block to each element.
 */
@Suppress("NOTHING_TO_INLINE")
inline fun <T> T.applyEach(blocks: Iterable<T.() -> Unit>): T = apply {
  for (block in blocks) block()
}

/**
 * Applies the given blocks to the receiver object.
 *
 * shorthand for `for (block in blocks) apply(block)`
 *
 * @param blocks The blocks of code to apply to each element.
 * @return The receiver object after applying the block to each element.
 */
@Suppress("NOTHING_TO_INLINE")
inline fun <T> T.applyEach(vararg blocks: T.() -> Unit): T = apply {
  for (block in blocks) block()
}

/**
 * Conditionally applies the provided block to the receiver object
 * if the predicate is true and returns the modified object.
 *
 * @param predicate The predicate to determine whether to apply the block.
 * @param body The block of code to apply to the receiver object.
 * @return The modified receiver object if the predicate
 *   is true, or the original receiver object otherwise.
 */
inline fun <T> T.applyIf(predicate: Boolean, body: T.() -> T): T = apply {
  if (predicate) {
    body()
  }
}

/**
 * Conditionally applies the provided transform function to the receiver
 * object if the predicate is true, then returns the result of that transform.
 * If the predicate is false, the receiver object itself is returned.
 *
 * @param predicate The predicate to determine whether to apply the transform function.
 * @param transform The transform function to apply to the receiver object.
 * @return The result of the transform function if the
 *   predicate is true, or the receiver object itself otherwise.
 */
inline fun <T> T.letIf(predicate: Boolean, transform: (T) -> T): T {
  return if (predicate) transform(this) else this
}

/**
 * Conditionally applies the provided transformation function to the
 * receiver object if the predicate is true and returns the result.
 * If the predicate is false, the receiver object itself is returned.
 *
 * @param predicate The predicate to determine whether to apply the transformation function.
 * @param body The transformation function to apply to the receiver object.
 * @return The result of the transformation function if the
 *   predicate is true, or the receiver object itself otherwise.
 */
inline fun <T> T.alsoIf(predicate: Boolean, body: (T) -> Unit): T {
  if (predicate) {
    body(this)
  }
  return this
}
