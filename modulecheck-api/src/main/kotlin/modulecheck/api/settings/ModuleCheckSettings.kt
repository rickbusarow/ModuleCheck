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

package modulecheck.api.settings

import modulecheck.api.KaptMatcher

interface ModuleCheckSettings {

  var autoCorrect: Boolean

  /**
   * Set of modules which are allowed to be unused.
   *
   * For instance, given:
   * ```
   * ignoreUnusedFinding = setOf(":core")
   * ```
   * If a module declares `:core` as a dependency but does not use it, no finding will be reported.
   */
  var ignoreUnusedFinding: Set<String>

  /**
   * Set of modules which will not be excluded from error reporting.
   * The most common use-case would be if the module is the root of a dependency graph,
   * like an Android application module, and it needs everything in its classpath
   * for dependency injection purposes.
   */
  var doNotCheck: Set<String>

  /**
   * List of [KaptMatcher]'s to be checked, which aren't included by default with ModuleCheck.
   */
  var additionalKaptMatchers: List<KaptMatcher>

  val checks: ChecksSettings
  fun checks(block: ChecksSettings.() -> Unit)

  val sort: SortSettings
  fun sort(block: SortSettings.() -> Unit)
}

interface SortSettings {
  var pluginComparators: List<String>
  var dependencyComparators: List<String>
}

interface ChecksSettings {
  var redundantDependency: Boolean
  var unusedDependency: Boolean
  var mustBeApi: Boolean
  var inheritedDependency: Boolean
  var sortDependencies: Boolean
  var sortPlugins: Boolean
  var unusedKapt: Boolean
  var anvilFactoryGeneration: Boolean
  var disableAndroidResources: Boolean
  var disableViewBinding: Boolean
}
