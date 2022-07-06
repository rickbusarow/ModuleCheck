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

package modulecheck.utils.lazy

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.filterIsInstance
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.toSet
import modulecheck.utils.coroutines.any
import modulecheck.utils.flatMapToSet
import modulecheck.utils.lazy.LazySet.DataSource
import modulecheck.utils.lazy.LazySet.DataSource.Priority
import modulecheck.utils.lazy.LazySet.DataSource.Priority.MEDIUM
import modulecheck.utils.lazy.internal.DataSourceImpl
import modulecheck.utils.lazy.internal.LazySetImpl

interface LazySet<out E> : Flow<E>, LazySetComponent<E> {

  val isFullyCached: Boolean

  suspend fun contains(element: Any?): Boolean

  suspend fun isEmpty(): Boolean
  suspend fun isNotEmpty(): Boolean

  fun snapshot(): State<E>

  /**
   * A **Lazy** data source for a [LazySet], which performs some suspending operation [get] in order
   * to incrementally add data to the `LazySet`.
   */
  interface DataSource<out E> :
    Comparable<DataSource<*>>,
    LazySetComponent<E>,
    LazyDeferred<Set<E>> {

    /**
     * The priority which should be applied to this source while in a LazySet. Higher priority
     * sources are invoked first.
     */
    val priority: Priority

    /** Called to retrieve this source's data. Implementations are thread-safe and lazy. */
    suspend fun get(): Set<E>

    enum class Priority : Comparable<Priority> {
      HIGH,
      MEDIUM,
      LOW
    }

    override fun compareTo(other: DataSource<*>): Int {
      return priority.compareTo(other.priority)
    }
  }

  class State<out E>(
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

suspend inline fun <reified T : E, E> LazySet<E>.getOrNull(element: E): T? {
  return takeIf { it.contains(element) }
    ?.filterIsInstance<T>()
    ?.firstOrNull { it == element }
}

sealed interface LazySetComponent<out E>

suspend fun <T : B, E : B, B> LazySet<T>.containsAny(elements: Collection<E>): Boolean {
  return elements.any { contains(it) }
}

suspend fun <T : B, E : B, B> LazySet<T>.containsAny(elements: LazySet<E>): Boolean {
  return elements.any { contains(it) }
}

fun <E> Flow<E>.asDataSource(
  priority: Priority = MEDIUM
): DataSource<E> = dataSource(priority) { toSet() }

fun <E> LazyDeferred<Set<E>>.asDataSource(
  priority: Priority = MEDIUM
): DataSource<E> = dataSource(priority) { await() }

fun <E> Lazy<Set<E>>.asDataSource(
  priority: Priority = MEDIUM
): DataSource<E> = dataSource(priority) { value }

fun <E> dataSourceOf(
  vararg elements: E,
  priority: Priority = MEDIUM
): DataSource<E> = DataSourceImpl(priority, lazyDeferred { elements.toSet() })

/** @return A DataSource<E> from this [priority] and [factory] */
fun <E> dataSource(
  priority: Priority = MEDIUM,
  factory: LazyDeferred<Set<E>>
): DataSource<E> = DataSourceImpl(priority, factory)

/** @return A DataSource<E> from this [priority] and [factory] */
fun <E> dataSource(
  priority: Priority = MEDIUM,
  factory: suspend () -> Set<E>
): DataSource<E> {
  return DataSourceImpl(priority, lazyDeferred { factory() })
}

fun <E> Collection<LazySetComponent<E>>.toLazySet(): LazySet<E> = lazySet(this)

fun <E> lazySet(
  vararg children: LazySetComponent<E>
): LazySet<E> {
  return lazySet(children.asList())
}

fun <E> lazySet(
  priority: Priority = MEDIUM,
  dataSource: suspend () -> Set<E>
): LazySet<E> {
  return lazySet(dataSource(priority, dataSource))
}

@JvmName("lazySetSingle")
fun <E> lazySet(
  priority: Priority = MEDIUM,
  dataSource: suspend () -> E
): LazySet<E> {
  return lazySet(dataSource(priority) { setOf(dataSource()) })
}

fun <E> Flow<E>.toLazySet(
  priority: Priority = MEDIUM
): LazySet<E> {
  return lazySet(priority) { toSet() }
}

fun <E> lazySet(
  children: Collection<LazySetComponent<E>>
): LazySet<E> {
  val (sets, dataSources) = children.partition { it is LazySet<*> }
  @Suppress("UNCHECKED_CAST")
  return createLazySet(
    sets as List<LazySet<E>>,
    dataSources as List<DataSource<E>>
  )
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
