/*
 * Copyright (C) 2021 Rick Busarow
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

package modulecheck.gradle

import com.android.build.gradle.TestedExtension
import modulecheck.api.*
import modulecheck.api.context.*
import modulecheck.api.context.ProjectContext.Element
import modulecheck.api.context.ProjectContext.Key
import modulecheck.core.rule.KAPT_PLUGIN_ID
import modulecheck.gradle.internal.existingFiles
import modulecheck.psi.DslBlockVisitor
import modulecheck.psi.ExternalDependencyDeclarationVisitor
import modulecheck.psi.internal.asKtFile
import org.gradle.api.Project
import org.gradle.api.artifacts.ProjectDependency
import org.gradle.api.internal.HasConvention
import org.gradle.api.plugins.JavaPluginConvention
import org.gradle.kotlin.dsl.findByType
import org.gradle.kotlin.dsl.findPlugin
import org.jetbrains.kotlin.gradle.plugin.KotlinSourceSet
import org.jetbrains.kotlin.psi.KtCallExpression
import java.io.File
import java.util.concurrent.*

@Suppress("TooManyFunctions")
open class Project2Gradle protected constructor(
  internal val gradleProject: Project,
  override val projectCache: ConcurrentHashMap<String, Project2>
) : Project2 {

  private val context by lazy { ProjectContextImpl(this) }

  override fun <E : Element> get(key: Key<E>): E {
    return context[key]
  }

  override val configurations = gradleProject
    .configurations
    .associate { configuration ->

      val external = configuration
        .dependencies
        .filterNot { it is ProjectDependency }
        .map { dep ->
          val psi = lazy psiLazy@{
            val parsed = DslBlockVisitor("dependencies")
              .parse(gradleProject.buildFile.asKtFile())
              ?: return@psiLazy null

            parsed
              .elements
              .firstOrNull { element ->

                val p = ExternalDependencyDeclarationVisitor(
                  configuration = configuration.name,
                  group = dep.group,
                  name = dep.name,
                  version = dep.version
                )

                p.find(element.psiElement as KtCallExpression)
              }
          }
          ExternalDependency(
            configurationName = configuration.name,
            group = dep.group,
            moduleName = dep.name,
            version = dep.version,
            psiElementWithSurroundingText = psi
          )
        }
        .toSet()

      val config = Config(configuration.name, external)

      configuration.name to config
    }

  override val path: String get() = gradleProject.path
  override val rootProject: Project2 by lazy {
    Project2Gradle.from(
      gradleProject.rootProject,
      projectCache
    )
  }
  override val allprojects: List<Project2> by lazy {
    gradleProject
      .rootProject
      .allprojects
      .map { Project2Gradle.from(it, projectCache) }
  }

  override val sourceSets: Map<SourceSetName, SourceSet> by lazy {
    gradleProject
      .convention
      .findPlugin(JavaPluginConvention::class)
      ?.sourceSets
      ?.map {

        val jvmFiles = (
          (it as? HasConvention)
            ?.convention
            ?.plugins
            ?.get("kotlin") as? KotlinSourceSet
          )
          ?.kotlin
          ?.sourceDirectories
          ?.files
          ?: it.allJava.files

        SourceSet(
          name = it.name,
          classpathFiles = it.compileClasspath.existingFiles().files,
          outputFiles = it.output.classesDirs.existingFiles().files,
          jvmFiles = jvmFiles,
          resourceFiles = it.resources.sourceDirectories.files
        )
      }
      ?.associateBy { it.name }
      .orEmpty()
  }

  override fun allPublicClassPathDependencyDeclarations(): Set<ConfiguredProjectDependency> {
    val sub = projectDependencies["api"]
      .orEmpty()
      .flatMap {
        it
          .project
          .allPublicClassPathDependencyDeclarations()
      }

    return projectDependencies["api"]
      .orEmpty()
      .plus(sub)
      .toSet()
  }

  override val projectDir: File get() = gradleProject.projectDir
  override val buildFile: File get() = gradleProject.buildFile
  override val hasKapt: Boolean by lazy { gradleProject.plugins.hasPlugin(KAPT_PLUGIN_ID) }

  override val projectDependencies: Map<ConfigurationName, List<ConfiguredProjectDependency>> by lazy {
    gradleProject
      .configurations
      .map { config ->
        config.name to config.dependencies.withType(ProjectDependency::class.java)
          .map {
            ConfiguredProjectDependency(
              configurationName = config.name,
              project = Project2Gradle.from(it.dependencyProject, projectCache)
            )
          }
      }
      .toMap()
  }

  fun anvilGradlePlugin(): AnvilGradlePlugin {
    return anvilGradlePlugin
  }

  override fun sourceOf(
    dependencyProject: ConfiguredProjectDependency,
    apiOnly: Boolean
  ): Project2? {
    val toCheck = if (apiOnly) {
      projectDependencies["api"]
        .orEmpty()
    } else {
      projectDependencies
        .main()
    }

    if (dependencyProject in toCheck) return this

    return toCheck.firstOrNull {
      it == dependencyProject || it.project.sourceOf(dependencyProject, true) != null
    }?.project
  }

  override fun compareTo(other: Project2): Int = path.compareTo(other.path)
  override fun toString(): String = "Project2Gradle(path='$path')"

  override fun equals(other: Any?): Boolean {
    if (this === other) return true
    if (other !is Project2Gradle) return false

    if (gradleProject != other.gradleProject) return false

    return true
  }

  override fun hashCode(): Int {
    return gradleProject.hashCode()
  }

  companion object {

    fun from(
      gradleProject: Project,
      projectCache: ConcurrentHashMap<String, Project2>
    ): Project2 =
      projectCache.getOrPut(gradleProject.path) {
        if (gradleProject.extensions.findByType(TestedExtension::class) != null) {
          AndroidProject2Gradle.from(gradleProject, projectCache)
        } else {
          Project2Gradle(gradleProject, projectCache)
        }
      }
  }
}
