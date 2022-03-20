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

package modulecheck.api.finding

import modulecheck.api.finding.Finding.FindingResult
import modulecheck.project.ConfiguredDependency
import modulecheck.project.ConfiguredProjectDependency
import modulecheck.utils.safeAs

interface Problem :
  Finding,
  DependencyFinding {

  suspend fun shouldSkip(): Boolean = declarationOrNull.await()?.suppressed
    ?.contains(findingName)
    ?: false

  override suspend fun toResult(fixed: Boolean): FindingResult {
    return FindingResult(
      dependentPath = dependentPath,
      problemName = findingName,
      sourceOrNull = null,
      configurationName = safeAs<ConfigurationFinding>()?.configurationName?.value ?: "",
      dependencyIdentifier = dependencyIdentifier,
      positionOrNull = positionOrNull.await(),
      buildFile = buildFile,
      message = message,
      fixed = fixed
    )
  }
}

interface RemovesDependency : Fixable {

  val oldDependency: ConfiguredDependency
}

interface AddsDependency : Fixable {

  val newDependency: ConfiguredProjectDependency
}

interface ModifiesDependency : RemovesDependency, AddsDependency

interface HasSource : Finding {

  val source: ConfiguredProjectDependency
}

interface Fixable : Finding, Problem {

  suspend fun fix(): Boolean {

    val declaration = declarationOrNull.await() ?: return false

    require(this is RemovesDependency)

    dependentProject.removeDependencyWithComment(declaration, fixLabel(), oldDependency)

    return true
  }

  fun fixLabel() = "  $FIX_LABEL [$findingName]"

  companion object {

    const val FIX_LABEL = "// ModuleCheck finding"
    const val INLINE_COMMENT = "// "
  }
}
