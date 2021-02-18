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
import com.android.build.gradle.AppExtension
import com.android.build.gradle.LibraryExtension
import modulecheck.api.AndroidProject2
import modulecheck.api.Project2
import modulecheck.core.parser.android.AndroidManifestParser
import modulecheck.gradle.internal.srcRoot
import net.swiftzer.semver.SemVer
import org.gradle.api.Project
import org.gradle.kotlin.dsl.findByType
import java.io.File
import java.util.concurrent.*

class AndroidProject2Gradle(
  private val project: Project
) : Project2 by Project2Gradle.from(project),
  AndroidProject2 {

  override val agpVersion: SemVer by lazy { SemVer.parse(Version.ANDROID_GRADLE_PLUGIN_VERSION) }

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
    private val cache = ConcurrentHashMap<Project, AndroidProject2Gradle>()

    fun from(project: Project) = cache.getOrPut(project) {
      AndroidProject2Gradle(project)
    }
  }
}
