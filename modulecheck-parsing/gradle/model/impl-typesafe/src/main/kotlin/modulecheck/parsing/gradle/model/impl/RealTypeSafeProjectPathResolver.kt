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
import modulecheck.parsing.gradle.model.AllProjectPathsProvider
import modulecheck.parsing.gradle.model.ProjectPath.StringProjectPath
import modulecheck.parsing.gradle.model.ProjectPath.TypeSafeProjectPath
import modulecheck.parsing.gradle.model.TypeSafeProjectPathResolver
import javax.inject.Inject

@ContributesBinding(AppScope::class)
class RealTypeSafeProjectPathResolver @Inject constructor(
  private val allProjectPathsProvider: AllProjectPathsProvider
) : TypeSafeProjectPathResolver {

  private val allPaths: Map<TypeSafeProjectPath, StringProjectPath> by lazy {
    allProjectPathsProvider.get()
      .associateBy { it.toTypeSafe() }
  }

  override fun resolveStringProjectPath(typeSafe: TypeSafeProjectPath): StringProjectPath {
    return allPaths.getValue(typeSafe)
  }
}
