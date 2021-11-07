/*
 * Copyright (C) 2021 Rick Busarow
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

package modulecheck.gradle.internal

import org.gradle.api.model.ObjectFactory
import kotlin.properties.ReadWriteProperty
import kotlin.reflect.KProperty

internal inline fun <reified T : Any> ObjectFactory.setProperty(
  initialValue: Set<T> = emptySet()
): ReadWriteProperty<Any, Set<T>> =
  object : ReadWriteProperty<Any, Set<T>> {

    val delegate = setProperty(T::class.java).convention(initialValue)

    override fun getValue(thisRef: Any, property: KProperty<*>): Set<T> {
      return delegate.get()
    }

    override fun setValue(thisRef: Any, property: KProperty<*>, value: Set<T>) {
      delegate.set(value)
    }
  }

internal inline fun <reified T : Any> ObjectFactory.listProperty(
  initialValue: List<T> = emptyList<T>()
): ReadWriteProperty<Any, List<T>> =
  object : ReadWriteProperty<Any, List<T>> {

    val delegate = listProperty(T::class.java).convention(initialValue)

    override fun getValue(thisRef: Any, property: KProperty<*>): List<T> {
      return delegate.get()
    }

    override fun setValue(thisRef: Any, property: KProperty<*>, value: List<T>) {
      delegate.set(value)
    }
  }

internal inline fun <reified T : Any> ObjectFactory.property(
  initialValue: T
): ReadWriteProperty<Any, T> =
  object : ReadWriteProperty<Any, T> {

    val delegate = property(T::class.java).convention(initialValue)

    override fun getValue(thisRef: Any, property: KProperty<*>): T {
      return delegate.get()
    }

    override fun setValue(thisRef: Any, property: KProperty<*>, value: T) {
      delegate.set(value)
    }
  }

internal inline fun <reified T> ObjectFactory.nullableProperty(): ReadWriteProperty<Any, T?> =
  object : ReadWriteProperty<Any, T?> {

    val delegate = property(T::class.java)

    override fun getValue(thisRef: Any, property: KProperty<*>): T? {
      return delegate.orNull
    }

    override fun setValue(thisRef: Any, property: KProperty<*>, value: T?) {
      delegate.set(value)
    }
  }
