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

package modulecheck.gradle.platforms.jvm

import modulecheck.gradle.platforms.ConfigurationsFactory
import modulecheck.gradle.platforms.SourceSetsFactory
import modulecheck.gradle.platforms.kotlin.getKotlinExtensionOrNull
import modulecheck.model.dependency.JvmPlatformPlugin
import modulecheck.model.dependency.JvmPlatformPlugin.JavaLibraryPlugin
import modulecheck.model.dependency.JvmPlatformPlugin.KotlinJvmPlugin
import modulecheck.model.dependency.McConfiguration
import modulecheck.parsing.gradle.model.GradleProject
import modulecheck.utils.lazy.LazyDeferred
import modulecheck.utils.lazy.lazyDeferred
import org.gradle.api.artifacts.ExternalModuleDependency
import java.io.File
import javax.inject.Inject

class JvmPlatformPluginFactory @Inject constructor(
  private val configurationsFactory: ConfigurationsFactory,
  private val sourceSetsFactory: SourceSetsFactory
) {

  fun create(gradleProject: GradleProject, hasTestFixturesPlugin: Boolean): JvmPlatformPlugin {

    val configurations = configurationsFactory.create(gradleProject)

    val sourceSets = sourceSetsFactory.create(
      gradleProject = gradleProject,
      configurations = configurations,
      hasTestFixturesPlugin = hasTestFixturesPlugin
    )

    return if (gradleProject.getKotlinExtensionOrNull() != null) {
      KotlinJvmPlugin(sourceSets, configurations)
    } else {
      JavaLibraryPlugin(sourceSets, configurations)
    }
  }
}

/**
 * returns a [Lazy][kotlin.Lazy] set of all external dependency files for a list of configurations
 *
 * NB This is technically unsafe, in that it assumes the files have all been resolved already. If
 * they need to be resolved still, it may happen on a non-Gradle thread, which causes an exception.
 */
fun List<McConfiguration>.classpathLazy(
  gradleProject: GradleProject
): LazyDeferred<List<File>> {
  return lazyDeferred {
    map { (name) ->
      gradleProject.configurations.getByName(name.value)
    }
      .flatMap { gradleConfiguration ->
        if (gradleConfiguration.isCanBeResolved) {
          gradleConfiguration.fileCollection { dep ->
            dep is ExternalModuleDependency
          }
        } else {
          emptyList()
        }
      }
  }
}
