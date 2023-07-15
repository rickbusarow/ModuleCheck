/*
 * Copyright (C) 2021-2023 Rick Busarow
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

package modulecheck.gradle.platforms.kotlin

import modulecheck.gradle.platforms.internal.GradleProject
import modulecheck.gradle.platforms.internal.toJavaVersion
import org.gradle.api.file.FileCollection
import org.gradle.api.plugins.JavaPluginExtension
import org.jetbrains.kotlin.config.JvmTarget
import org.jetbrains.kotlin.config.LanguageVersion
import org.jetbrains.kotlin.gradle.dsl.KotlinProjectExtension

/** Safely returns KGP's `kotlin` extension if it exists */
fun GradleProject.getKotlinExtensionOrNull(): KotlinProjectExtension? =
  extensions.findByName("kotlin") as? KotlinProjectExtension

/**
 * @return every file which is an actual file (not
 *   directory), and actually exists in this file system
 * @since 0.12.0
 */
fun FileCollection.existingFiles(): FileCollection = filter { it.isFile && it.exists() }

/** shorthand for `extensions.findByType(JavaPluginExtension::class.java)` */
fun GradleProject.getJavaPluginExtensionOrNull(): JavaPluginExtension? =
  extensions.findByType(org.gradle.api.plugins.JavaPluginExtension::class.java)

/**
 * @return the Java version used to compile this project
 * @since 0.12.0
 */
fun GradleProject.jvmTarget(): JvmTarget {
  return extensions.findByType(JavaPluginExtension::class.java)
    ?.sourceCompatibility
    ?.toJavaVersion()
    ?: JvmTarget.JVM_1_8
}

/**
 * @return the Kotlin language version used to compile this project
 * @since 0.12.0
 */
fun GradleProject.kotlinLanguageVersionOrNull(): LanguageVersion? {

  return getKotlinExtensionOrNull()?.let { kotlinExtension ->
    LanguageVersion.fromFullVersionString(kotlinExtension.coreLibrariesVersion)
  }
}
