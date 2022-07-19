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

package modulecheck.gradle.platforms.sourcesets

import modulecheck.gradle.platforms.getKotlinExtensionOrNull
import modulecheck.gradle.platforms.internal.toJavaVersion
import modulecheck.parsing.gradle.model.GradleProject
import org.gradle.api.file.FileCollection
import org.gradle.api.plugins.JavaPluginExtension
import org.jetbrains.kotlin.config.JvmTarget
import org.jetbrains.kotlin.config.LanguageVersion

/**
 * @return every file which is an actual file (not directory), and actually exists in this file
 *   system
 */
fun FileCollection.existingFiles() = filter { it.isFile && it.exists() }

/** @return the Java version used to compile this project */
fun GradleProject.jvmTarget(): JvmTarget {
  return extensions.findByType(JavaPluginExtension::class.java)
    ?.sourceCompatibility
    ?.toJavaVersion()
    ?: JvmTarget.JVM_1_8
}

/** @return the Kotlin language version used to compile this project */
fun GradleProject.kotlinLanguageVersionOrNull(): LanguageVersion? {

  return getKotlinExtensionOrNull()?.let { kotlinExtension ->
    LanguageVersion.fromFullVersionString(kotlinExtension.coreLibrariesVersion)
  }
}
