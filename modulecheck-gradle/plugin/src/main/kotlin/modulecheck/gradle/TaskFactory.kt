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

import com.android.build.gradle.internal.res.GenerateLibraryRFileTask
import com.android.build.gradle.internal.res.LinkApplicationAndroidResourcesTask
import com.android.build.gradle.internal.tasks.factory.dependsOn
import com.android.build.gradle.tasks.GenerateBuildConfig
import com.android.build.gradle.tasks.ManifestProcessorTask
import modulecheck.finding.FindingName
import modulecheck.gradle.platforms.android.AgpApiAccess
import modulecheck.gradle.platforms.android.internal.generatesBuildConfig
import modulecheck.gradle.platforms.android.internal.isMissingManifestFile
import modulecheck.gradle.platforms.android.isAndroid
import modulecheck.gradle.task.ModuleCheckDependencyResolutionTask
import modulecheck.gradle.task.MultiRuleModuleCheckTask
import modulecheck.gradle.task.SingleRuleModuleCheckTask
import modulecheck.model.dependency.ConfigurationName
import modulecheck.model.dependency.asConfigurationName
import modulecheck.model.sourceset.SourceSetName
import modulecheck.parsing.gradle.model.GradleConfiguration
import modulecheck.parsing.gradle.model.GradleProject
import modulecheck.utils.capitalize
import org.gradle.api.NamedDomainObjectProvider
import org.gradle.api.Project
import org.gradle.api.Task
import org.gradle.api.artifacts.ExternalModuleDependency
import org.gradle.api.internal.artifacts.dependencies.DefaultSelfResolvingDependency
import org.gradle.api.internal.file.FileCollectionInternal
import org.gradle.api.tasks.TaskProvider

internal class TaskFactory(
  private val target: GradleProject,
  private val agpApiAccess: AgpApiAccess
) {

  // Gradle doesn't use semantic versioning, so for instance `7.4` is "7.4" and not "7.4.0".
  // Fortunately `7.0` was "7.0" and not "7".  It's safe to use a simple string comparison.
  val disableConfigCache = target.gradle.gradleVersion >= "7.4"

  fun registerRootTasks(settings: ModuleCheckExtension) {
    target.registerSingleRuleTasks(
      taskName = "moduleCheckSortDependencies",
      findingName = FindingName("sort-dependencies"),
      includeAuto = true
    )
    target.registerSingleRuleTasks(
      taskName = "moduleCheckSortPlugins",
      findingName = FindingName("sort-plugins"),
      includeAuto = true
    )
    target.registerSingleRuleTasks(
      taskName = "moduleCheckDepths",
      findingName = FindingName("project-depth"),
      includeAuto = false,
      doFirstAction = {
        settings.checks.depths = true
        settings.reports.depths.enabled = true
      }
    )
    target.registerSingleRuleTasks(
      taskName = "moduleCheckGraphs",
      findingName = FindingName("project-depth"),
      includeAuto = false,
      doFirstAction = {
        settings.reports.graphs.enabled = true
      }
    )

    target.registerMultiRuleTasks(
      taskName = "moduleCheck",
      includeAuto = true
    )
  }

  private fun GradleProject.registerSingleRuleTasks(
    taskName: String,
    findingName: FindingName,
    includeAuto: Boolean,
    doFirstAction: (() -> Unit)? = null
  ) = buildList {
    add(
      tasks.register(taskName, SingleRuleModuleCheckTask::class.java) { task ->
        task.configure(
          findingName = findingName,
          autoCorrect = false,
          disableConfigCache = disableConfigCache
        )
        if (doFirstAction != null) {
          task.doFirst { doFirstAction() }
        }
      }
    )
    if (includeAuto) {
      add(
        tasks.register("${taskName}Auto", SingleRuleModuleCheckTask::class.java) { task ->
          task.configure(
            findingName = findingName,
            autoCorrect = true,
            disableConfigCache = disableConfigCache
          )
          if (doFirstAction != null) {
            task.doFirst { doFirstAction() }
          }
        }
      )
    }
  }

  @Suppress("LongParameterList")
  private fun GradleProject.registerMultiRuleTasks(
    taskName: String,
    includeAuto: Boolean,
    doFirstAction: (() -> Unit)? = null
  ) {

    val tasks = buildList {

      add(
        tasks.register(taskName, MultiRuleModuleCheckTask::class.java) {
          it.configure(autoCorrect = false, disableConfigCache = disableConfigCache)
        }
      )
      if (includeAuto) {
        add(
          tasks.register("${taskName}Auto", MultiRuleModuleCheckTask::class.java) {
            it.configure(autoCorrect = true, disableConfigCache = disableConfigCache)
          }
        )
      }
    }

    val resolveTasks = allprojects.map { project ->
      project.registerResolutionTask(tasks)
    }

    tasks.forEach { taskProvider ->

      if (doFirstAction != null) {
        taskProvider.configure {
          it.doFirst { doFirstAction() }
          it.dependsOn(resolveTasks)
        }
      }
    }
  }

  fun SourceSetName.aggregateConfigName(): ConfigurationName {
    val sourceSetCaps = value.capitalize()
    return "moduleCheck${sourceSetCaps}AggregateDependencies".asConfigurationName()
  }

  private fun Project.registerResolutionTask(
    rootTasks: List<TaskProvider<*>>
  ) {

    onCompileConfigurations(agpApiAccess) { sourceSetName, configs ->

      val sourceSetCaps = sourceSetName.value.capitalize()

      val cfg = configurations
        .register(
          "moduleCheck${sourceSetCaps}AggregateDependencies"
        ) { config ->

          val external = configs.map {
            it.allDependencies.withType(ExternalModuleDependency::class.java)
          }
            .flatten()

          configs.forEach {
            config.extendsFrom(it)
          }

          println("                                                      -----  external")
          external.joinToString("\n").also(::println)

          config.dependencies.addAll(external)
        }

      val resolveTask = tasks.register(
        "resolve${sourceSetCaps}AggregateDependencies",
        ModuleCheckDependencyResolutionTask::class.java
      ) { task ->
        task.dependsOn(cfg)
        task.inputs.files(cfg)

        task.addInternalDependencies(
          leafProject = this@registerResolutionTask,
          configuration = cfg
        )
      }

      rootTasks.forEach { it.dependsOn(resolveTask) }
    }
  }

  private fun ModuleCheckDependencyResolutionTask.addInternalDependencies(
    leafProject: Project,
    configuration: NamedDomainObjectProvider<GradleConfiguration>
  ) = apply {

    fun addDependencies(depTask: Task) {
      configuration.configure { config ->
        val filesDep = DefaultSelfResolvingDependency(
          depTask.outputs.files as FileCollectionInternal
        )
        config.dependencies.add(filesDep)
      }
      inputs.files(depTask.outputs.files)
      dependsOn(depTask)
    }

    if (leafProject.isMissingManifestFile(this@TaskFactory.agpApiAccess)) {
      leafProject.tasks.withType(ManifestProcessorTask::class.java)
        .forEach { manifestTask ->
          addDependencies(manifestTask)
        }
    }

    if (leafProject.generatesBuildConfig(this@TaskFactory.agpApiAccess)) {
      leafProject.tasks.withType(GenerateBuildConfig::class.java)
        .forEach { buildConfigTask ->
          addDependencies(buildConfigTask)
        }
    }
    if (leafProject.isAndroid(this@TaskFactory.agpApiAccess)) {
      leafProject.tasks.withType(LinkApplicationAndroidResourcesTask::class.java)
        .matching { "process[A-Z][a-z]*Resources".toRegex().matches(it.name) }
        .forEach { androidResourcesTask ->
          addDependencies(androidResourcesTask)
        }
      leafProject.tasks.withType(GenerateLibraryRFileTask::class.java)
        .forEach { androidResourcesTask ->
          addDependencies(androidResourcesTask)
        }
    }
  }
}
