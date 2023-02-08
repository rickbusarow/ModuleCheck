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

@file:Suppress("UndocumentedPublicClass", "UndocumentedPublicFunction")

package modulecheck.utils.lazy

import kotlinx.coroutines.DisposableHandle
import org.jetbrains.kotlin.com.intellij.openapi.Disposable

interface LazyResets<out T : Any> : Lazy<T>, Resets

interface Resets {
  fun reset()
}

inline fun <reified T : Any> ResetManager.lazyResets(
  noinline valueFactory: () -> T
): LazyResets<T> = LazyResets(this, valueFactory)

fun <T : Any> LazyResets(
  resetManager: ResetManager,
  valueFactory: () -> T
): LazyResets<T> = LazyResetsImpl(resetManager, valueFactory)

internal class LazyResetsImpl<out T : Any>(
  private val resetManager: ResetManager,
  private val valueFactory: () -> T
) : LazyResets<T> {

  private var lazyHolder: Lazy<T> = createLazy()

  override val value: T
    get() = lazyHolder.value

  override fun isInitialized(): Boolean = lazyHolder.isInitialized()

  private fun createLazy() = lazy {
    resetManager.register(this)
    valueFactory()
  }

  override fun reset() {
    lazyHolder = createLazy()
  }
}

interface ResetManager : Resets, Disposable {
  fun register(delegate: Resets)

  override fun dispose()

  override fun reset()
  fun child(childDelegates: MutableCollection<Resets> = mutableListOf()): ResetManager

  companion object {
    operator fun invoke(): ResetManager = RealResetManager()
  }
}

object EmptyResetManager : ResetManager {
  override fun register(delegate: Resets): Unit = Unit
  override fun dispose(): Unit = Unit
  override fun reset(): Unit = Unit
  override fun child(childDelegates: MutableCollection<Resets>): EmptyResetManager = this
}

class RealResetManager(
  private val delegates: MutableCollection<Resets> = mutableListOf()
) : DisposableHandle, ResetManager {

  override fun register(delegate: Resets) {
    synchronized(delegates) {
      delegates.add(delegate)
    }
  }

  override fun dispose() {
    reset()
  }
  override fun reset() {
    synchronized(delegates) {
      delegates.forEach { it.reset() }
      delegates.clear()
    }
  }

  override fun child(childDelegates: MutableCollection<Resets>): RealResetManager {
    return RealResetManager(childDelegates)
      .also { child -> register(child) }
  }
}
