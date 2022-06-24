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

package modulecheck.utils.coroutines

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.channelFlow
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.fold
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onCompletion
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withPermit
import java.lang.Integer.max

suspend fun <T> Flow<T>.any(predicate: suspend (T) -> Boolean): Boolean {
  val matching = firstOrNull(predicate)

  return matching != null
}

fun <T> Flow<T>.distinct(): Flow<T> = flow {
  val past = mutableSetOf<T>()
  collect {
    if (past.add(it)) emit(it)
  }
}

suspend fun <T> Flow<T>.contains(element: T): Boolean {
  return any { it == element }
}

suspend fun <T, R> Flow<T>.flatMapListConcat(
  destination: MutableList<R> = mutableListOf(),
  transform: suspend (T) -> List<R>
): List<R> {
  return fold(destination) { acc, value ->
    acc.also { it.addAll(transform(value)) }
  }
}

suspend fun <T, R> Flow<T>.flatMapSetConcat(
  destination: MutableSet<R> = mutableSetOf(),
  transform: suspend (T) -> Set<R>
): Set<R> {
  return fold(destination) { acc, value ->
    acc.also { it.addAll(transform(value)) }
  }
}

private val DEFAULT_CONCURRENCY: Int
  get() = max(Runtime.getRuntime().availableProcessors(), 2)

fun <T, R> Flow<T>.mapAsync(
  concurrency: Int = DEFAULT_CONCURRENCY,
  transform: suspend (T) -> R
): Flow<R> {
  val semaphore = Semaphore(concurrency)
  return channelFlow {
    semaphore.withPermit {
      this@mapAsync.onEach { send(transform(it)) }
        .launchIn(this)
    }
  }
}

fun <T, R> Iterable<T>.mapAsync(
  concurrency: Int = DEFAULT_CONCURRENCY,
  transform: suspend (T) -> R
): Flow<R> {

  val semaphore = Semaphore(concurrency)
  return channelFlow {
    forEach {
      semaphore.withPermit {
        launch { send(transform(it)) }
      }
    }
  }
}

fun <T> Iterable<T>.onEachAsync(
  concurrency: Int = DEFAULT_CONCURRENCY,
  action: suspend (T) -> Unit
): Flow<T> {

  val semaphore = Semaphore(concurrency)
  return channelFlow {
    forEach {
      semaphore.withPermit {
        launch {
          action(it)
          send(it)
        }
      }
    }
  }
}

fun <T, R> Sequence<T>.mapAsync(
  concurrency: Int = DEFAULT_CONCURRENCY,
  transform: suspend (T) -> R
): Flow<R> {
  val semaphore = Semaphore(concurrency)
  return channelFlow {
    forEach {
      semaphore.withPermit { launch { send(transform(it)) } }
    }
  }
}

fun <T, R : Any> Flow<T>.mapAsyncNotNull(transform: suspend (T) -> R?): Flow<R> {
  return channelFlow {
    this@mapAsyncNotNull.onEach { element -> transform(element)?.let { send(it) } }
      .launchIn(this)
  }
}

fun <T, R : Any> Iterable<T>.mapAsyncNotNull(transform: suspend (T) -> R?): Flow<R> {
  return channelFlow {
    forEach { element ->
      launch { transform(element)?.let { send(it) } }
    }
  }
}

fun <T, R : Any> Sequence<T>.mapAsyncNotNull(transform: suspend (T) -> R?): Flow<R> {
  return channelFlow {
    forEach { element ->
      launch { transform(element)?.let { send(it) } }
    }
  }
}

fun <T> Flow<T>.filterAsync(predicate: suspend (T) -> Boolean): Flow<T> {
  return channelFlow {
    this@filterAsync.onEach { if (predicate(it)) send(it) }
      .launchIn(this)
  }
}

fun <T> Iterable<T>.filterAsync(predicate: suspend (T) -> Boolean): Flow<T> {
  return channelFlow {
    forEach { launch { if (predicate(it)) send(it) } }
  }
}

fun <T> Sequence<T>.filterAsync(predicate: suspend (T) -> Boolean): Flow<T> {
  return channelFlow {
    forEach { launch { if (predicate(it)) send(it) } }
  }
}

/**
 * Shorthand for `onCompletion { if (it == null) emitAll(other) }`
 *
 * ```
 * val mySingleFlow = someFlow + someOtherFlow
 * ```
 *
 * @since 0.13.0
 */
operator fun <T> Flow<T>.plus(other: Flow<T>): Flow<T> {
  return onCompletion { if (it == null) emitAll(other) }
}
