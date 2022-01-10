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

fun <T, R> List<T>.mapBlocking(transform: suspend (T) -> R): List<R> {
  return map { runBlocking { transform(it) } }
}

fun <T> Sequence<T>.filterBlocking(predicate: suspend (T) -> Boolean): Sequence<T> {
  return filter { runBlocking { predicate(it) } }
}

fun <T> Sequence<T>.filterNotBlocking(predicate: suspend (T) -> Boolean): Sequence<T> {
  return filterNot { runBlocking { predicate(it) } }
}

fun <T, R> Sequence<T>.mapBlocking(transform: suspend (T) -> R): Sequence<R> {
  return map { runBlocking { transform(it) } }
}

fun <T, R> Sequence<T>.flatMapBlocking(transform: suspend (T) -> Iterable<R>): Sequence<R> {
  return flatMap { runBlocking { transform(it) } }
}
