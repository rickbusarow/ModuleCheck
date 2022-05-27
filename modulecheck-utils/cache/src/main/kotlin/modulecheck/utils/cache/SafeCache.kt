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

package modulecheck.utils.cache

import com.github.benmanes.caffeine.cache.Cache
import com.github.benmanes.caffeine.cache.Caffeine
import kotlinx.coroutines.flow.Flow
import modulecheck.utils.coroutines.mapAsyncNotNull
import modulecheck.utils.lazy.LazyDeferred
import modulecheck.utils.lazy.lazyDeferred

/**
 * A thread (and coroutine) -safe cache, with automatic eviction.
 *
 * When accessing data via [getOrPut], the operation inside the lambda is guaranteed to only execute
 * once for each key -- unless the previous data has been evicted from the cache.
 */
interface SafeCache<K : Any, V> {

  val values: Flow<V>

  /**
   * This is conceptually similar to
   * [ConcurrentHashMap.computeIfAbsent][java.util.concurrent.ConcurrentHashMap.computeIfAbsent].
   *
   * @param key the unique key for the desired value
   * @param loader the action to perform if [key] does not already have a value in the cache.
   *   This action is guaranteed only to be performed once per key.
   * @return the value associated with this [key]
   */
  suspend fun getOrPut(key: K, loader: suspend () -> V): V

  companion object {
    /**
     * @return a [SafeCache] with initial initialValues of [initialValues]
     */
    operator fun <K : Any, V> invoke(
      initialValues: Map<K, V> = emptyMap()
    ): SafeCache<K, V> = RealSafeCache(initialValues.toList())

    /**
     * @return a [SafeCache] with initial initialValues of [initialValues]
     */
    operator fun <K : Any, V> invoke(
      vararg initialValues: Pair<K, V>
    ): SafeCache<K, V> = RealSafeCache(initialValues.toList())
  }
}

internal class RealSafeCache<K : Any, V>(
  initialValues: List<Pair<K, V>>
) : SafeCache<K, V> {

  /**
   * Note that the api surface is that of a `Cache<K, V>`, but this is using a `LazyDeferred<V>`.
   * This allows all "loader" operations to be light-weight and non-recursive.
   *
   * @see getOrPut
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

    // Note that the cache is only holding a LazyDeferred, and we return that LazyDeferred without
    // actually calling `await()`.
    val deferred = delegate.get(key) { lazyDeferred { loader() } }

    return deferred.await()
  }
}
