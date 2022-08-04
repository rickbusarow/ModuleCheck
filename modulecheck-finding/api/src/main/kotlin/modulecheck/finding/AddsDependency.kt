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

import modulecheck.finding.internal.addDependency
import modulecheck.model.dependency.ConfiguredDependency
import modulecheck.parsing.gradle.dsl.asDeclaration

interface AddsDependency : Fixable {

  /**
   * The dependency to be added
   *
   * @since 0.12.0
   */
  val newDependency: ConfiguredDependency

  suspend fun addDependency(): Boolean {

    val (newDeclaration, tokenOrNull) = newDependency
      .asDeclaration(dependentProject)

    dependentProject.addDependency(newDependency, newDeclaration, tokenOrNull)

    return true
  }
}
