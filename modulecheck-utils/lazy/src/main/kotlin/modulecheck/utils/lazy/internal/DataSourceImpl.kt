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

import modulecheck.utils.lazy.LazyDeferred
import modulecheck.utils.lazy.LazySet

@PublishedApi
internal class DataSourceImpl<E>(
  override val priority: LazySet.DataSource.Priority,
  private val lazyDeferred: LazyDeferred<Set<E>>
) : LazySet.DataSource<E>, LazyDeferred<Set<E>> by lazyDeferred {

  override suspend fun get(): Set<E> = await()

  override fun equals(other: Any?): Boolean {
    if (this === other) return true
    if (javaClass != other?.javaClass) return false

    other as DataSourceImpl<*>

    if (lazyDeferred != other.lazyDeferred) return false

    return true
  }

  override fun hashCode(): Int = lazyDeferred.hashCode()
}
