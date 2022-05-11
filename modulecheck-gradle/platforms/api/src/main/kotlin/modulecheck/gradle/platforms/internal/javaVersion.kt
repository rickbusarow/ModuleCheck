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

import modulecheck.parsing.source.JavaVersion
import org.gradle.api.JavaVersion.VERSION_11
import org.gradle.api.JavaVersion.VERSION_12
import org.gradle.api.JavaVersion.VERSION_13
import org.gradle.api.JavaVersion.VERSION_14
import org.gradle.api.JavaVersion.VERSION_15
import org.gradle.api.JavaVersion.VERSION_16
import org.gradle.api.JavaVersion.VERSION_17
import org.gradle.api.JavaVersion.VERSION_18
import org.gradle.api.JavaVersion.VERSION_19
import org.gradle.api.JavaVersion.VERSION_1_1
import org.gradle.api.JavaVersion.VERSION_1_10
import org.gradle.api.JavaVersion.VERSION_1_2
import org.gradle.api.JavaVersion.VERSION_1_3
import org.gradle.api.JavaVersion.VERSION_1_4
import org.gradle.api.JavaVersion.VERSION_1_5
import org.gradle.api.JavaVersion.VERSION_1_6
import org.gradle.api.JavaVersion.VERSION_1_7
import org.gradle.api.JavaVersion.VERSION_1_8
import org.gradle.api.JavaVersion.VERSION_1_9
import org.gradle.api.JavaVersion.VERSION_20
import org.gradle.api.JavaVersion.VERSION_HIGHER

typealias GradleJavaVersion = org.gradle.api.JavaVersion

@Suppress("UnstableApiUsage")
fun GradleJavaVersion.toJavaVersion(): JavaVersion {
  return when (this) {
    VERSION_1_1 -> JavaVersion.VERSION_1_1
    VERSION_1_2 -> JavaVersion.VERSION_1_2
    VERSION_1_3 -> JavaVersion.VERSION_1_3
    VERSION_1_4 -> JavaVersion.VERSION_1_4
    VERSION_1_5 -> JavaVersion.VERSION_1_5
    VERSION_1_6 -> JavaVersion.VERSION_1_6
    VERSION_1_7 -> JavaVersion.VERSION_1_7
    VERSION_1_8 -> JavaVersion.VERSION_1_8
    VERSION_1_9 -> JavaVersion.VERSION_1_9
    VERSION_1_10 -> JavaVersion.VERSION_1_10
    VERSION_11 -> JavaVersion.VERSION_11
    VERSION_12 -> JavaVersion.VERSION_12
    VERSION_13 -> JavaVersion.VERSION_13
    VERSION_14 -> JavaVersion.VERSION_14
    VERSION_15 -> JavaVersion.VERSION_15
    VERSION_16 -> JavaVersion.VERSION_16
    VERSION_17 -> JavaVersion.VERSION_17
    VERSION_18 -> JavaVersion.VERSION_18
    VERSION_19 -> JavaVersion.VERSION_19
    VERSION_20 -> JavaVersion.VERSION_20
    VERSION_HIGHER -> JavaVersion.VERSION_HIGHER
  }
}
