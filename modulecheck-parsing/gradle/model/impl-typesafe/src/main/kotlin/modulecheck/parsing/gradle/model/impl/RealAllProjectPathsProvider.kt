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

package modulecheck.parsing.gradle.model.impl

import com.squareup.anvil.annotations.ContributesBinding
import modulecheck.dagger.AppScope
import modulecheck.dagger.RootGradleProject
import modulecheck.parsing.gradle.model.AllProjectPathsProvider
import modulecheck.parsing.gradle.model.ProjectPath
import org.gradle.api.Project
import javax.inject.Inject

@ContributesBinding(AppScope::class)
class RealAllProjectPathsProvider @Inject constructor(
  @RootGradleProject
  private val rootGradleProject: Project
) : AllProjectPathsProvider {

  private val _allPaths by lazy {
    rootGradleProject.allprojects
      .map { ProjectPath.StringProjectPath(it.path) }
  }

  override fun getAllPaths(): List<ProjectPath.StringProjectPath> {
    return _allPaths
  }
}
