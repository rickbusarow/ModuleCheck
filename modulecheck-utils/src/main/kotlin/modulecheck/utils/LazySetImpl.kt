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

import kotlinx.coroutines.flow.AbstractFlow
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.FlowCollector
import kotlinx.coroutines.flow.asFlow
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onCompletion
import java.util.concurrent.atomic.AtomicReference

internal class LazySetImpl<E>(
  cache: Set<E>,
  sources: List<LazySet.DataSource<E>>
) : AbstractFlow<E>(), LazySet<E> {

  internal val state = AtomicReference(
    LazySet.State(
      cache = cache, remaining = sources
    )
  )

  override val isFullyCached: Boolean
    get() = state.get().remaining.isEmpty()

  override fun snapshot(): LazySet.State<E> = state.get()

  override suspend fun contains(element: Any?): Boolean {

    val snap = state.get()

    return (
      snap.cache.contains(element) || snap.remainingFlow()
        .any { it.contains(element) }
      )
  }

  private fun updateCache(new: Set<E>, completed: List<LazySet.DataSource<E>>) {

    var fromAtomic: LazySet.State<E>
    var newPair: LazySet.State<E>

    do {
      fromAtomic = state.get()
      newPair = LazySet.State(
        cache = fromAtomic.cache + new,
        remaining = fromAtomic.remaining.minus(completed.toSet())
      )
    } while (!state.compareAndSet(fromAtomic, newPair))
  }

  private fun LazySet.State<E>.remainingFlow(): Flow<Set<E>> {

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
    val completed: MutableList<LazySet.DataSource<E>> = mutableListOf()

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
