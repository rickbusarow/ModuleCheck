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

import javax.inject.Provider
import javax.inject.Qualifier
import javax.inject.Scope
import kotlin.reflect.KClass

/**
 * Tied to a single Gradle task. Currently, that's essentially the same as an `AppScope`.
 */
@Suppress("UnnecessaryAbstractClass")
abstract class TaskScope private constructor()

/** path of ":" */
@Qualifier
annotation class RootGradleProject

/**
 * Indicates that the annotated dependency will be a singleton within this scope.
 *
 * @property scope the scope in which this will be a singleton.
 * @since 0.12.0
 */
@Scope
@Retention(AnnotationRetention.RUNTIME)
annotation class SingleIn(
  @Suppress("UNUSED")
  val scope: KClass<*>
)

/**
 * example: "https://github.com/rbusarow/ModuleCheck"
 *
 * taken from the generated BuildProperties class
 *
 * @since 0.12.0
 */
fun interface SourceWebsiteUrlProvider : Provider<String> {
  override fun get(): String
}

/**
 * example: "0.13.0"
 *
 * @since 0.12.0
 */
fun interface ModuleCheckVersionProvider : Provider<String> {
  override fun get(): String
}

/**
 * example: "https://rbusarow.github.io/ModuleCheck"
 *
 * taken from the generated BuildProperties class
 *
 * @since 0.12.0
 */
fun interface DocsWebsiteUrlProvider : Provider<String> {
  override fun get(): String
}
