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

@file:JvmMultifileClass

package modulecheck.gradle.platforms.internal

import com.android.build.api.dsl.CommonExtension
import com.android.build.gradle.TestedExtension
import modulecheck.parsing.gradle.model.SourceSetName
import modulecheck.parsing.gradle.model.asSourceSetName
import java.io.File
import org.gradle.api.Project as GradleProject

fun FileTreeWalk.files(): Sequence<File> = asSequence().filter { it.isFile }

fun GradleProject.testedExtensionOrNull(): TestedExtension? = extensions
  .findByType(TestedExtension::class.java)

fun GradleProject.androidManifests(): Map<SourceSetName, File>? = testedExtensionOrNull()
  ?.sourceSets
  ?.associate { it.name.asSourceSetName() to it.manifest.srcFile }

/**
 * @return the main src `AndroidManifest.xml` file if it exists. This will typically be
 *   `$projectDir/src/main/AndroidManifest.xml`, but if the position has
 *   been changed in the Android extension, the new path will be used.
 */
fun GradleProject.mainAndroidManifest() = testedExtensionOrNull()
  ?.sourceSets
  ?.getByName("main")
  ?.manifest
  ?.srcFile

/**
 * @return true if the project is an Android project and no manifest file exists at the location
 *   defined in the Android extension
 */
fun GradleProject.isMissingManifestFile(): Boolean {

  return mainAndroidManifest()
    // the file must be declared, but not exist in order for this to be triggered
    ?.let { !it.exists() }
    ?: false
}

/**
 * @return true if the project is an Android library, dynamic feature, or test extensions module and
 *   BuildConfig generation has NOT been explicitly disabled.
 */
fun GradleProject.generatesBuildConfig(): Boolean {
  @Suppress("UnstableApiUsage")
  return extensions.findByType(CommonExtension::class.java)
    ?.let { it.buildFeatures.buildConfig != false }
    .orPropertyDefault(
      gradleProject = this,
      key = "android.defaults.buildfeatures.buildconfig",
      defaultValue = true
    )
}

fun Boolean?.orPropertyDefault(
  gradleProject: GradleProject,
  key: String,
  defaultValue: Boolean
): Boolean {
  if (this != null) return this
  return gradleProject.findProperty(key) as? Boolean ?: defaultValue
}
