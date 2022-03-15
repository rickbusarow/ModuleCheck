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
import modulecheck.utils.LazySet.DataSource
import modulecheck.utils.LazySet.DataSource.Priority
import modulecheck.utils.LazySet.DataSource.Priority.LOW
import modulecheck.utils.LazySet.DataSource.Priority.MEDIUM
import modulecheck.utils.LazySet.State
import java.util.concurrent.atomic.AtomicReference

sealed interface LazySet<E> : Flow<E> {

  val isFullyCached: Boolean

  suspend fun contains(element: E): Boolean

  fun snapshot(): State<E>

  interface DataSource<E> : Comparable<DataSource<E>> {

    val priority: Priority

    suspend fun get(): Set<E>

    enum class Priority : Comparable<Priority> {
      HIGH,
      MEDIUM,
      LOW
    }

    override fun compareTo(other: DataSource<E>): Int {
      return priority.compareTo(other.priority)
    }
  }

  class State<E>(
    val cache: Set<E>,
    val remaining: List<DataSource<E>>
  ) {
    private val remainingMap by lazy {
      remaining.groupBy { it.priority }
    }

    fun nextSources(): Sequence<List<DataSource<E>>> {

      return sequence {

        Priority.values()
          .forEach { priority ->

            val sources = remainingMap[priority]

            if (sources != null) {
              yield(sources)
            }
          }
      }
    }
  }
}

fun <E> LazyDeferred<Set<E>>.asDataSource(
  priority: Priority = MEDIUM
): DataSource<E> = dataSource(priority) { await() }

fun <E> Lazy<Set<E>>.asDataSource(
  priority: Priority = MEDIUM
): DataSource<E> = dataSource(priority) { value }

fun <E> emptyDataSource(): DataSource<E> = object : DataSource<E> {
  override val priority: Priority
    get() = LOW

  override suspend fun get(): Set<E> = emptySet()
}

fun <E> dataSource(
  priority: Priority = MEDIUM,
  factory: suspend () -> Set<E>
): DataSource<E> = DataSourceImpl(priority, factory)

fun <E> lazyDataSource(
  priority: Priority = MEDIUM,
  factory: suspend () -> Set<E>
): DataSource<E> {

  val lazyFactory = lazyDeferred { factory() }

  return DataSourceImpl(priority) { lazyFactory.await() }
}

@PublishedApi
internal class DataSourceImpl<E>(
  override val priority: Priority,
  private val factory: suspend () -> Set<E>
) : DataSource<E> {

  override suspend fun get(): Set<E> = factory()

  override fun equals(other: Any?): Boolean {
    if (this === other) return true
    if (javaClass != other?.javaClass) return false

    other as DataSourceImpl<*>

    if (factory != other.factory) return false

    return true
  }

  override fun hashCode(): Int = factory.hashCode()
}

fun <E> lazySet(
  vararg children: LazySet<E>,
  source: DataSource<E>
): LazySet<E> {
  return createLazySet(children.toList(), listOf(source))
}

fun <E> lazySet(
  children: Collection<LazySet<E>>,
  source: DataSource<E>
): LazySet<E> {
  return createLazySet(children, listOf(source))
}

fun <E> lazySet(
  vararg children: LazySet<E>
): LazySet<E> {
  return createLazySet(children.toList(), listOf())
}

fun <E> lazySet(
  vararg source: DataSource<E>
): LazySet<E> {
  return createLazySet(listOf(), source.toList())
}

fun <E> lazySet(
  children: Collection<LazySet<E>> = emptyList(),
  sources: Collection<DataSource<E>>
): LazySet<E> {
  return createLazySet(children.toList(), sources)
}

fun <E> emptyLazySet(): LazySet<E> {
  return createLazySet(listOf(), listOf())
}

internal fun <E> createLazySet(
  children: Collection<LazySet<E>>,
  sources: Collection<DataSource<E>>
): LazySet<E> {

  val childCaches = children
    .map { child -> child.snapshot() }

  val cache = childCaches.flatMapToSet { it.cache }
  val remaining = childCaches.flatMapToSet { it.remaining }
    .plus(sources)
    .sortedByDescending { it.priority }

  return LazySetImpl(cache, remaining)
}



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

  override suspend fun contains(element: E): Boolean {

    val snap = state.get()

    if (snap.cache.contains(element)) return true

    return snap.remainingFlow()
      .any { it.contains(element) }
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
