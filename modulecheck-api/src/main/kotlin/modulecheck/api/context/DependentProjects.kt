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

package modulecheck.api.context

import modulecheck.project.McProject
import modulecheck.project.ProjectContext

data class DependentProjects(
  internal val delegate: Set<McProject>
) : Set<McProject> by delegate,
  ProjectContext.Element {

  override val key: ProjectContext.Key<DependentProjects>
    get() = Key

  companion object Key : ProjectContext.Key<DependentProjects> {
    override suspend operator fun invoke(project: McProject): DependentProjects {
      val others = project.projectCache
        .values
        .filter { otherProject ->
          project.path in otherProject
            .projectDependencies
            .flatMap { it.value.map { it.project.path } }
        }
        .toSet()

      return DependentProjects(others)
    }
  }
}

suspend fun ProjectContext.dependentProjects(): DependentProjects = get(DependentProjects)
suspend fun ProjectContext.dependendents(): DependentProjects = get(DependentProjects)
