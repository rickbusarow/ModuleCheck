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

package modulecheck.gradle.platforms

import modulecheck.gradle.platforms.internal.GradleProject
import modulecheck.model.dependency.AllProjectPathsProvider
import modulecheck.model.dependency.ProjectPath

class RealAllProjectPathsProvider(
  private val allPaths: List<ProjectPath.StringProjectPath>
) : AllProjectPathsProvider {

  constructor(gradleProject: GradleProject) : this(
    gradleProject.allprojects
      .map { ProjectPath.StringProjectPath(it.path) }
  )

  override fun getAllPaths(): List<ProjectPath.StringProjectPath> = allPaths

  override fun toString(): String {
    return "RealAllProjectPathsProvider(allPaths=$allPaths)"
  }

  override fun equals(other: Any?): Boolean {
    if (this === other) return true
    if (other !is RealAllProjectPathsProvider) return false

    return allPaths == other.allPaths
  }

  override fun hashCode(): Int {
    return allPaths.hashCode()
  }
}
