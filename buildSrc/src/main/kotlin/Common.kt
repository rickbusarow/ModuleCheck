/*
 * Copyright (C) 2020 Rick Busarow
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

import org.gradle.api.*
import org.gradle.kotlin.dsl.*
import org.jetbrains.kotlin.gradle.tasks.*

fun Project.common() {

  tasks.withType<KotlinCompile>()
    .configureEach {

      kotlinOptions {

        allWarningsAsErrors = true

        jvmTarget = "1.8"
      }
    }

// force update all transitive dependencies (prevents some library leaking an old version)
  configurations.all {
    resolutionStrategy {
      force(
        Libs.Kotlin.reflect,
        Libs.Kotlinx.Coroutines.core,
        Libs.Kotlinx.Coroutines.test,
        Libs.Kotlinx.Coroutines.android,
        Libs.RickBusarow.Dispatch.core,
        Libs.RickBusarow.Dispatch.detekt,
        Libs.RickBusarow.Dispatch.espresso,
        Libs.RickBusarow.Dispatch.lifecycle,
        Libs.RickBusarow.Dispatch.lifecycleExtensions,
        Libs.RickBusarow.Dispatch.viewModel,
        Libs.RickBusarow.Dispatch.Test.core,
        Libs.RickBusarow.Dispatch.Test.jUnit4,
        Libs.RickBusarow.Dispatch.Test.jUnit5
      )
    }
  }
}
