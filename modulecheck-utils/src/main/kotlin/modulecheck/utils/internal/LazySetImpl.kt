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

package modulecheck.utils.internal

import kotlinx.coroutines.flow.AbstractFlow
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.FlowCollector
import kotlinx.coroutines.flow.asFlow
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onCompletion
import kotlinx.coroutines.flow.take
import modulecheck.utils.LazySet
import modulecheck.utils.LazySet.DataSource
import modulecheck.utils.LazySet.State
import modulecheck.utils.any
import modulecheck.utils.distinct
import modulecheck.utils.flatMapSetConcat
import modulecheck.utils.mapAsync
import java.util.concurrent.atomic.AtomicReference

internal class LazySetImpl<E>(
  cache: Set<E>,
  sources: List<DataSource<E>>
) : AbstractFlow<E>(), LazySet<E> {

  internal val state = AtomicReference(
    State(
      cache = cache, remaining = sources
    )
  )

  override val isFullyCached: Boolean
    get() = state.get().remaining.isEmpty()

  override fun snapshot(): State<E> = state.get()

  override suspend fun isEmpty(): Boolean {
    val snap = snapshot()

    if (snap.remaining.isEmpty()) {
      return snap.cache.isEmpty()
    }

    if (snap.cache.isNotEmpty()) {
      return false
    }

    // There can be a queue of DataSources lined up, but they may all be empty.
    // In order to determine whether the set has any more elements, we have to iterate over that
    // queue, unwrapping all those DataSources.  As soon as we reach any element, we know the set
    // isn't empty and we can return.
    var reachedElement = false
    take(1)
      .collect { reachedElement = true }
    return !reachedElement
  }

  override suspend fun isNotEmpty(): Boolean = !isEmpty()

  override suspend fun contains(element: Any?): Boolean {

    val snap = state.get()

    return (
      snap.cache.contains(element) || snap.remainingFlow()
        .any { it.contains(element) }
      )
  }

  private fun updateCache(new: Set<E>, completed: List<DataSource<E>>) {

    var fromAtomic: State<E>
    var newPair: State<E>

    do {
      fromAtomic = state.get()
      newPair = State(
        cache = fromAtomic.cache + new,
        remaining = fromAtomic.remaining.minus(completed.toSet())
      )
    } while (!state.compareAndSet(fromAtomic, newPair))
  }

  private fun State<E>.remainingFlow(): Flow<Set<E>> {

    return nextSources().asFlow()
      .map { nextSources ->

        nextSources.mapAsync { it.get() }
          .flatMapSetConcat { it }
          .also { newData -> updateCache(newData, nextSources) }
      }
  }

  override suspend fun collectSafely(collector: FlowCollector<E>) {

    val snap = state.get()

    val additionalCache = mutableSetOf<E>()
    val completed: MutableList<DataSource<E>> = mutableListOf()

    val distinctFlow = flow {

      emitAll(snap.cache.asFlow())

      @Suppress("MagicNumber")
      snap.remaining.sorted()
        .chunked(100)
        .forEach { dataProviderChunk ->

          val new = dataProviderChunk
            .mapAsync { provider -> provider.get() }
            .flatMapSetConcat { it }

          additionalCache.addAll(new)
          completed.addAll(dataProviderChunk)

          emitAll(new.asFlow())
        }
    }
      .distinct()
      .onCompletion { updateCache(additionalCache, completed) }

    collector.emitAll(distinctFlow)
  }
}
