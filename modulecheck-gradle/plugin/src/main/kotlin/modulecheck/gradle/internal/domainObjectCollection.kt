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

package modulecheck.gradle.internal

import modulecheck.gradle.platforms.internal.UnsafeInternalGradleApiReference
import org.gradle.api.Named
import org.gradle.api.NamedDomainObjectCollection
import org.gradle.api.internal.DefaultNamedDomainObjectCollection

/**
 * Executes the specified action when an element is registered in the [NamedDomainObjectCollection].
 * This function does not cause the registered element to be initialized or configured.
 *
 * @param action The action to execute when an element is registered.
 *   It takes the name of the registered element as a parameter.
 * @receiver The receiver [NamedDomainObjectCollection] to observe for element registration.
 * @throws IllegalArgumentException if the receiver collection
 *   does not extend [DefaultNamedDomainObjectCollection].
 */
@UnsafeInternalGradleApiReference
inline fun <reified T : Named> NamedDomainObjectCollection<T>.whenElementRegistered(
  crossinline action: (name: String) -> Unit
) {
  require(this is DefaultNamedDomainObjectCollection<*>) {
    "The receiver collection must extend " +
      "${DefaultNamedDomainObjectCollection::class.qualifiedName}, " +
      "but this type is ${this::class.java.canonicalName}."
  }

  whenElementKnown { action(it.name) }
}
