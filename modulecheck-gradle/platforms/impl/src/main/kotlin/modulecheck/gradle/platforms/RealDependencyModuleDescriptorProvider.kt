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

package modulecheck.gradle.platforms

import com.squareup.anvil.annotations.ContributesBinding
import kotlinx.coroutines.flow.toList
import modulecheck.dagger.AppScope
import modulecheck.parsing.gradle.model.ProjectPath
import modulecheck.parsing.gradle.model.SourceSetName
import modulecheck.project.ProjectCache
import modulecheck.project.isAndroid
import modulecheck.utils.coroutines.mapAsyncNotNull
import modulecheck.utils.lazy.LazyDeferred
import modulecheck.utils.lazy.lazyDeferred
import org.jetbrains.kotlin.descriptors.impl.ModuleDescriptorImpl
import javax.inject.Inject

@ContributesBinding(AppScope::class)
class RealDependencyModuleDescriptorProvider @Inject constructor(
  private val projectCache: ProjectCache
) : DependencyModuleDescriptorProvider {

  override fun get(
    projectPath: ProjectPath,
    sourceSetName: SourceSetName
  ): LazyDeferred<List<ModuleDescriptorImpl>> {
    return lazyDeferred {
      projectCache.getValue(projectPath)
        .projectDependencies[sourceSetName]
        .mapAsyncNotNull { dep ->

          val dependencyProject = projectCache.getValue(dep.path)
          val dependencySourceSetName = dep.declaringSourceSetName(dependencyProject.isAndroid())

          dependencySourceSetName.withUpstream(dependencyProject)
            .firstNotNullOfOrNull { dependencyProject.sourceSets[it] }
            ?.kotlinEnvironmentDeferred?.await()
            ?.moduleDescriptorDeferred?.await()
        }
        .toList()
    }
  }
}
