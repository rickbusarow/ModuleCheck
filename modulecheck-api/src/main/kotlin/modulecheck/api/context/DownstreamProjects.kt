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

package modulecheck.api.context

import modulecheck.model.dependency.DownstreamDependency
import modulecheck.project.McProject
import modulecheck.project.ProjectContext
import modulecheck.project.project
import modulecheck.utils.flatMapToSet

data class DownstreamProjects(
  private val delegate: Set<DownstreamDependency>
) : Set<DownstreamDependency> by delegate,
  ProjectContext.Element {

  override val key: ProjectContext.Key<DownstreamProjects>
    get() = Key

  companion object Key : ProjectContext.Key<DownstreamProjects> {
    override suspend operator fun invoke(project: McProject): DownstreamProjects {
      val others = project.projectCache
        .values
        .flatMapToSet { otherProject ->

          otherProject
            .classpathDependencies()
            .all()
            .filter { it.contributed.project(project.projectCache) == project }
            .map { transitive ->
              DownstreamDependency(
                dependentProjectPath = otherProject.path,
                configuredProjectDependency = transitive.withContributedConfiguration().contributed
              )
            }
        }

      return DownstreamProjects(others)
    }
  }
}

/**
 * All projects which are downstream of the receiver project, including those which only inherit via
 * another dependency's `api` configuration without declaring the dependency directly.
 */
suspend fun ProjectContext.dependents(): DownstreamProjects = get(DownstreamProjects)
