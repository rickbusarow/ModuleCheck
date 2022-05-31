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

import modulecheck.finding.RemovesDependency.RemovalStrategy.COMMENT
import modulecheck.finding.RemovesDependency.RemovalStrategy.DELETE
import modulecheck.finding.internal.removeDependencyWithComment
import modulecheck.finding.internal.removeDependencyWithDelete
import modulecheck.finding.internal.statementOrNullIn
import modulecheck.model.dependency.ConfiguredDependency
import modulecheck.model.dependency.ProjectDependency

interface RemovesDependency : Fixable {

  val oldDependency: ConfiguredDependency

  suspend fun removeDependency(removalStrategy: RemovalStrategy): Boolean {

    val oldDeclaration = (oldDependency as? ProjectDependency)
      ?.statementOrNullIn(dependentProject)
      ?: statementOrNull.await()
      ?: return false

    when (removalStrategy) {
      DELETE -> dependentProject.removeDependencyWithDelete(oldDeclaration, oldDependency)
      COMMENT -> dependentProject.removeDependencyWithComment(
        statement = oldDeclaration,
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
