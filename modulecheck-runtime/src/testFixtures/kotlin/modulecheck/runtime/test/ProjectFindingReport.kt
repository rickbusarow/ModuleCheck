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

package modulecheck.runtime.test

sealed interface ProjectFindingReport {

  val fixed: Boolean
  val configuration: String? get() = null
  val dependency: String? get() = null
  val source: String? get() = null
  val position: String? get() = null

  data class inheritedDependency(
    override val fixed: Boolean,
    override val configuration: String?,
    override val dependency: String?,
    override val source: String?,
    override val position: String?
  ) : ProjectFindingReport

  data class mustBeApi(
    override val fixed: Boolean,
    override val configuration: String?,
    override val dependency: String?,
    override val position: String?
  ) : ProjectFindingReport

  data class overshot(
    override val fixed: Boolean,
    override val configuration: String?,
    override val dependency: String?,
    override val position: String?
  ) : ProjectFindingReport

  data class redundant(
    override val fixed: Boolean,
    override val configuration: String?,
    override val dependency: String?,
    override val source: String?,
    override val position: String?
  ) : ProjectFindingReport

  data class unusedDependency(
    override val fixed: Boolean,
    override val configuration: String?,
    override val dependency: String?,
    override val position: String?
  ) : ProjectFindingReport

  data class depth(override val fixed: Boolean) : ProjectFindingReport
  data class useAnvilFactories(override val fixed: Boolean) : ProjectFindingReport
  data class disableViewBinding(override val fixed: Boolean, override val position: String?) :
    ProjectFindingReport

  data class unsortedDependencies(override val fixed: Boolean) : ProjectFindingReport
  data class unsortedPlugins(override val fixed: Boolean) : ProjectFindingReport
  data class unusedKaptPlugin(
    override val fixed: Boolean,
    override val dependency: String?,
    override val position: String?
  ) :
    ProjectFindingReport

  data class unusedKaptProcessor(
    override val fixed: Boolean,
    override val configuration: String?,
    override val dependency: String?,
    override val position: String?
  ) : ProjectFindingReport

  data class disableAndroidResources(override val fixed: Boolean, override val position: String?) :
    ProjectFindingReport
}
