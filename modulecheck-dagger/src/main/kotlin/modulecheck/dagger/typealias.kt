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

package modulecheck.dagger

/**
 * shorthand for `Set<@JvmSuppressWildcards T>`
 *
 * @since 0.12.0
 */
typealias DaggerSet<T> = Set<@JvmSuppressWildcards T>

/**
 * shorthand for `List<@JvmSuppressWildcards T>`
 *
 * @since 0.12.0
 */
typealias DaggerList<T> = List<@JvmSuppressWildcards T>

/** shorthand for `dagger.Lazy<@JvmSuppressWildcards T>` */
typealias DaggerLazy<T> = dagger.Lazy<@JvmSuppressWildcards T>
