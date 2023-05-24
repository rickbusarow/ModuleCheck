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

package modulecheck.utils

inline fun <T : Any, E> T.applyEach(elements: Iterable<E>, block: T.(E) -> Unit): T {
  elements.forEach { element -> this.block(element) }
  return this
}

inline fun <T> T.applyIf(predicate: Boolean, body: T.() -> T): T = apply {
  if (predicate) {
    body()
  }
}

inline fun <T> T.letIf(predicate: Boolean, body: (T) -> T): T = let { t ->
  if (predicate) {
    body(t)
  } else {
    t
  }
}
