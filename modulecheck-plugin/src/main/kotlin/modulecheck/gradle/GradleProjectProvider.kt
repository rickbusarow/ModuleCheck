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

import com.android.Version
import com.android.build.gradle.*
import com.android.build.gradle.api.BaseVariant
import com.android.build.gradle.internal.api.TestedVariant
import com.squareup.anvil.plugin.AnvilExtension
import modulecheck.api.*
import modulecheck.core.parser.android.AndroidManifestParser
import modulecheck.core.rule.KAPT_PLUGIN_ID
import modulecheck.gradle.internal.existingFiles
import modulecheck.gradle.internal.srcRoot
import modulecheck.psi.DslBlockVisitor
import modulecheck.psi.ExternalDependencyDeclarationVisitor
import modulecheck.psi.internal.asKtFile
import net.swiftzer.semver.SemVer
import org.gradle.api.DomainObjectSet
import org.gradle.api.artifacts.ProjectDependency
import org.gradle.api.internal.HasConvention
import org.gradle.api.plugins.JavaPluginConvention
import org.gradle.kotlin.dsl.findByType
import org.gradle.kotlin.dsl.findPlugin
import org.jetbrains.kotlin.gradle.plugin.KotlinSourceSet
import org.jetbrains.kotlin.psi.KtCallExpression
import java.io.File
import java.util.concurrent.ConcurrentHashMap
import kotlin.LazyThreadSafetyMode.NONE

class GradleProjectProvider(
  private val rootGradleProject: GradleProject,
  override val projectCache: ConcurrentHashMap<String, Project2>
) : ProjectProvider {

  private val gradleProjects = rootGradleProject.allprojects.associateBy { it.path }

  private val agpVersion: SemVer by lazy(NONE) { SemVer.parse(Version.ANDROID_GRADLE_PLUGIN_VERSION) }

  override fun get(path: String): Project2 {
    return projectCache.getOrPut(path) {
      createProject(path)
    }
  }

  @Suppress("UnstableApiUsage")
  private fun createProject(path: String): Project2 {
    val gradleProject = gradleProjects.getValue(path)

    val configurations = gradleProject.configurations()

    val projectDependencies = gradleProject.projectDependencies()
    val hasKapt = gradleProject.plugins.hasPlugin(KAPT_PLUGIN_ID)
    val sourceSets = gradleProject.jvmSourceSets()

    val isAndroid = gradleProject.extensions.findByType(TestedExtension::class) != null

    val libraryExtension by lazy(NONE) { gradleProject.extensions.findByType<LibraryExtension>() }
    val testedExtension by lazy(NONE) {
      gradleProject.extensions.findByType<LibraryExtension>()
        ?: gradleProject.extensions.findByType<AppExtension>()
    }

    return if (isAndroid) {
      AndroidProject2Impl(
        path = path,
        projectDir = gradleProject.projectDir,
        buildFile = gradleProject.buildFile,
        configurations = configurations,
        projectDependencies = projectDependencies,
        hasKapt = hasKapt,
        sourceSets = gradleProject.androidSourceSets(),
        projectCache = projectCache,
        anvilGradlePlugin = gradleProject.anvilGradlePluginOrNull(),
        agpVersion = agpVersion,
        androidResourcesEnabled = libraryExtension?.buildFeatures?.androidResources == true,
        viewBindingEnabled = testedExtension?.buildFeatures?.viewBinding == true,
        resourceFiles = gradleProject.androidResourceFiles(),
        androidPackageOrNull = gradleProject.androidPackageOrNull()
      )
    } else Project2Impl(
      path = path,
      projectDir = gradleProject.projectDir,
      buildFile = gradleProject.buildFile,
      configurations = configurations,
      projectDependencies = projectDependencies,
      hasKapt = hasKapt,
      sourceSets = sourceSets,
      projectCache = projectCache,
      anvilGradlePlugin = gradleProject.anvilGradlePluginOrNull()
    )
  }

  private fun GradleProject.configurations() = configurations
    .associate { configuration ->

      val external = configuration
        .dependencies
        .filterNot { it is ProjectDependency }
        .map { dep ->
          val psi = lazy psiLazy@{
            val parsed = DslBlockVisitor("dependencies")
              .parse(buildFile.asKtFile())
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

  private fun GradleProject.allprojects(): List<Project2> = rootProject
    .allprojects
    .map { get(it.path) }

  private fun GradleProject.projectDependencies(): Lazy<Map<ConfigurationName, List<ConfiguredProjectDependency>>> =
    lazy {
      configurations
        .map { config ->
          config.name to config.dependencies.withType(ProjectDependency::class.java)
            .map {
              ConfiguredProjectDependency(
                configurationName = config.name,
                project = get(it.dependencyProject.path)
              )
            }
        }
        .toMap()
    }

  private fun GradleProject.jvmSourceSets(): Map<String, SourceSet> = convention
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

  private fun GradleProject.anvilGradlePluginOrNull(): AnvilGradlePlugin? {
    val version = configurations
      .findByName("kotlinCompilerPluginClasspath")
      ?.dependencies
      ?.find { it.group == "com.squareup.anvil" }
      ?.version
      ?.let { versionString -> SemVer.parse(versionString) }
      ?: return null

    val enabled = extensions
      .findByType<AnvilExtension>()
      ?.generateDaggerFactories == true

    return AnvilGradlePlugin(version, enabled)
  }

  private fun GradleProject.androidPackageOrNull(): String? {
    val manifest = File("$srcRoot/main/AndroidManifest.xml")

    if (!manifest.exists()) return null

    return AndroidManifestParser.parse(manifest)["package"]
  }

  private fun GradleProject.androidResourceFiles(): Set<File> {
    val testedExtension =
      extensions.findByType<LibraryExtension>()
        ?: extensions.findByType<AppExtension>()

    return testedExtension
      ?.sourceSets
      ?.flatMap { sourceSet ->
        sourceSet.res.getSourceFiles().toList()
      }
      .orEmpty()
      .toSet()
  }

  private val BaseExtension.variants: DomainObjectSet<out BaseVariant>?
    get() = when (this) {
      is AppExtension -> applicationVariants
      is LibraryExtension -> libraryVariants
      is TestExtension -> applicationVariants
      else -> null
    }

  private val BaseVariant.testVariants: List<BaseVariant>
    get() = when (this) {
      is TestedVariant -> listOfNotNull(testVariant, unitTestVariant)
      else -> emptyList()
    }

  private fun GradleProject.androidSourceSets(): Map<String, SourceSet> {
    return extensions
      .findByType<BaseExtension>()
      ?.variants
      ?.flatMap { variant ->

        val testSourceSets = variant
          .testVariants
          .flatMap { it.sourceSets }

        val mainSourceSets = variant.sourceSets

        (testSourceSets + mainSourceSets)
          .distinctBy { it.name }
          .map { sourceProvider ->

            val jvmFiles = sourceProvider
              .javaDirectories
              .flatMap { it.listFiles().orEmpty().toList() }
              .toSet()

            // val bootClasspath = project.files(baseExtension!!.bootClasspath)
            // val classPath = variant
            //   .getCompileClasspath(null)
            //   .filter { it.exists() }
            //   .plus(bootClasspath)
            //   .toSet()

            val resourceFiles = sourceProvider
              .resDirectories
              .flatMap { it.listFiles().orEmpty().toList() }
              .toSet()

            val layoutFiles = resourceFiles
              .filter { it.isFile && it.path.contains("""/res/layouts.*/.*.xml""".toRegex()) }
              .toSet()

            SourceSet(
              name = sourceProvider.name,
              classpathFiles = emptySet(),
              outputFiles = setOf(), // TODO
              jvmFiles = jvmFiles,
              resourceFiles = resourceFiles,
              layoutFiles = layoutFiles
            )
          }
      }

      ?.associateBy { it.name }
      .orEmpty()
  }
}
