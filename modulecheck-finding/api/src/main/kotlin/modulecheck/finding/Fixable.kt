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

package modulecheck.finding

import modulecheck.finding.Finding.FindingResult
import modulecheck.finding.RemovesDependency.RemovalStrategy
import modulecheck.finding.RemovesDependency.RemovalStrategy.COMMENT
import modulecheck.finding.RemovesDependency.RemovalStrategy.DELETE
import modulecheck.finding.internal.addDependency
import modulecheck.finding.internal.closestDeclarationOrNull
import modulecheck.finding.internal.removeDependencyWithComment
import modulecheck.finding.internal.removeDependencyWithDelete
import modulecheck.finding.internal.statementOrNullIn
import modulecheck.parsing.gradle.ModuleDependencyDeclaration
import modulecheck.parsing.gradle.createProjectDependencyDeclaration
import modulecheck.project.ConfiguredDependency
import modulecheck.project.ConfiguredProjectDependency
import modulecheck.utils.LazyDeferred
import modulecheck.utils.lazyDeferred
import modulecheck.utils.safeAs

interface Problem :
  Finding,
  DependencyFinding {

  val isSuppressed: LazyDeferred<Boolean>
    get() = lazyDeferred {
      declarationOrNull.await()?.suppressed
        ?.contains(findingName.id)
        ?: false
    }

  override suspend fun toResult(fixed: Boolean): FindingResult {
    return FindingResult(
      dependentPath = dependentPath,
      findingName = findingName,
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

  suspend fun removeDependency(removalStrategy: RemovalStrategy): Boolean {

    val oldDeclaration = (oldDependency as? ConfiguredProjectDependency)
      ?.statementOrNullIn(dependentProject)
      ?: declarationOrNull.await()
      ?: return false

    when (removalStrategy) {
      DELETE -> dependentProject.removeDependencyWithDelete(oldDeclaration, oldDependency)
      COMMENT -> dependentProject.removeDependencyWithComment(
        declaration = oldDeclaration,
        fixLabel = fixLabel(),
        configuredDependency = oldDependency
      )
    }
    return true
  }

  enum class RemovalStrategy {
    DELETE,
    COMMENT
  }
}

interface AddsDependency : Fixable {

  val newDependency: ConfiguredProjectDependency

  suspend fun addDependency(): Boolean {
    val token = dependentProject
      .closestDeclarationOrNull(
        newDependency,
        matchPathFirst = false
      ) as? ModuleDependencyDeclaration

    val newDeclaration = token?.copy(
      newConfigName = newDependency.configurationName,
      newModulePath = newDependency.path,
      testFixtures = newDependency.isTestFixture
    )
      ?: dependentProject.createProjectDependencyDeclaration(
        configurationName = newDependency.configurationName,
        projectPath = newDependency.path,
        isTestFixtures = newDependency.isTestFixture
      )

    dependentProject.addDependency(newDependency, newDeclaration, token)

    return true
  }
}

interface ModifiesProjectDependency : RemovesDependency, AddsDependency

interface HasSource : Finding {

  val source: ConfiguredProjectDependency
}

interface Fixable : Finding, Problem {

  suspend fun fix(removalStrategy: RemovalStrategy): Boolean {

    var addSuccessful = true
    var removeSuccessful = true

    if (this is AddsDependency) {
      addSuccessful = addDependency()
    }

    if (this is RemovesDependency) {

      removeSuccessful = removeDependency(removalStrategy)
    }

    return addSuccessful && removeSuccessful
  }

  fun fixLabel() = "  $FIX_LABEL [${findingName.id}]"

  companion object {

    const val FIX_LABEL = "// ModuleCheck finding"
    const val INLINE_COMMENT = "// "
  }
}
