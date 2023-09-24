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

package modulecheck.builds

import com.rickbusarow.kgx.applyOnce
import com.rickbusarow.kgx.dependency
import com.rickbusarow.kgx.libsCatalog
import org.gradle.api.GradleException
import org.gradle.api.Project
import org.gradle.api.artifacts.ExternalDependency

fun Project.applyAnvil() {

  pluginManager.withPlugin("org.jetbrains.kotlin.kapt") {
    throw GradleException(
      "Don't use `mcbuild.anvil()` if kapt is also being applied.  Just use `dagger()`."
    )
  }

  plugins.applyOnce("com.squareup.anvil")

  extensions.configure(com.squareup.anvil.plugin.AnvilExtension::class.java) {
    it.generateDaggerFactories.set(true) // default is false
  }

  dependencies.add("compileOnly", project.libsCatalog.dependency("javax-inject"))
  dependencies.add("compileOnly", project.libsCatalog.dependency("google-dagger-api"))

  // Anvil adds its annotations artifact as 'implementation', which is unnecessary.
  // Replace it with a 'compileOnly' dependency.
  dependencies.add("compileOnly", project.libsCatalog.dependency("square-anvil-annotations"))
  afterEvaluate {
    configurations.named("implementation") {
      val annotations = project.libsCatalog.dependency("square-anvil-annotations").get()

      it.dependencies.removeIf { dep ->
        dep is ExternalDependency && dep.group == annotations.group && dep.name == annotations.name
      }
    }
  }
}
