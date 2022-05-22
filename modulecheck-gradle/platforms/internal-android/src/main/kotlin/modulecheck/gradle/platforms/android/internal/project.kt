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

package modulecheck.gradle.platforms.android.internal

import modulecheck.gradle.platforms.android.AgpApiAccess
import modulecheck.gradle.platforms.android.UnsafeDirectAgpApiReference
import modulecheck.parsing.gradle.model.SourceSetName
import modulecheck.parsing.gradle.model.asSourceSetName
import java.io.File
import org.gradle.api.Project as GradleProject

fun FileTreeWalk.files(): Sequence<File> = asSequence().filter { it.isFile }

/**
 * @param agpApiAccess the [AgpApiAccess] to use for safe access
 * @return A map of the [SourceSetName] to manifest [File] if the AGP plugin is applied, or null if
 *   AGP isn't applied
 */
@Suppress("UnstableApiUsage")
@UnsafeDirectAgpApiReference
fun GradleProject.androidManifests(
  agpApiAccess: AgpApiAccess
): Map<SourceSetName, File>? =
  agpApiAccess.ifSafeOrNull(this) {
    requireBaseExtension()
      .sourceSets
      .associate { it.name.asSourceSetName() to it.manifest.srcFile }
  }

/**
 * @param agpApiAccess the [AgpApiAccess] to use for safe access
 * @return the main src `AndroidManifest.xml` file if it exists. This will typically be
 *   `$projectDir/src/main/AndroidManifest.xml`, but if the position has
 *   been changed in the Android extension, the new path will be used.
 */
fun GradleProject.mainAndroidManifest(agpApiAccess: AgpApiAccess): File? {

  return agpApiAccess.ifSafeOrNull(this) {
    @Suppress("UnstableApiUsage")
    requireCommonExtension().sourceSets
      .findByName("main")
      ?.manifest
      ?.let { it as? com.android.build.gradle.internal.api.DefaultAndroidSourceFile }
      ?.srcFile
  }
}

/**
 * @param agpApiAccess the [AgpApiAccess] to use for safe access
 * @return true if the project is an Android project and no manifest file exists at the location
 *   defined in the Android extension
 */
fun GradleProject.isMissingManifestFile(agpApiAccess: AgpApiAccess): Boolean {

  return mainAndroidManifest(agpApiAccess)
    // the file must be declared, but not exist in order for this to be triggered
    ?.let { !it.exists() }
    ?: false
}

/**
 * @param agpApiAccess the [AgpApiAccess] to use for safe access
 * @return true if the project is an Android library, dynamic feature, or test extensions module and
 *   BuildConfig generation has NOT been explicitly disabled.
 */
fun GradleProject.generatesBuildConfig(agpApiAccess: AgpApiAccess): Boolean {

  return agpApiAccess.ifSafeOrNull(this) {

    @Suppress("UnstableApiUsage")
    requireCommonExtension().buildFeatures.buildConfig != false
  }
    ?.orPropertyDefault(
      gradleProject = this,
      key = "android.defaults.buildfeatures.buildconfig",
      defaultValue = true
    )
    ?: false
}

fun Boolean?.orPropertyDefault(
  gradleProject: GradleProject,
  key: String,
  defaultValue: Boolean
): Boolean {
  if (this != null) return this
  return gradleProject.findProperty(key) as? Boolean ?: defaultValue
}
