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

package modulecheck.api

import modulecheck.api.context.*
import net.swiftzer.semver.SemVer
import org.jetbrains.kotlin.resolve.BindingContext
import java.io.File
import java.util.concurrent.*
import kotlin.contracts.contract

interface ProjectsAware {
  val projectCache: ConcurrentHashMap<String, Project2>
}

@Suppress("TooManyFunctions")
interface Project2 : ProjectContextAware, Comparable<Project2>, ProjectsAware {

  val path: String

  val rootProject: Project2
  val allprojects: List<Project2>
  val projectDir: File
  val buildFile: File

  val configurations: Map<String, Config>

  val dependendents: Set<Project2>
  val projectDependencies: Map<ConfigurationName, List<ConfiguredProjectDependency>>

  val hasKapt: Boolean

  val sourceSets: Map<SourceSetName, SourceSet>

  fun kaptDependenciesForConfig(configurationName: ConfigurationName): Set<KaptProcessor>
  fun bindingContextForSourceSetName(sourceSetName: SourceSetName): BindingContext
  fun classpathForSourceSetName(sourceSetName: SourceSetName): Set<File>
  fun jvmFilesForSourceSetName(sourceSetName: SourceSetName): List<JvmFile>
  fun jvmSourcesForSourceSetName(sourceSetName: SourceSetName): Set<File>
  fun resourcesForSourceSetName(sourceSetName: SourceSetName): Set<File>
  fun layoutFilesForSourceSetName(sourceSetName: SourceSetName): Set<XmlFile.LayoutFile>

  fun classpathForSourceSet(sourceSet: SourceSet): Set<File>
  fun jvmFilesForSourceSet(sourceSet: SourceSet): List<JvmFile>
  fun jvmSourcesForSourceSet(sourceSet: SourceSet): Set<File>
  fun resourcesForSourceSet(sourceSet: SourceSet): Set<File>
  fun layoutFilesForSourceSet(sourceSet: SourceSet): Set<XmlFile.LayoutFile>

  fun importsForSourceSetName(sourceSetName: SourceSetName): Set<ImportName>
  fun extraPossibleReferencesForSourceSetName(
    sourceSetName: SourceSetName
  ): Set<PossibleReferenceName>

  fun allPublicClassPathDependencyDeclarations(): Set<ConfiguredProjectDependency>
  fun sourceOf(dependencyProject: ConfiguredProjectDependency, apiOnly: Boolean = false): Project2?
}

data class ParsedKapt<T>(
  val androidTest: Set<T>,
  val main: Set<T>,
  val test: Set<T>
) {
  fun all() = androidTest + main + test
}

fun Project2.isAndroid(): Boolean {
  contract {
    returns(true) implies (this@isAndroid is AndroidProject2)
  }
  return this is AndroidProject2
}

interface AndroidProject2 : Project2 {
  val agpVersion: SemVer
  val androidResourcesEnabled: Boolean
  val viewBindingEnabled: Boolean
  val resourceFiles: Set<File>
  val androidPackageOrNull: String?
  fun androidResourceDeclarationsForSourceSetName(
    sourceSetName: SourceSetName
  ): Set<DeclarationName>
}

val Project2.srcRoot get() = File("$projectDir/src")
val Project2.mainJavaRoot get() = File("$srcRoot/main/java")
val Project2.androidTestJavaRoot get() = File("$srcRoot/androidTest/java")
val Project2.testJavaRoot get() = File("$srcRoot/test/java")
val Project2.mainKotlinRoot get() = File("$srcRoot/main/kotlin")
val Project2.androidTestKotlinRoot get() = File("$srcRoot/androidTest/kotlin")
val Project2.testKotlinRoot get() = File("$srcRoot/test/kotlin")

fun Project2.mainLayoutRootOrNull(): File? {
  val file = File("$srcRoot/main/res/layout")
  return if (file.exists()) file else null
}

fun Project2.androidTestResRootOrNull(): File? {
  val file = File("$srcRoot/androidTest/res")
  return if (file.exists()) file else null
}

fun Project2.mainResRootOrNull(): File? {
  val file = File("$srcRoot/main/res")
  return if (file.exists()) file else null
}

fun Project2.testResRootOrNull(): File? {
  val file = File("$srcRoot/test/res")
  return if (file.exists()) file else null
}
