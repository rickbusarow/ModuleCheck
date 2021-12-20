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

package modulecheck.api.finding

import modulecheck.api.finding.Finding.FindingResult
import modulecheck.project.ConfiguredDependency
import modulecheck.project.ConfiguredProjectDependency

interface Problem : Finding,
  DependencyFinding {

  val dependencyIdentifier: String

  fun shouldSkip(): Boolean = declarationOrNull?.suppressed
    ?.contains(findingName)
    ?: false

  override fun toResult(fixed: Boolean): FindingResult {
    return FindingResult(
      dependentPath = dependentPath,
      problemName = findingName,
      sourceOrNull = null,
      dependencyPath = dependencyIdentifier,
      positionOrNull = positionOrNull,
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

interface HasSource : Finding {

  val source: ConfiguredProjectDependency
}

interface Fixable : Finding, Problem {

  fun fix(): Boolean = synchronized(buildFile) {

    val declaration = declarationOrNull ?: return false

    require(this is RemovesDependency)

    dependentProject.removeDependencyWithComment(declaration, fixLabel(), oldDependency)

    true
  }

  fun fixLabel() = "  $FIX_LABEL [$findingName]"

  companion object {

    const val FIX_LABEL = "// ModuleCheck finding"
    const val INLINE_COMMENT = "// "
  }
}
