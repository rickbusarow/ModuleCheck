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

package modulecheck.parsing.gradle.model.impl

import com.squareup.anvil.annotations.ContributesBinding
import modulecheck.dagger.RootGradleProject
import modulecheck.dagger.TaskScope
import modulecheck.model.dependency.AllProjectPathsProvider
import modulecheck.model.dependency.ProjectPath
import modulecheck.parsing.gradle.model.GradleProject
import javax.inject.Inject

@ContributesBinding(TaskScope::class)
class RealAllProjectPathsProvider @Inject constructor(
  @RootGradleProject
  private val rootGradleProject: GradleProject
) : AllProjectPathsProvider {

  private val _allPaths by lazy {
    rootGradleProject.allprojects
      .map { ProjectPath.StringProjectPath(it.path) }
  }

  override fun getAllPaths(): List<ProjectPath.StringProjectPath> {
    return _allPaths
  }
}
