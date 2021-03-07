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
import modulecheck.api.AndroidProject2
import modulecheck.api.Project2
import modulecheck.api.SourceSet
import modulecheck.api.SourceSetName
import modulecheck.api.context.*
import modulecheck.core.parser.android.AndroidManifestParser
import modulecheck.core.parser.android.AndroidResourceDeclarations
import modulecheck.gradle.internal.srcRoot
import net.swiftzer.semver.SemVer
import org.gradle.api.DomainObjectSet
import org.gradle.api.Project
import org.gradle.kotlin.dsl.findByType
import org.jetbrains.kotlin.utils.addToStdlib.cast
import java.io.File
import java.util.concurrent.*

class AndroidProject2Gradle(
  private val project: Project,
  projectCache: ConcurrentHashMap<String, Project2>
) : Project2Gradle(project, projectCache),
  AndroidProject2 {

  override val agpVersion: SemVer by lazy { SemVer.parse(Version.ANDROID_GRADLE_PLUGIN_VERSION) }

  private val baseExtension by lazy { project.extensions.findByType<BaseExtension>() }
  private val libraryExtension by lazy { project.extensions.findByType<LibraryExtension>() }
  private val testedExtension by lazy {
    project.extensions.findByType<LibraryExtension>()
      ?: project.extensions.findByType<AppExtension>()
  }

  override val androidPackageOrNull: String? by lazy {

    val manifest = File("${project.srcRoot}/main/AndroidManifest.xml")

    if (!manifest.exists()) return@lazy null

    AndroidManifestParser.parse(manifest)["package"]
  }

  @Suppress("UnstableApiUsage")
  override val androidResourcesEnabled: Boolean
    get() = libraryExtension?.buildFeatures?.androidResources == true

  @Suppress("UnstableApiUsage")
  override val viewBindingEnabled: Boolean
    get() = testedExtension?.buildFeatures?.viewBinding == true

  override val resourceFiles: Set<File> by lazy {
    testedExtension?.sourceSets?.flatMap { sourceSet ->
      sourceSet.res.getSourceFiles().toList()
    }.orEmpty().toSet()
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

  override val sourceSets: Map<SourceSetName, SourceSet> by lazy {

    baseExtension
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

  override fun androidResourceDeclarationsForSourceSetName(
    sourceSetName: SourceSetName
  ): Set<DeclarationName> {
    return get(AndroidResourceDeclarations)[sourceSetName].orEmpty()
  }

  override fun toString(): String = "AndroidProject2Gradle(path='$path')"

  override fun equals(other: Any?): Boolean {
    if (this === other) return true
    if (other !is AndroidProject2Gradle) return false

    if (project != other.project) return false

    return true
  }

  override fun hashCode(): Int {
    return project.hashCode()
  }

  companion object {

    fun from(
      gradleProject: Project,
      projectCache: ConcurrentHashMap<String, Project2>
    ): AndroidProject2 =
      projectCache.getOrPut(gradleProject.path) {
        AndroidProject2Gradle(gradleProject, projectCache)
      }.cast()
  }
}
