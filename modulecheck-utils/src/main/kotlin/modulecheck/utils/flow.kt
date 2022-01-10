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

import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.channelFlow
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.fold
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch

suspend fun <T> Flow<T>.any(predicate: suspend (T) -> Boolean): Boolean {
  val matching = firstOrNull(predicate)

  return matching != null
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

@OptIn(ExperimentalCoroutinesApi::class)
fun <T, R> Flow<T>.mapAsync(transform: suspend (T) -> R): Flow<R> {
  return channelFlow {
    this@mapAsync.onEach { send(transform(it)) }
      .launchIn(this)
  }
}

@OptIn(ExperimentalCoroutinesApi::class)
fun <T, R> Iterable<T>.mapAsync(transform: suspend (T) -> R): Flow<R> {
  return channelFlow {
    forEach { launch { send(transform(it)) } }
  }
}

@OptIn(ExperimentalCoroutinesApi::class)
fun <T, R> Sequence<T>.mapAsync(transform: suspend (T) -> R): Flow<R> {
  return channelFlow {
    forEach { launch { send(transform(it)) } }
  }
}

@OptIn(ExperimentalCoroutinesApi::class)
fun <T, R : Any> Flow<T>.mapAsyncNotNull(transform: suspend (T) -> R?): Flow<R> {
  return channelFlow {
    this@mapAsyncNotNull.onEach { element -> transform(element)?.let { send(it) } }
      .launchIn(this)
  }
}

@OptIn(ExperimentalCoroutinesApi::class)
fun <T, R : Any> Iterable<T>.mapAsyncNotNull(transform: suspend (T) -> R?): Flow<R> {
  return channelFlow {
    forEach { element ->
      launch { transform(element)?.let { send(it) } }
    }
  }
}

@OptIn(ExperimentalCoroutinesApi::class)
fun <T, R : Any> Sequence<T>.mapAsyncNotNull(transform: suspend (T) -> R?): Flow<R> {
  return channelFlow {
    forEach { element ->
      launch { transform(element)?.let { send(it) } }
    }
  }
}

@OptIn(ExperimentalCoroutinesApi::class)
fun <T> Flow<T>.filterAsync(predicate: suspend (T) -> Boolean): Flow<T> {
  return channelFlow {
    this@filterAsync.onEach { if (predicate(it)) send(it) }
      .launchIn(this)
  }
}

@OptIn(ExperimentalCoroutinesApi::class)
fun <T> Iterable<T>.filterAsync(predicate: suspend (T) -> Boolean): Flow<T> {
  return channelFlow {
    forEach { launch { if (predicate(it)) send(it) } }
  }
}

@OptIn(ExperimentalCoroutinesApi::class)
fun <T> Sequence<T>.filterAsync(predicate: suspend (T) -> Boolean): Flow<T> {
  return channelFlow {
    forEach { launch { if (predicate(it)) send(it) } }
  }
}
