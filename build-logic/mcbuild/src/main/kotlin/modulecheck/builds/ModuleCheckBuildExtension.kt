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

package modulecheck.builds

import org.gradle.api.model.ObjectFactory
import javax.inject.Inject
import kotlin.properties.ReadWriteProperty
import kotlin.reflect.KProperty

@Suppress("MemberVisibilityCanBePrivate", "UnnecessaryAbstractClass")
abstract class ModuleCheckBuildExtension @Inject constructor(
  objects: ObjectFactory,
  private val artifactIdListener: ArtifactIdListener,
  private val diListener: DIListener
) {

  var artifactId: String? by objects.nullableProperty<String> {
    if (it != null) artifactIdListener.onChanged(it)
  }
  var anvil: Boolean by objects.property(false) {
    diListener.onChanged(it, dagger)
  }
  var dagger: Boolean by objects.property(false) {
    diListener.onChanged(anvil, it)
  }
}

internal inline fun <reified T : Any> ObjectFactory.property(
  initialValue: T,
  crossinline onSet: (T) -> Unit
): ReadWriteProperty<Any, T> =
  object : ReadWriteProperty<Any, T> {

    val delegate = property(T::class.java).convention(initialValue)

    override fun getValue(thisRef: Any, property: KProperty<*>): T {
      return delegate.get()
    }

    override fun setValue(thisRef: Any, property: KProperty<*>, value: T) {
      delegate.set(value)
      onSet(value)
    }
  }

internal inline fun <reified T> ObjectFactory.nullableProperty(
  crossinline onSet: (T?) -> Unit
): ReadWriteProperty<Any, T?> =
  object : ReadWriteProperty<Any, T?> {

    val delegate = property(T::class.java)

    override fun getValue(thisRef: Any, property: KProperty<*>): T? {
      return delegate.orNull
    }

    override fun setValue(thisRef: Any, property: KProperty<*>, value: T?) {
      delegate.set(value)
      onSet(value)
    }
  }
