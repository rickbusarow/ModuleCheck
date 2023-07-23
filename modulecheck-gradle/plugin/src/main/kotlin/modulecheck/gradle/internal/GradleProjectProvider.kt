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

package modulecheck.gradle.internal

import com.squareup.anvil.annotations.ContributesBinding
import modulecheck.dagger.TaskScope
import modulecheck.model.dependency.AllProjectPathsProvider
import modulecheck.model.dependency.ProjectPath
import modulecheck.model.dependency.ProjectPath.StringProjectPath
import modulecheck.project.McProject
import modulecheck.project.ProjectCache
import modulecheck.project.ProjectProvider
import javax.inject.Inject

@ContributesBinding(TaskScope::class, AllProjectPathsProvider::class)
@ContributesBinding(TaskScope::class, ProjectProvider::class)
class GradleProjectProvider @Inject constructor(
  override val projectCache: ProjectCache
) : ProjectProvider, AllProjectPathsProvider {

  override fun get(path: ProjectPath): McProject = projectCache.getValue(path)

  override fun getAll(): List<McProject> {
    return projectCache.values.toList()
  }

  override fun getAllPaths(): List<StringProjectPath> = getAll().map { it.projectPath }

  override fun clearCaches() {
    projectCache.clearContexts()
  }
}
