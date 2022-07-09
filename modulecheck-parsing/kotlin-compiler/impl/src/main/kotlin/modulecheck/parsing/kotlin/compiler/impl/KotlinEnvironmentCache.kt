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

package modulecheck.parsing.kotlin.compiler.impl

import modulecheck.api.context.classpathDependencies
import modulecheck.parsing.gradle.model.SourceSetName
import modulecheck.parsing.kotlin.compiler.KotlinEnvironment
import modulecheck.project.McProject
import modulecheck.project.ProjectContext
import modulecheck.project.isAndroid
import modulecheck.project.project
import modulecheck.utils.cache.SafeCache
import modulecheck.utils.lazy.lazyDeferred

/** cache for [KotlinEnvironment] per project/source set */
data class KotlinEnvironmentCache(
  private val delegate: SafeCache<SourceSetName, KotlinEnvironment>,
  private val project: McProject
) : ProjectContext.Element {

  override val key: ProjectContext.Key<KotlinEnvironmentCache>
    get() = KotlinEnvironmentCache

  /** @return a [KotlinEnvironment] for this [sourceSetName] */
  suspend fun get(sourceSetName: SourceSetName): KotlinEnvironment {

    return delegate.getOrPut(sourceSetName) {

      val sourceSet = project.sourceSets.getValue(sourceSetName)

      val classpath = sourceSet.classpath
      val sourceDirs = sourceSet.jvmFiles
      val kotlinLanguageVersion = sourceSet.kotlinLanguageVersion
      val jvmTarget = sourceSet.jvmTarget

      val descriptors = lazyDeferred {
        project.classpathDependencies()
          .get(sourceSetName)
          .map { transitive ->
            val dependencyProject = transitive.contributed.project(project)
            val dependencySourceSetName = transitive.contributed
              .declaringSourceSetName(dependencyProject.isAndroid())

            dependencyProject.kotlinEnvironmentCache()
              .get(dependencySourceSetName)
              .moduleDescriptor
              .await()
          }
      }

      val descriptors2 = lazyDeferred {

        sourceSetName.withUpstream(project).map { sourceSetOrUpstream ->
          project.projectDependencies[sourceSetOrUpstream].map { dep ->
            dep.project(project)
              .kotlinEnvironmentCache()
              .get(SourceSetName.MAIN).moduleDescriptor.await()
          }
        }

        project.classpathDependencies()
          .get(sourceSetName)
          .map { transitive ->
            val dependencyProject = transitive.contributed.project(project)
            val dependencySourceSetName = transitive.contributed
              .declaringSourceSetName(dependencyProject.isAndroid())

            dependencyProject.kotlinEnvironmentCache()
              .get(dependencySourceSetName)
              .moduleDescriptor
              .await()
          }
      }

      RealKotlinEnvironment(
        projectPath = project.path,
        classpathFiles = classpath,
        sourceDirs = sourceDirs,
        kotlinLanguageVersion = kotlinLanguageVersion,
        jvmTarget = jvmTarget,
        dependencyModuleDescriptors = descriptors2
      )
        .also { it.bindingContext.await() }
    }
  }

  /** @suppress */
  companion object Key : ProjectContext.Key<KotlinEnvironmentCache> {
    override suspend fun invoke(project: McProject): KotlinEnvironmentCache {
      return KotlinEnvironmentCache(
        SafeCache(listOf(project.path, KotlinEnvironmentCache::class)),
        project
      )
    }
  }
}

/** @return this project's [KotlinEnvironmentCache] */
suspend fun ProjectContext.kotlinEnvironmentCache(): KotlinEnvironmentCache =
  get(KotlinEnvironmentCache)
