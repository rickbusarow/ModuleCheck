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
import modulecheck.core.internal.jvmFiles
import modulecheck.core.kapt.KAPT_PLUGIN_ID
import modulecheck.core.kapt.KaptParser
import modulecheck.gradle.internal.existingFiles
import modulecheck.gradle.internal.ktFiles
import modulecheck.psi.*
import modulecheck.psi.ExternalDependencyDeclarationVisitor
import modulecheck.psi.internal.*
import net.swiftzer.semver.SemVer
import org.gradle.api.Project
import org.gradle.api.artifacts.ProjectDependency
import org.gradle.api.internal.HasConvention
import org.gradle.api.plugins.JavaPluginConvention
import org.gradle.api.tasks.SourceSetContainer
import org.gradle.kotlin.dsl.findByType
import org.jetbrains.kotlin.cli.common.messages.*
import org.jetbrains.kotlin.gradle.plugin.KotlinSourceSet
import org.jetbrains.kotlin.psi.KtCallExpression
import org.jetbrains.kotlin.resolve.BindingContext
import java.io.File
import java.util.concurrent.*

fun Project.project2() =
  if (extensions.findByType(TestedExtension::class) != null) {
    AndroidProject2Gradle.from(this)
  } else {
    Project2Gradle.from(this)
  }

class Project2Gradle private constructor(
  private val project: Project
) : Project2 {
  override val path: String get() = project.path
  override val rootProject: Project2 by lazy { Project2Gradle(project.rootProject) }
  override val allprojects: List<Project2> by lazy {
    project
      .rootProject
      .allprojects
      .map { Project2Gradle(it) }
  }

  private val sourceSets: SourceSetContainer? = project
    .convention
    .findPlugin(JavaPluginConvention::class.java)
    ?.sourceSets

  private val bindingContexts = ConcurrentHashMap<SourceSet, BindingContext>()
  private val classpaths = ConcurrentHashMap<SourceSet, Set<File>>()
  private val jvmSources = ConcurrentHashMap<SourceSet, Set<File>>()
  private val resSources = ConcurrentHashMap<SourceSet, Set<File>>()

  override fun bindingContextForSourceSet(sourceSet: SourceSet): BindingContext {
    return bindingContexts.getOrPut(sourceSet) {
      val classPath = classpathForSourceSet(sourceSet).map { it.path }
      val jvmSources = jvmSourcesForSourceSet(sourceSet).ktFiles()

      createBindingContext(classPath, jvmSources)
    }
  }

  override fun classpathForSourceSet(sourceSet: SourceSet): Set<File> {
    return classpaths.getOrPut(sourceSet) {
      sourceSets
        ?.findByName(sourceSet.name)
        ?.let { set ->

          set.compileClasspath.existingFiles().files + set.output.classesDirs.existingFiles().files
        } ?: emptySet()
    }
  }

  override fun jvmSourcesForSourceSet(sourceSet: SourceSet): Set<File> {
    return jvmSources.getOrPut(sourceSet) {
      sourceSets
        ?.findByName(sourceSet.name)
        ?.let { set ->

          val kotlinSourceSet = (set as? HasConvention)
            ?.convention
            ?.plugins
            ?.get("kotlin") as? KotlinSourceSet

          kotlinSourceSet ?: return@let set.allJava.files

          kotlinSourceSet.kotlin.sourceDirectories.files
        } ?: emptySet()
    }
  }

  override fun jvmFilesForSourceSet(sourceSet: SourceSet): List<JvmFile> {
    return jvmSources.getOrPut(sourceSet) {
      sourceSets
        ?.findByName(sourceSet.name)
        ?.let { set ->

          val kotlinSourceSet = (set as? HasConvention)
            ?.convention
            ?.plugins
            ?.get("kotlin") as? KotlinSourceSet

          kotlinSourceSet ?: return@let set.allJava.files

          kotlinSourceSet.kotlin.sourceDirectories.files
        } ?: emptySet()
    }.jvmFiles(bindingContextForSourceSet(sourceSet))
  }

  override fun resourcesForSourceSet(sourceSet: SourceSet): Set<File> {
    return resSources.getOrPut(sourceSet) {
      sourceSets
        ?.findByName(sourceSet.name)
        ?.resources
        ?.sourceDirectories
        ?.files
        ?: emptySet()
    }
  }

  override val projectDir: File get() = project.projectDir
  override val buildFile: File get() = project.buildFile
  override val hasKapt: Boolean by lazy { project.plugins.hasPlugin(KAPT_PLUGIN_ID) }

  override val kaptProcessors: ParsedKapt<KaptProcessor> by KaptParser.parseLazy(this)

  override val projectDependencies: List<ConfiguredProjectDependency> by lazy {
    project
      .configurations
      .flatMap { config ->
        config.dependencies.withType(ProjectDependency::class.java)
          .map {
            ConfiguredProjectDependency(
              config = Config.from(config.name),
              project = it.dependencyProject.project2()
            )
          }
      }.toList()
  }

  override val externalDependencies: List<ExternalDependency> by lazy {
    project
      .configurations
      .flatMap { config ->
        config.dependencies
          .filterNot { it is ProjectDependency }
          .map {

            val psi = lazy psiLazy@{
              val parsed = DslBlockVisitor("dependencies")
                .parse(project.buildFile.asKtFile())
                ?: return@psiLazy null

              parsed
                .elements
                .firstOrNull { element ->

                  val p = ExternalDependencyDeclarationVisitor(
                    configuration = Config.from(config.name),
                    group = it.group,
                    name = it.name,
                    version = it.version
                  )

                  p.find(element.psiElement as KtCallExpression)
                }
            }
            ExternalDependency(
              config = Config.from(config.name),
              group = it.group,
              moduleName = it.name,
              version = it.version,
              psiElementWithSurroundingText = psi
            )
          }
      }
  }

  override fun compilerPluginVersionForGroupName(groupName: String): SemVer? {
    return project
      .configurations
      .findByName("kotlinCompilerPluginClasspath")
      ?.dependencies
      ?.find { it.group == groupName }
      ?.version
      ?.let { versionString -> SemVer.parse(versionString) }
  }

  override fun toString(): String = "Project2Gradle(path='$path')"

  override fun equals(other: Any?): Boolean {
    if (this === other) return true
    if (other !is Project2Gradle) return false

    if (project != other.project) return false

    return true
  }

  override fun hashCode(): Int {
    return project.hashCode()
  }

  companion object {
    private val cache = ConcurrentHashMap<Project, Project2Gradle>()

    fun from(project: Project): Project2Gradle = cache.getOrPut(project) { Project2Gradle(project) }
  }
}
