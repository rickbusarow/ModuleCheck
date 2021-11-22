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

package modulecheck.gradle.internal

import com.android.build.gradle.TestedExtension
import modulecheck.project.SourceSetName
import modulecheck.project.toSourceSetName
import org.gradle.api.Project
import org.gradle.kotlin.dsl.findByType
import java.io.File

val Project.srcRoot get() = File("$projectDir/src")
val Project.mainJavaRoot get() = File("$srcRoot/main/java")
val Project.androidTestJavaRoot get() = File("$srcRoot/androidTest/java")
val Project.testJavaRoot get() = File("$srcRoot/test/java")
val Project.mainKotlinRoot get() = File("$srcRoot/main/kotlin")
val Project.androidTestKotlinRoot get() = File("$srcRoot/androidTest/kotlin")
val Project.testKotlinRoot get() = File("$srcRoot/test/kotlin")

fun FileTreeWalk.dirs(): Sequence<File> = asSequence().filter { it.isDirectory }
fun FileTreeWalk.files(): Sequence<File> = asSequence().filter { it.isFile }

fun createFile(
  path: String,
  text: String
) {
  File(path).writeText(text)
}

fun Project.isAndroid(): Boolean = extensions.findByType(TestedExtension::class) != null

fun Project.testedExtensionOrNull(): TestedExtension? = extensions
  .findByType(TestedExtension::class)

fun Project.androidManifests(): Map<SourceSetName, File>? = testedExtensionOrNull()
  ?.sourceSets
  ?.associate { it.name.toSourceSetName() to it.manifest.srcFile }

/**
 * @return the main src `AndroidManifest.xml` file if it exists. This will typically be
 *   `$projectDir/src/main/AndroidManifest.xml`, but if the position has
 *   been changed in the Android extension, the new path will be used.
 */
fun Project.mainAndroidManifest() = testedExtensionOrNull()
  ?.sourceSets
  ?.getByName("main")
  ?.manifest
  ?.srcFile

/**
 * @return true if the project is an Android project and no manifest file exists at the location
 *   defined in the Android extension
 */
fun Project.isMissingManifestFile() = mainAndroidManifest()
  // the file must be declared, but not exist in order for this to be triggered
  ?.let { !it.exists() }
  ?: false
