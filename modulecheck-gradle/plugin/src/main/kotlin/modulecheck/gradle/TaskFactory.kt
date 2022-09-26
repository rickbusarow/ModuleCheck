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

import com.android.build.gradle.api.BaseVariant
import com.android.build.gradle.internal.res.GenerateLibraryRFileTask
import com.android.build.gradle.internal.res.LinkApplicationAndroidResourcesTask
import com.android.build.gradle.internal.tasks.VariantAwareTask
import com.android.build.gradle.internal.tasks.factory.dependsOn
import com.android.build.gradle.tasks.GenerateBuildConfig
import com.android.build.gradle.tasks.ManifestProcessorTask
import modulecheck.finding.FindingName
import modulecheck.gradle.platforms.android.AgpApiAccess
import modulecheck.gradle.platforms.android.AndroidBaseExtension
import modulecheck.gradle.platforms.android.UnsafeDirectAgpApiReference
import modulecheck.gradle.platforms.android.androidTestVariant
import modulecheck.gradle.platforms.android.baseVariants
import modulecheck.gradle.platforms.android.internal.onAndroidPlugin
import modulecheck.gradle.platforms.android.unitTestVariant
import modulecheck.gradle.task.ModuleCheckDependencyResolutionTask
import modulecheck.gradle.task.MultiRuleModuleCheckTask
import modulecheck.gradle.task.SingleRuleModuleCheckTask
import modulecheck.model.sourceset.SourceSetName
import modulecheck.model.sourceset.asSourceSetName
import modulecheck.parsing.gradle.model.GradleProject
import modulecheck.utils.capitalize
import org.gradle.api.DefaultTask
import org.gradle.api.Project
import org.gradle.api.artifacts.FileCollectionDependency
import org.gradle.api.file.FileCollection
import org.gradle.api.internal.artifacts.dependencies.DefaultSelfResolvingDependency
import org.gradle.api.internal.file.FileCollectionInternal
import org.gradle.api.tasks.TaskCollection
import org.gradle.api.tasks.TaskContainer
import org.gradle.api.tasks.TaskProvider
import kotlin.reflect.KClass

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

  @OptIn(UnsafeDirectAgpApiReference::class)
  private fun Project.registerResolutionTask(
    rootTasks: List<TaskProvider<*>>
  ) {

    val configFactory = ResolutionConfigFactory()

    onAndroidPlugin(agpApiAccess) {

      agpApiAccess.ifSafeOrNull(this@registerResolutionTask) {

        val baseExtension = requireBaseExtension()

        fun register(variant: BaseVariant) {
          afterAndroidVariants(
            project = this@registerResolutionTask,
            sourceSetName = variant.sourceSets.last().name.asSourceSetName(),
            variantName = variant.name,
            configFactory = configFactory,
            baseExtension = baseExtension,
            rootTasks = rootTasks
          )
        }

        baseExtension.baseVariants().configureEach { variant: BaseVariant ->

          register(variant)

          variant.androidTestVariant()?.let { androidTestVariant ->
            register(androidTestVariant)
          }
          variant.unitTestVariant()?.let { unitTestVariant ->
            register(unitTestVariant)
          }
        }

        fun register(sourceSetName: SourceSetName) {
          afterAndroidVariants(
            project = this@registerResolutionTask,
            sourceSetName = sourceSetName,
            variantName = null,
            configFactory = configFactory,
            baseExtension = baseExtension,
            rootTasks = rootTasks
          )
        }
        register(SourceSetName.MAIN)
        register(SourceSetName.ANDROID_TEST)
        register(SourceSetName.TEST)
      }
    }
  }

  @Suppress("UnstableApiUsage")
  @OptIn(UnsafeDirectAgpApiReference::class)
  fun afterAndroidVariants(
    project: GradleProject,
    sourceSetName: SourceSetName,
    variantName: String?,
    configFactory: ResolutionConfigFactory,
    baseExtension: AndroidBaseExtension,
    rootTasks: List<TaskProvider<*>>
  ) {

    val sourceSet = baseExtension.sourceSets.getByName(sourceSetName.value)

    val configs = sequenceOf(
      sourceSet.apiConfigurationName,
      sourceSet.implementationConfigurationName,
      sourceSet.compileOnlyConfigurationName,
      sourceSet.runtimeOnlyConfigurationName
    )
      .mapNotNull { configName -> project.configurations.findByName(configName) }

    val sourceSetCaps = sourceSetName.value.capitalize()

    val cfg = configFactory.create(
      project = project,
      configurations = configs.toList()
    )

    val resolveTask = project.tasks.register(
      "resolve${sourceSetCaps}AggregateDependencies",
      ModuleCheckDependencyResolutionTask::class.java
    ) { task ->

      task.classpathFile
        .set(ModuleCheckDependencyResolutionTask.classpathFile(project, sourceSetName))

      task.dependsOn(cfg)
      task.inputs.files(cfg)

      fun <T> TaskContainer.variantTask(
        tClass: KClass<T>
      ): TaskCollection<T>
        where T : VariantAwareTask,
              T : DefaultTask {
        return withType(tClass.java)
          .matching { it.variantName == variantName }
      }

      sequenceOf(
        project.tasks.variantTask(ManifestProcessorTask::class),
        project.tasks.variantTask(GenerateBuildConfig::class),
        project.tasks.variantTask(LinkApplicationAndroidResourcesTask::class),
        project.tasks.variantTask(GenerateLibraryRFileTask::class)
      )
        .forEach { variantTaskCollection ->
          variantTaskCollection.forEach { variantTask ->

            cfg.dependencies.add(variantTask.outputs.files.asDependency())
            task.inputs.files(variantTask.outputs.files)
            task.dependsOn(variantTask)
          }
        }
    }

    rootTasks.forEach { it.dependsOn(resolveTask) }
  }
}

fun FileCollection.asDependency(): FileCollectionDependency =
  DefaultSelfResolvingDependency(this as FileCollectionInternal)
