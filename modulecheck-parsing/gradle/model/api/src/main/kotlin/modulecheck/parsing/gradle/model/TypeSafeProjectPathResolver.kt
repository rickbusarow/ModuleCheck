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

package modulecheck.parsing.gradle.model

import modulecheck.parsing.gradle.model.ProjectPath.StringProjectPath
import modulecheck.parsing.gradle.model.ProjectPath.TypeSafeProjectPath
import javax.inject.Inject

/**
 * A type-safe name can't always be resolved to a String path, because dashes and pascalCase in a
 * String path are treated the same. For instance, these two paths:
 * - `:foo-bar`
 * - `:fooBar`
 *
 * will both have the same accessor name: `projects.fooBar`.
 *
 * Gradle guards against this by checking for name collisions during configuration, so we're
 * guaranteed that only one of the String versions will exist.
 *
 * So, in order to convert a type-safe name into its original String path, we need the list of
 * actual paths to compare against.
 */
class TypeSafeProjectPathResolver @Inject constructor(
  private val allProjectPathsProvider: AllProjectPathsProvider
) {

  private val allPaths: Map<TypeSafeProjectPath, StringProjectPath> by lazy {
    allProjectPathsProvider.getAllPaths()
      .associateBy { it.toTypeSafe() }
  }

  fun resolveStringProjectPath(typeSafe: TypeSafeProjectPath): StringProjectPath {
    return allPaths.getValue(typeSafe)
  }
}

fun interface AllProjectPathsProvider {
  fun getAllPaths(): List<StringProjectPath>
}
