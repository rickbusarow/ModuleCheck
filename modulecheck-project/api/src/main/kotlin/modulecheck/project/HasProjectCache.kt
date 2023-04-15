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

package modulecheck.project

import modulecheck.model.dependency.DownstreamDependency
import modulecheck.model.dependency.HasProjectPath

interface HasProjectCache {
  val projectCache: ProjectCache

  fun HasProjectPath.project(): McProject = projectCache.getValue(projectPath)
}

/**
 * @param projectCache the project cache which contains the desired project
 * @receiver has a defined path to be resolved to a project
 * @return the project associated with the path in the receiver
 * @since 0.12.0
 */
fun HasProjectPath.project(projectCache: ProjectCache): McProject =
  projectCache.getValue(projectPath)

/**
 * @param hasProjectCache has the project cache which contains the desired project
 * @receiver has a defined path to be resolved to a project
 * @return the project associated with the path in the receiver
 * @since 0.12.0
 */
fun HasProjectPath.project(hasProjectCache: HasProjectCache): McProject =
  hasProjectCache.projectCache
    .getValue(projectPath)

/**
 * @param projectCache the project cache which contains the desired project
 * @receiver has a dependentPath to be resolved to a project
 * @return the project associated with the path in the receiver
 * @since 0.12.0
 */
fun DownstreamDependency.project(
  projectCache: ProjectCache
): McProject = projectCache.getValue(dependentProjectPath)

/**
 * @param hasProjectCache has the project cache which contains the desired project
 * @receiver has a dependentPath to be resolved to a project
 * @return the project associated with the path in the receiver
 * @since 0.12.0
 */
fun DownstreamDependency.project(
  hasProjectCache: HasProjectCache
): McProject = hasProjectCache.projectCache.getValue(dependentProjectPath)
