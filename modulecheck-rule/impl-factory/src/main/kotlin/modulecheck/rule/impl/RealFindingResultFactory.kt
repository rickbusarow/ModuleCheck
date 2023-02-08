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

package modulecheck.rule.impl

import com.squareup.anvil.annotations.ContributesBinding
import kotlinx.coroutines.flow.toList
import modulecheck.dagger.TaskScope
import modulecheck.finding.AddsDependency
import modulecheck.finding.Deletable
import modulecheck.finding.Finding
import modulecheck.finding.FindingResultFactory
import modulecheck.finding.Fixable
import modulecheck.finding.ModifiesProjectDependency
import modulecheck.finding.ProjectDependencyFinding
import modulecheck.finding.RemovesDependency
import modulecheck.utils.coroutines.mapAsync
import modulecheck.utils.coroutines.onEachAsync
import modulecheck.utils.sortedWith
import javax.inject.Inject

@ContributesBinding(TaskScope::class)
class RealFindingResultFactory @Inject constructor() : FindingResultFactory {

  override suspend fun create(
    findings: List<Finding>,
    autoCorrect: Boolean,
    deleteUnused: Boolean
  ): List<Finding.FindingResult> {

    return findings
      .onEachAsync { finding ->
        // This is a hack to ensure that the position reported
        // reflects the position *before* fixes have been applied
        finding.positionOrNull.await()
      }
      .toList()
      .groupBy { it.dependentPath }
      .toList()
      .mapAsync { (_, findings) ->
        findings
          /*
          Add all dependencies before removing any, because we need to find the source
          dependency in order to determine where to put the new one.  The source may also need
          to be removed, so that needs to happen later.
          For the same reason, if a finding *modifies* a dependency, then it implements both the
          Adds- and Removes- interfaces.  Do all the add-only work, then modify, then remove.

          Remember that Boolean Comparables are sorted so that true values are at the end.
           */
          .sortedWith(
            // only Adds-, without Modifies-
            { !(it is AddsDependency && it !is ModifiesProjectDependency) },
            // ModifiesDependency is second
            { it !is ModifiesProjectDependency },
            // Sort by type, ish.
            { it::class.java.canonicalName },
            // Amongst findings of the same type, sort by path (if it exists)
            { (it as? ProjectDependencyFinding)?.dependency?.projectPath ?: "" }
          )
          .map { finding ->

            val removalStrategy = if (deleteUnused) {
              RemovesDependency.RemovalStrategy.DELETE
            } else {
              RemovesDependency.RemovalStrategy.COMMENT
            }

            val fixed = when {
              !autoCorrect -> false
              deleteUnused && finding is Deletable -> finding.delete()
              finding is Fixable -> finding.fix(removalStrategy)
              else -> false
            }

            finding.toResult(fixed)
          }
      }
      .toList()
      .flatten()
  }
}
