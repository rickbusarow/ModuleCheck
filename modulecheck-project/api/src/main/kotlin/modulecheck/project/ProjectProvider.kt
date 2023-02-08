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

import modulecheck.model.dependency.AllProjectPathsProvider
import modulecheck.model.dependency.ProjectPath
import modulecheck.model.dependency.ProjectPath.StringProjectPath
import modulecheck.model.dependency.TypeSafeProjectPathResolver
import java.io.File

interface ProjectProvider : HasProjectCache, AllProjectPathsProvider {

  fun get(path: ProjectPath): McProject

  fun getAll(): List<McProject>

  override fun getAllPaths(): List<StringProjectPath> = getAll().map { it.projectPath }

  fun clearCaches()
}

fun ProjectProvider.toTypeSafeProjectPathResolver(): TypeSafeProjectPathResolver {
  return TypeSafeProjectPathResolver { getAllPaths() }
}

fun interface ProjectRoot {
  fun get(): File
}
