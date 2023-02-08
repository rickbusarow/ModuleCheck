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

package modulecheck.utils.cache

import com.github.benmanes.caffeine.cache.Cache
import com.github.benmanes.caffeine.cache.Caffeine
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.supervisorScope
import modulecheck.utils.coroutines.mapAsyncNotNull
import modulecheck.utils.lazy.LazyDeferred
import modulecheck.utils.lazy.lazyDeferred
import modulecheck.utils.trace.HasTraceTags
import modulecheck.utils.trace.Trace
import modulecheck.utils.trace.traced

/**
 * A thread (and coroutine) -safe cache, with automatic eviction.
 *
 * When accessing data via [getOrPut], the operation inside the lambda is guaranteed to only execute
 * once for each key -- unless the previous data has been evicted from the cache.
 *
 * @since 0.12.0
 */
interface SafeCache<K : Any, V> : HasTraceTags {

  val values: Flow<V>

  /**
   * This is conceptually similar to
   * [ConcurrentHashMap.computeIfAbsent][java.util.concurrent.ConcurrentHashMap.computeIfAbsent].
   *
   * @param key the unique key for the desired value
   * @param loader the action to perform if [key] does not already have a value in the cache. This
   *   action is guaranteed only to be performed once per key.
   * @return the value associated with this [key]
   * @since 0.12.0
   */
  suspend fun getOrPut(key: K, loader: suspend () -> V): V

  companion object {
    /**
     * @return a [SafeCache] with initial initialValues of [initialValues]
     * @since 0.12.0
     */
    operator fun <K : Any, V> invoke(
      tags: Iterable<Any>,
      initialValues: Map<K, V> = emptyMap()
    ): SafeCache<K, V> {

      val tagsList = tags.toList()

      check(tagsList.isNotEmpty()) {
        "You must provide at least one tag when creating a ${SafeCache::class.java.simpleName}."
      }
      return RealSafeCache(tagsList, initialValues.toList())
    }

    /**
     * @return a [SafeCache] with initial initialValues of [initialValues]
     * @since 0.12.0
     */
    operator fun <K : Any, V> invoke(
      tags: Iterable<Any>,
      vararg initialValues: Pair<K, V>
    ): SafeCache<K, V> {

      val tagsList = tags.toList()

      check(tagsList.isNotEmpty()) {
        "You must provide at least one tag when creating a ${SafeCache::class.java.simpleName}."
      }
      return RealSafeCache(tagsList, initialValues = initialValues.toList())
    }
  }
}

internal class RealSafeCache<K : Any, V>(
  override val tags: Iterable<Any>,
  initialValues: List<Pair<K, V>>
) : SafeCache<K, V> {

  /**
   * Note that the api surface is that of a `Cache<K, V>`, but this is using a `LazyDeferred<V>`.
   * This allows all "loader" operations to be light-weight and non-recursive.
   *
   * @see getOrPut
   * @since 0.12.0
   */
  private val delegate: Cache<K, LazyDeferred<V>> = Caffeine.newBuilder()
    .build<K, LazyDeferred<V>>()
    .also {
      val initialMap = initialValues.associate { (key, value) ->
        key to lazyDeferred { value }
      }
      it.putAll(initialMap)
    }

  override val values: Flow<V>
    get() = delegate.asMap()
      .values
      .mapAsyncNotNull { it.await() }

  override suspend fun getOrPut(key: K, loader: suspend () -> V): V {
    return traced(key) {
      supervisorScope {
        try {
          // Note that the cache is only holding a LazyDeferred,
          // and we return that LazyDeferred without actually calling `await()`.
          val deferred = delegate.get(key) { lazyDeferred { loader() } }

          deferred.await()
        } catch (e: IllegalStateException) {

          val trace = currentCoroutineContext()[Trace] ?: throw e
          throw IllegalStateException("${e.message}\n\n${trace.asString()}\n\n", e)
        }
      }
    }
  }
}
