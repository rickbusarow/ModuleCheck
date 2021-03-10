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

import org.gradle.api.internal.HasConvention
import org.jetbrains.kotlin.gradle.plugin.KotlinSourceSet
import java.io.File

typealias GradleSourceSet = org.gradle.api.tasks.SourceSet

internal fun GradleSourceSet.javaFiles(): Set<File> {
  val kotlinSourceSet = (this as? HasConvention)
    ?.convention
    ?.plugins
    ?.get("kotlin") as? KotlinSourceSet

  kotlinSourceSet ?: return allJava.files

  return kotlinSourceSet.kotlin.sourceDirectories.files
}
