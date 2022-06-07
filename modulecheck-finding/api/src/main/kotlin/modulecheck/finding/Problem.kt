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
import modulecheck.utils.lazy.LazyDeferred
import modulecheck.utils.lazy.lazyDeferred
import modulecheck.utils.safeAs

interface Problem :
  Finding,
  DependencyFinding {

  /**
   * Whether this Problem should be ignored. True if the associated statement is annotated with
   * `@Suppress` and the corresponding finding ID.
   */
  val isSuppressed: LazyDeferred<Boolean>
    get() = lazyDeferred {
      statementOrNull.await()
        ?.suppressed
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
