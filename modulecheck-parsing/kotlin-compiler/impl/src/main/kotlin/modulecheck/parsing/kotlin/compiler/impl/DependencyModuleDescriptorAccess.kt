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

package modulecheck.parsing.kotlin.compiler.impl

import kotlinx.coroutines.flow.toList
import modulecheck.dagger.SingleIn
import modulecheck.dagger.TaskScope
import modulecheck.model.dependency.ProjectPath
import modulecheck.model.dependency.upstream
import modulecheck.model.dependency.withUpstream
import modulecheck.model.sourceset.SourceSetName
import modulecheck.parsing.kotlin.compiler.KotlinEnvironment
import modulecheck.project.McProject
import modulecheck.project.ProjectCache
import modulecheck.utils.coroutines.distinct
import modulecheck.utils.coroutines.flatMapListMerge
import modulecheck.utils.coroutines.mapAsync
import org.jetbrains.kotlin.descriptors.impl.ModuleDescriptorImpl
import javax.inject.Inject

/**
 * Provides all descriptors for the dependencies of a given project's source set.
 *
 * These descriptors are globally cached and shared.
 */
@SingleIn(TaskScope::class)
class DependencyModuleDescriptorAccess @Inject constructor(
  private val projectCache: ProjectCache
) {

  /**
   * @return all descriptors for the dependencies of a given project's
   *   source set. These descriptors are globally cached and shared.
   */
  suspend fun dependencyModuleDescriptors(
    projectPath: ProjectPath,
    sourceSetName: SourceSetName
  ): List<ModuleDescriptorImpl> {

    return projectCache.getValue(projectPath)
      .projectDependencies[sourceSetName]
      .flatMapListMerge { dep ->

        val dependencyProject = projectCache.getValue(dep.projectPath)
        val dependencySourceSetName = dep.declaringSourceSetName(dependencyProject.sourceSets)

        dependencySourceSetName.upstreamEnvironments(dependencyProject, includeSelf = true)
      }
      .plus(
        sourceSetName.upstreamEnvironments(
          project = projectCache.getValue(projectPath),
          includeSelf = false
        )
      )
      .mapAsync { dependencyKotlinEnvironment ->
        dependencyKotlinEnvironment.moduleDescriptorDeferred.await()
      }
      .distinct()
      .toList()
  }

  private suspend fun SourceSetName.upstreamEnvironments(
    project: McProject,
    includeSelf: Boolean
  ): List<KotlinEnvironment> {
    val seed = if (includeSelf) {
      withUpstream(project)
    } else {
      upstream(project)
    }

    return seed.mapNotNull { project.sourceSets[it]?.kotlinEnvironmentDeferred?.await() }
  }
}
