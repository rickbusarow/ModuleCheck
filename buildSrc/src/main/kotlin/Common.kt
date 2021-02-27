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

@file:Suppress("LongMethod", "TopLevelPropertyNaming")

import org.gradle.api.Project
import org.gradle.kotlin.dsl.withType
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

fun Project.common() {

  val ci = !System.getenv("CI").isNullOrBlank()

  tasks.withType<KotlinCompile>()
    .configureEach {

      kotlinOptions {

        allWarningsAsErrors = ci

        jvmTarget = "1.8"
      }
    }

  configurations.all {
    resolutionStrategy {
      force(Libs.Kotlin.reflect)
    }
  }

  val irEnabled = properties["modulecheck.kotlinIR"]?.toString()?.toBoolean() ?: true

  tasks.withType<KotlinCompile>().configureEach {
    kotlinOptions {
      useIR = irEnabled
    }
  }
}
