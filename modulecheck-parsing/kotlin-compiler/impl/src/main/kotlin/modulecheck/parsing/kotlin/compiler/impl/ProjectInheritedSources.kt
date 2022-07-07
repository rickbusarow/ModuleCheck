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

import kotlinx.coroutines.flow.filterIsInstance
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.toSet
import modulecheck.api.context.jvmFilesForSourceSetName
import modulecheck.parsing.gradle.model.SourceSetName
import modulecheck.parsing.kotlin.compiler.KotlinEnvironment.InheritedSources
import modulecheck.parsing.source.KotlinFile
import modulecheck.project.McProject
import modulecheck.project.ProjectContext
import modulecheck.project.isAndroid
import modulecheck.project.project
import modulecheck.utils.cache.SafeCache
import org.jetbrains.kotlin.psi.KtFile
import java.io.File

/** Cache for generated [InheritedSources] */
class ProjectInheritedSources private constructor(
  private val delegate: SafeCache<SourceSetName, InheritedSources>,
  private val project: McProject
) : ProjectContext.Element {

  override val key: ProjectContext.Key<ProjectInheritedSources>
    get() = ProjectInheritedSources

  /** @return [InheritedSources] for the given [sourceSetName] */
  suspend fun get(sourceSetName: SourceSetName): InheritedSources {

    return delegate.getOrPut(sourceSetName) {

      val ktFiles = mutableSetOf<KtFile>()
      val javaFiles = mutableSetOf<File>()
      val classpathFiles = mutableSetOf<File>()

      val projectUpstreamSourceSets = sourceSetName.withUpstream(project)
        .filterNot { it == sourceSetName }

      projectUpstreamSourceSets
        .flatMap {
          listOfNotNull(
            project.projectInheritedSources().get(it),
            project.sources(it)
          )
        }
        .forEach { pds ->

          ktFiles.addAll(pds.ktFiles)
          javaFiles.addAll(pds.jvmFiles)
          classpathFiles.addAll(pds.classpathFiles)
        }

      sourceSetName.withUpstream(project)
        .flatMap { sourceSetOrUpstream ->
          project.projectDependencies[sourceSetOrUpstream]
        }.flatMap { dep ->

          val depProject = dep.project(project)
          val depSourceSet = dep.declaringSourceSetName(depProject.isAndroid())

          listOfNotNull(
            depProject.projectInheritedSources().get(depSourceSet),
            depProject.sources(depSourceSet)
          )
        }
        .forEach { pds ->

          ktFiles.addAll(pds.ktFiles)
          javaFiles.addAll(pds.jvmFiles)
          classpathFiles.addAll(pds.classpathFiles)
        }

      InheritedSources(
        ktFiles = ktFiles,
        jvmFiles = javaFiles,
        classpathFiles = classpathFiles
      )
    }
  }

  private suspend fun McProject.sources(
    sourceSetName: SourceSetName
  ): InheritedSources? {

    val allJvmFiles = jvmFilesForSourceSetName(sourceSetName)

    val ktFiles = allJvmFiles.filterIsInstance<KotlinFile>()
      .map { it.psi }
      .toSet()

    val sourceSet = sourceSets[sourceSetName] ?: return null

    return InheritedSources(
      ktFiles = ktFiles,
      jvmFiles = allJvmFiles.map { it.file }.toSet(),
      classpathFiles = sourceSet.classpath.value
    )
  }

  /** @suppress */
  companion object Key : ProjectContext.Key<ProjectInheritedSources> {
    override suspend fun invoke(project: McProject): ProjectInheritedSources {
      return ProjectInheritedSources(
        SafeCache(listOf(project.path, ProjectInheritedSources::class)), project
      )
    }
  }
}

/** @return this project's [ProjectInheritedSources] */
suspend fun ProjectContext.projectInheritedSources(): ProjectInheritedSources =
  get(ProjectInheritedSources)
