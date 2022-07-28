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

package modulecheck.gradle

import modulecheck.gradle.platforms.android.AgpApiAccess
import modulecheck.gradle.platforms.android.internal.onAndroidCompileConfigurationsOrNull
import modulecheck.gradle.platforms.android.internal.onAndroidPlugin
import modulecheck.gradle.platforms.android.isAndroid
import modulecheck.gradle.platforms.getJavaPluginExtensionOrNull
import modulecheck.gradle.platforms.getKotlinExtensionOrNull
import modulecheck.model.sourceset.SourceSetName
import modulecheck.model.sourceset.asSourceSetName
import modulecheck.parsing.gradle.model.GradleConfiguration
import modulecheck.parsing.gradle.model.GradleProject
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
  agpApiAccess: AgpApiAccess,
  crossinline action: (SourceSetName, Set<GradleConfiguration>) -> Unit
) {

  onAndroidPlugin(agpApiAccess) {
    onAndroidCompileConfigurationsOrNull(agpApiAccess, action)
  }

  plugins.withId("com.jetbrains.kotlin.jvm") { plugin ->
    getKotlinExtensionOrNull()
      ?.takeIf { !isAndroid(agpApiAccess) }
      ?.sourceSets
      ?.forEach { sourceSet ->
        val configs = sourceSet.relatedConfigurationNames
          .mapToSet { configurations.getByName(it) }

        action(sourceSet.name.asSourceSetName(), configs)
      }
  }

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
