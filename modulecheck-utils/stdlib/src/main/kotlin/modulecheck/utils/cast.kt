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
 * Casts this [Any] instance to the desired type [T].
 *
 * @return This instance cast to type [T].
 * @throws ClassCastException if this instance is not of type [T].
 */
@Suppress("UNCHECKED_CAST", "NOTHING_TO_INLINE")
inline fun <T> Any.cast(): T = this as T

/**
 * Attempts to cast this instance to the specified type
 * [T], returning `null` if the cast is not possible.
 *
 * **Usage:**
 * ```
 * val number: Any = "Not a number"
 * val integer: Int? = number.safeAs<Int>() // integer will be null
 * ```
 *
 * @return This instance cast to type [T], or `null` if the cast is not possible.
 */
inline fun <reified T : Any> Any?.safeAs(): T? = this as? T

/**
 * A functional version of [kotlin.requireNotNull]
 *
 * @return This instance, guaranteed not to be `null`.
 * @throws IllegalArgumentException if this instance is `null`.
 */
@Suppress("NOTHING_TO_INLINE")
inline fun <T : Any> T?.requireNotNull(): T = requireNotNull(this)

/**
 * A functional version of [kotlin.requireNotNull]
 *
 * @param lazyMessage The lambda function that generates the error message.
 * @return This instance, guaranteed not to be `null`.
 * @throws IllegalArgumentException if this instance is `null`.
 */
inline fun <T : Any> T?.requireNotNull(lazyMessage: () -> Any): T =
  requireNotNull(this, lazyMessage)

/**
 * A functional version of [kotlin.require]
 *
 * @param condition The lambda function that defines the condition.
 * @param lazyMessage The lambda function that generates the error message.
 * @return This instance, after passing the check.
 * @throws IllegalStateException if the condition is not met.
 */
inline fun <T> T.require(condition: (T) -> Boolean, lazyMessage: (T) -> Any): T = apply {
  check(condition(this)) { lazyMessage(this) }
}

/**
 * A functional version of [kotlin.checkNotNull]
 *
 * @return This instance, guaranteed not to be `null`.
 * @throws IllegalStateException if this instance is `null`.
 */
@Suppress("NOTHING_TO_INLINE")
inline fun <T : Any> T?.checkNotNull(): T = checkNotNull(this)

/**
 * A functional version of [kotlin.checkNotNull]
 *
 * @param lazyMessage The lambda function that generates the error message.
 * @return This instance, guaranteed not to be `null`.
 * @throws IllegalStateException if this instance is `null`.
 */
inline fun <T : Any> T?.checkNotNull(lazyMessage: () -> Any): T = checkNotNull(this, lazyMessage)

/**
 * A functional version of [kotlin.check]
 *
 * @param condition The lambda function that defines the condition.
 * @param lazyMessage The lambda function that generates the error message.
 * @return This instance, after passing the check.
 * @throws IllegalStateException if the condition is not met.
 */
inline fun <T> T.check(condition: (T) -> Boolean, lazyMessage: (T) -> Any): T = apply {
  check(condition(this)) { lazyMessage(this) }
}
