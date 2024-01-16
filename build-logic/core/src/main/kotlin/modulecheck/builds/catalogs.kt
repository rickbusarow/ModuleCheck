/*
 * Copyright (C) 2021-2024 Rick Busarow
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

package modulecheck.builds

import com.rickbusarow.kgx.extras
import com.rickbusarow.kgx.getOrNullAs
import org.gradle.accessors.dm.LibrariesForLibs
import org.gradle.api.Project

val Project.libs: LibrariesForLibs
  get() = extensions.getByType(LibrariesForLibs::class.java)

val Project.VERSION_NAME_STABLE: String
  get() = libs.versions.rickBusarow.moduleCheck.get()

@Suppress("UnusedReceiverParameter")
val Project.VERSION_NAME: String
  get() = modulecheck.builds.VERSION_NAME

const val GROUP = "com.rickbusarow.modulecheck"
const val PLUGIN_ID = "com.rickbusarow.module-check"
const val VERSION_NAME = "0.13.0-SNAPSHOT"
const val SOURCE_WEBSITE = "https://github.com/rickbusarow/ModuleCheck"
const val DOCS_WEBSITE = "https://rickbusarow.github.io/ModuleCheck"

var Project.artifactId: String?
  get() = extras.getOrNullAs("artifactId")
  set(value) {
    extras.set("artifactId", value)
  }

/** "1.6", "1.7", "1.8", etc. */
val Project.KOTLIN_API: String
  get() = libs.versions.kotlinApi.get()

/**
 * the jdk used in packaging
 *
 * "1.6", "1.8", "11", etc.
 */
val Project.JVM_TARGET: String
  get() = libs.versions.jvmTarget.get()

/**
 * the jdk used to build the project
 *
 * "1.6", "1.8", "11", etc.
 */
val Project.JDK: String
  get() = libs.versions.jdk.get()

/** `6`, `8`, `11`, etc. */
val Project.JVM_TARGET_INT: Int
  get() = JVM_TARGET.substringAfterLast('.').toInt()
