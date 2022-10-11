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

package modulecheck.builds

import org.gradle.api.Project
import org.gradle.api.model.ObjectFactory
import org.gradle.api.provider.Property
import org.gradle.api.provider.Provider
import kotlin.properties.ReadWriteProperty
import kotlin.reflect.KProperty

inline fun <reified T : Any> ObjectFactory.property(
  initialValue: T,
  crossinline onSet: (T) -> Unit
): ReadWriteProperty<Any, T> =
  object : ReadWriteProperty<Any, T> {

    val delegate = property(T::class.java).convention(initialValue)

    override fun getValue(thisRef: Any, property: KProperty<*>): T {
      // return delegate.get()
      return delegate.getFinal()
    }

    override fun setValue(thisRef: Any, property: KProperty<*>, value: T) {
      // delegate.set(value)
      delegate.setFinal(value)
      onSet(value)
    }
  }

inline fun <reified T> ObjectFactory.optionalProperty(
  crossinline onSet: (T) -> Unit
): ReadWriteProperty<Any, T?> =
  object : ReadWriteProperty<Any, T?> {

    val delegate = property(T::class.java)

    override fun getValue(thisRef: Any, property: KProperty<*>): T? {
      return delegate.orNull
      // return delegate.getFinal()
    }

    override fun setValue(thisRef: Any, property: KProperty<*>, value: T?) {
      // delegate.set(value)
      delegate.setFinal(value)
      if (value != null) {
        onSet(value)
      }
    }
  }

fun <T> Property<T>.getOrNullFinal(): T? {
  finalizeValueOnRead()
  disallowChanges()
  return orNull
}

@PublishedApi
internal fun <T> Property<T>.getFinal(): T {
  finalizeValueOnRead()
  return get()
}

@PublishedApi
internal fun <T> Property<T>.setFinal(value: T) {
  set(value)
  disallowUnsafeRead()
  disallowChanges()
}

fun <T> Project.propertyNamed(name: String): T {
  return requireNotNull(findPropertyNamed(name))
}

fun <T> Project.findPropertyNamed(name: String): T? {
  @Suppress("UNCHECKED_CAST")
  return findProperty(name) as? T
}

fun <T> Project.gradlePropertyAsProvider(name: String): Provider<T?> {
  return provider { findPropertyNamed(name) }
}

inline fun <T, R> Project.gradlePropertyAsProvider(
  name: String,
  crossinline transform: (T?) -> R
): Provider<R?> {
  return provider { transform(findPropertyNamed(name)) }
}
