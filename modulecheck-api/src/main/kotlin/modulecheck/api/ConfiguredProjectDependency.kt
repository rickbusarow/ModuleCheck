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

package modulecheck.api

interface HasConfig {
  val config: Config
  val name: String
}

data class ExternalDependency(
  override val config: Config,
  override val name: String
) : HasConfig

data class ConfiguredProjectDependency(
  override val config: Config,
  val project: Project2
) : HasConfig {

  override val name = project.path

  override fun equals(other: Any?): Boolean {
    if (this === other) return true
    if (other !is ConfiguredProjectDependency) return false

    if (config != other.config) return false
    if (project.path != other.project.path) return false

    return true
  }

  override fun hashCode(): Int {
    var result = config.hashCode()
    result = 31 * result + project.hashCode()
    return result
  }
}
