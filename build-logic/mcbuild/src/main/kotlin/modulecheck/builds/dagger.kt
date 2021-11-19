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

package modulecheck.builds

import org.gradle.api.GradleException
import org.gradle.api.Project
import org.gradle.kotlin.dsl.apply
import org.gradle.kotlin.dsl.dependencies

fun Project.applyDagger(anvil: Boolean, dagger: Boolean) {

  if (!dagger) return

  if (anvil && dagger) throw GradleException(
    "Don't apply both Anvil and Dagger.  If you need Dagger for components, just use Dagger."
  )

  apply(plugin = "org.jetbrains.kotlin.kapt")
  apply(plugin = "com.squareup.anvil")

  dependencies {
    "compileOnly"(project.libsCatalog.dependency("javax-inject"))
    "implementation"(project.libsCatalog.dependency("google-dagger-api"))
    "kapt"(project.libsCatalog.dependency("google-dagger-compiler"))
  }
}
