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

package modulecheck.gradle

import modulecheck.gradle.platforms.android.AgpApiAccess
import modulecheck.gradle.platforms.internal.GradleConfiguration
import modulecheck.gradle.platforms.internal.GradleProject
import modulecheck.gradle.platforms.kotlin.getJavaPluginExtensionOrNull
import modulecheck.model.sourceset.SourceSetName
import modulecheck.model.sourceset.asSourceSetName
import modulecheck.utils.mapToSet
import org.gradle.api.Plugin
import org.gradle.language.base.plugins.LifecycleBasePlugin

class ModuleCheckPlugin : Plugin<GradleProject> {

  override fun apply(target: GradleProject) {
    val settings = target.extensions
      .create("moduleCheck", ModuleCheckExtension::class.java)

    val agpApiAccess = AgpApiAccess()

    val taskFactory = TaskFactory(target, agpApiAccess)

    taskFactory.registerRootTasks(settings)

    target.tasks
      .matching { it.name == LifecycleBasePlugin.CHECK_TASK_NAME }
      .configureEach {
        it.dependsOn("moduleCheck")
      }
  }
}

internal inline fun GradleProject.onCompileConfigurations(
  crossinline action: (SourceSetName, Set<GradleConfiguration>) -> Unit
) {

  getJavaPluginExtensionOrNull()
    ?.sourceSets
    ?.forEach { sourceSet ->
      val configs = sequenceOf(
        sourceSet.compileOnlyConfigurationName,
        sourceSet.apiConfigurationName,
        sourceSet.implementationConfigurationName,
        sourceSet.runtimeOnlyConfigurationName
      ).mapToSet { configurations.getByName(it) }

      action(sourceSet.name.asSourceSetName(), configs)
    }
}
