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

import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

/**
 * Lazily invokes [action] the first time [await][Deferred.await] or [join][kotlinx.coroutines.Job.join] is called.
 *
 * This [action] is only invoked once.  The caller's coroutine is used to execute this action.
 *
 * ```
 * val expensive = lazyDeferred { repository.getAll() }
 *
 * suspend fun getExpensive() = expensive.await()
 * ```
 */
fun <T> lazyDeferred(action: suspend () -> T): LazyDeferred<T> {

  return LazyDeferredImpl(
    action = action,
    delegate = CompletableDeferred(),
    lock = Mutex(false)
  )
}

fun interface LazyDeferred<T> {
  suspend fun await(): T
}

suspend fun <T> Collection<LazyDeferred<T>>.awaitAll(): List<T> {
  return if (isEmpty()) {
    emptyList()
  } else {
    coroutineScope {
      map { it.await() }
    }
  }
}

internal class LazyDeferredImpl<T>(
  private val action: suspend () -> T,
  private val delegate: CompletableDeferred<T>,
  private val lock: Mutex
) : LazyDeferred<T> {

  override suspend fun await(): T {
    lock.withLock {
      if (!delegate.isCompleted) {
        delegate.complete(action())
      }
    }
    return delegate.await()
  }
}

fun <T, R> List<T>.mapBlocking(transform: suspend (T) -> R): List<R> {
  return map { runBlocking { transform(it) } }
}

fun <T> Sequence<T>.filterBlocking(predicate: suspend (T) -> Boolean): Sequence<T> {
  return filter { runBlocking { predicate(it) } }
}

fun <T> Sequence<T>.filterNotBlocking(predicate: suspend (T) -> Boolean): Sequence<T> {
  return filterNot { runBlocking { predicate(it) } }
}

fun <T, R> Sequence<T>.mapBlocking(transform: suspend (T) -> R): Sequence<R> {
  return map { runBlocking { transform(it) } }
}

fun <T, R> Sequence<T>.flatMapBlocking(transform: suspend (T) -> Iterable<R>): Sequence<R> {
  return flatMap { runBlocking { transform(it) } }
}
