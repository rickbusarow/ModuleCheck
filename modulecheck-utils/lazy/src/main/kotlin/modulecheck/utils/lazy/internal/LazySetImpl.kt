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

package modulecheck.utils.lazy.internal

import kotlinx.coroutines.flow.AbstractFlow
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.FlowCollector
import kotlinx.coroutines.flow.asFlow
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.take
import modulecheck.utils.containsAny
import modulecheck.utils.coroutines.any
import modulecheck.utils.coroutines.distinct
import modulecheck.utils.coroutines.flatMapSetConcat
import modulecheck.utils.coroutines.mapAsync
import modulecheck.utils.lazy.LazySet
import modulecheck.utils.lazy.LazySet.DataSource
import modulecheck.utils.lazy.LazySet.State
import java.util.concurrent.atomic.AtomicReference

internal class LazySetImpl<E>(
  cache: Set<E>,
  sources: List<DataSource<E>>
) : AbstractFlow<E>(), LazySet<E> {

  internal val state = AtomicReference(
    State(
      cache = cache,
      remaining = sources
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
    var reachedElement = true
    take(1)
      .collect { reachedElement = true }
    return !reachedElement
  }

  override suspend fun isNotEmpty(): Boolean = !isEmpty()

  override suspend fun contains(element: Any?): Boolean {

    val snap = state.get()

    return snap.cache.contains(element) || snap.remainingFlow()
      .any { it.contains(element) }
  }

  override suspend fun containsAny(other: LazySet<Any?>): Boolean {

    // contents will match if this is reflexive
    if (this === other) return true

    val snap = snapshot()
    val thisCached = snap.cache
    val thisRemaining = snap.remainingFlow()

    val otherSnap = other.snapshot()
    val otherCached = otherSnap.cache
    val otherRemaining = with(other as LazySetImpl<Any?>) {
      otherSnap.remainingFlow()
    }

    // avoid pulling non-cached data if possible
    // prioritize building the cache for "this" before adding to the cache of the other LazySet

    return when {
      // cached vs other cached
      thisCached.containsAny(otherCached) -> true

      snap.remaining.isEmpty() && otherSnap.remaining.isEmpty() -> false

      // not-cached vs other cached, so that we build up "this" LazySet's cache first
      thisRemaining.any { remainingSetChunk ->
        remainingSetChunk.containsAny(otherCached)
      } -> true

      else -> {
        // At this point, the "this" LazySet is fully cached.  So if there's a matching element,
        // it must be in a "remaining" data source from the other.
        val fullCache = snapshot().cache
        otherRemaining.any { otherRemainingChunk ->
          fullCache.containsAny(otherRemainingChunk)
        }
      }
    }
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
          .flatMapSetConcat()
          .also { updateCache(it, nextSources) }
      }
  }

  override suspend fun collectSafely(collector: FlowCollector<E>) {

    val snap = state.get()

    val distinctFlow = flow distinctFlow@{

      this@distinctFlow.emitAll(snap.cache.asFlow())

      @Suppress("MagicNumber")
      snap.remaining.sorted()
        .chunked(100)
        .forEach { dataSourceChunk ->

          val new = dataSourceChunk
            .mapAsync { dataSource -> dataSource.get() }
            .flatMapSetConcat()

          updateCache(new, dataSourceChunk)

          this@distinctFlow.emitAll(new.asFlow())
        }
    }
      .distinct()

    collector.emitAll(distinctFlow)
  }
}
