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

package modulecheck.utils.trace

import kotlin.coroutines.AbstractCoroutineContextElement
import kotlin.coroutines.CoroutineContext

class TraceCollector(
  private val cache: MutableList<Trace>
) : AbstractCoroutineContextElement(Key) {

  fun add(trace: Trace) = synchronized(cache) {
    cache.add(trace)

    trace.parentOrNull()?.let { parent ->
      cache.remove(parent)
    }
  }

  fun all() = cache

  companion object Key : CoroutineContext.Key<TraceCollector>
}
