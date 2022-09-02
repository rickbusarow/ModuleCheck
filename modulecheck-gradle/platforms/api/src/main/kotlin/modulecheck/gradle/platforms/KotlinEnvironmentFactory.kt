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

package modulecheck.gradle.platforms

import modulecheck.model.dependency.ProjectPath.StringProjectPath
import modulecheck.model.sourceset.SourceSetName
import modulecheck.parsing.kotlin.compiler.KotlinEnvironment
import org.jetbrains.kotlin.config.JvmTarget
import org.jetbrains.kotlin.config.LanguageVersion
import java.io.File

/**
 * Kotlin environment factory
 *
 * @since 0.12.0
 */
fun interface KotlinEnvironmentFactory {
  /**
   * @return a kotlin environment for these many arguments
   * @since 0.12.0
   */
  @Suppress("LongParameterList")
  fun create(
    projectPath: StringProjectPath,
    sourceSetName: SourceSetName,
    classpathFiles: Lazy<Collection<File>>,
    sourceDirs: Collection<File>,
    kotlinLanguageVersion: LanguageVersion?,
    jvmTarget: JvmTarget
  ): KotlinEnvironment
}
