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

import kotlin.properties.ReadWriteProperty
import kotlin.reflect.KProperty

fun <T> unsafeLazy(initializer: () -> T): Lazy<T> =
  lazy(mode = LazyThreadSafetyMode.NONE, initializer = initializer)

/**
 * just a var, but the initial value is lazy
 *
 * @since 0.13.0
 */
fun <T> lazyVar(initializer: () -> T): ReadWriteProperty<Any?, T> = SynchronizedLazyVar(initializer)

private class SynchronizedLazyVar<T>(initializer: () -> T) : ReadWriteProperty<Any?, T> {

  @Volatile
  private var isSet = false
  private var initializer: (() -> T)? = initializer
  private var value: Any? = null

  @Suppress("UNCHECKED_CAST")
  override fun getValue(thisRef: Any?, property: KProperty<*>): T {

    if (isSet) {
      return value as T
    }
    synchronized(this) {
      if (!isSet) {
        value = initializer!!.invoke()
        isSet = true
        initializer = null
      }
      return value as T
    }
  }

  override fun setValue(thisRef: Any?, property: KProperty<*>, value: T) {
    synchronized(this) {
      this.value = value
      isSet = true
    }
  }
}
