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

package modulecheck.gradle.platforms.internal

import org.gradle.api.JavaVersion.VERSION_11
import org.gradle.api.JavaVersion.VERSION_12
import org.gradle.api.JavaVersion.VERSION_13
import org.gradle.api.JavaVersion.VERSION_14
import org.gradle.api.JavaVersion.VERSION_15
import org.gradle.api.JavaVersion.VERSION_16
import org.gradle.api.JavaVersion.VERSION_17
import org.gradle.api.JavaVersion.VERSION_1_10
import org.gradle.api.JavaVersion.VERSION_1_6
import org.gradle.api.JavaVersion.VERSION_1_8
import org.gradle.api.JavaVersion.VERSION_1_9
import org.jetbrains.kotlin.config.JvmTarget
import org.jetbrains.kotlin.config.JvmTarget.JVM_10
import org.jetbrains.kotlin.config.JvmTarget.JVM_11
import org.jetbrains.kotlin.config.JvmTarget.JVM_12
import org.jetbrains.kotlin.config.JvmTarget.JVM_13
import org.jetbrains.kotlin.config.JvmTarget.JVM_14
import org.jetbrains.kotlin.config.JvmTarget.JVM_15
import org.jetbrains.kotlin.config.JvmTarget.JVM_16
import org.jetbrains.kotlin.config.JvmTarget.JVM_17
import org.jetbrains.kotlin.config.JvmTarget.JVM_1_6
import org.jetbrains.kotlin.config.JvmTarget.JVM_1_8
import org.jetbrains.kotlin.config.JvmTarget.JVM_9

typealias GradleJavaVersion = org.gradle.api.JavaVersion

/**
 * @return the [JvmTarget] version for this receiver [JavaVersion][GradleJavaVersion]
 * @since 0.12.0
 */
@Suppress("ComplexMethod")
fun GradleJavaVersion.toJavaVersion(): JvmTarget {
  return when (this) {
    VERSION_1_6 -> JVM_1_6
    VERSION_1_8 -> JVM_1_8
    VERSION_1_9 -> JVM_9
    VERSION_1_10 -> JVM_10
    VERSION_11 -> JVM_11
    VERSION_12 -> JVM_12
    VERSION_13 -> JVM_13
    VERSION_14 -> JVM_14
    VERSION_15 -> JVM_15
    VERSION_16 -> JVM_16
    VERSION_17 -> JVM_17
    // VERSION_18 -> JVM_18
    else -> error("Unsupported Java version: $this")
  }
}
