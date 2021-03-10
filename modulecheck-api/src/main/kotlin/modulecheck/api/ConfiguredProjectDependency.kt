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

import modulecheck.psi.PsiElementWithSurroundingText

interface HasConfig {
  val configurationName: ConfigurationName
  val name: String
}

data class ExternalDependency(
  override val configurationName: ConfigurationName,
  val group: String?,
  val moduleName: String?,
  val version: String?,
  val psiElementWithSurroundingText: Lazy<PsiElementWithSurroundingText?>
) : HasConfig {
  override val name = "${group ?: ""}:${moduleName ?: ""}"
  val nameWithVersion = "${group ?: ""}:${moduleName ?: ""}:${version ?: ""}"
}

data class ConfiguredProjectDependency(
  override val configurationName: ConfigurationName,
  val project: Project2
) : HasConfig {

  override val name = project.path

  override fun equals(other: Any?): Boolean {
    if (this === other) return true
    if (other !is ConfiguredProjectDependency) return false

    if (configurationName != other.configurationName) return false
    if (project.path != other.project.path) return false

    return true
  }

  override fun hashCode(): Int {
    var result = configurationName.hashCode()
    result = 31 * result + project.hashCode()
    return result
  }
}
