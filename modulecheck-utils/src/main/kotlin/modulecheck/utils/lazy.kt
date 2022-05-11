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

import kotlinx.coroutines.runBlocking

fun <T> unsafeLazy(initializer: () -> T): Lazy<T> =
  lazy(mode = LazyThreadSafetyMode.NONE, initializer = initializer)

class ResetManager(
  private val delegates: MutableCollection<Resets> = mutableListOf()
) {

  fun register(delegate: Resets) {
    synchronized(delegates) {
      delegates.add(delegate)
    }
  }

  fun resetAll() {
    synchronized(delegates) {
      delegates.forEach { it.reset() }
      delegates.clear()
    }
  }
}

interface LazyResets<out T : Any> : Lazy<T>, Resets

interface Resets {
  fun reset()
}

inline fun <reified T : Any> ResetManager.lazyResets(
  noinline valueFactory: suspend () -> T
): LazyResets<T> = LazyResets(this, valueFactory)

fun <T : Any> LazyResets(
  resetManager: ResetManager,
  valueFactory: suspend () -> T
): LazyResets<T> = LazyResetsImpl(resetManager, valueFactory)

internal class LazyResetsImpl<out T : Any>(
  private val resetManager: ResetManager,
  private val valueFactory: suspend () -> T
) : LazyResets<T> {

  private var lazyHolder: Lazy<T> = createLazy()

  override val value: T
    get() = lazyHolder.value

  override fun isInitialized(): Boolean = lazyHolder.isInitialized()

  private fun createLazy() = lazy {
    resetManager.register(this)
    runBlocking { valueFactory() }
  }

  override fun reset() {
    lazyHolder = createLazy()
  }
}
