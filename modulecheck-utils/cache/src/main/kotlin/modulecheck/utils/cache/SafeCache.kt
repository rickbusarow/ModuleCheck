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

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import modulecheck.utils.cache.RealSafeCache.ValueWrapper.NULL
import modulecheck.utils.cache.RealSafeCache.ValueWrapper.Value
import modulecheck.utils.coroutines.mapAsyncNotNull
import java.util.concurrent.ConcurrentHashMap

interface SafeCache<K : Any, V> {

  val values: Flow<V>

  suspend fun getOrPut(key: K, defaultValue: suspend () -> V): V

  companion object {
    operator fun <K : Any, V> invoke(
      delegate: ConcurrentHashMap<K, V> = ConcurrentHashMap(),
      lockCache: ConcurrentHashMap<K, Mutex> = ConcurrentHashMap<K, Mutex>()
    ): SafeCache<K, V> = RealSafeCache(delegate, lockCache)
  }
}

internal class RealSafeCache<K : Any, V>(
  delegate: ConcurrentHashMap<K, V> = ConcurrentHashMap(),
  private val lockCache: ConcurrentHashMap<K, Mutex> = ConcurrentHashMap<K, Mutex>()
) : SafeCache<K, V> {

  private val delegate: ConcurrentHashMap<K, ValueWrapper<V>> =
    ConcurrentHashMap(
      delegate.mapValues { (_, value) ->
        value.wrapper()
      }
    )

  override val values: Flow<V>
    get() = delegate.keys
      .mapAsyncNotNull { key ->
        lockForKey(key).withLock { delegate[key]?.unwrap() }
      }

  private fun lockForKey(key: K): Mutex {
    return lockCache.computeIfAbsent(key) { Mutex(locked = false) }
  }

  override suspend fun getOrPut(key: K, defaultValue: suspend () -> V): V {

    val lock = lockForKey(key)

    // Wrap the insert code inside a Mutex because `getOrPut` isn't synchronized, so multiple
    // requests for the same key before a value has been cached will result in duplicated work.
    return lock.withLock {
      @Suppress("UNCHECKED_CAST")
      delegate[key]?.unwrap() ?: defaultValue()
        .wrapper()
        .let { default ->
          delegate.putIfAbsent(key, default) ?: default
        }
        .unwrap() as V
    }
  }

  private fun V?.wrapper(): ValueWrapper<V> {
    return if (this != null) {
      Value(this)
    } else {
      NULL
    }
  }

  private sealed interface ValueWrapper<out V> {
    data class Value<V>(val data: V) : ValueWrapper<V>
    object NULL : ValueWrapper<Nothing>

    fun unwrap(): V? = when (this) {
      NULL -> null
      is Value -> data
    }
  }
}
