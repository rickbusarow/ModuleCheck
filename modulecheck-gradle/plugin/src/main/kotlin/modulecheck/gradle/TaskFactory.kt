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

@file:Suppress("TYPEALIAS_EXPANSION_DEPRECATION")

package modulecheck.gradle

import com.android.build.gradle.internal.api.DefaultAndroidSourceDirectorySet
import com.android.build.gradle.internal.res.GenerateLibraryRFileTask
import com.android.build.gradle.internal.res.LinkApplicationAndroidResourcesTask
import com.android.build.gradle.internal.tasks.VariantAwareTask
import com.android.build.gradle.internal.tasks.factory.dependsOn
import com.android.build.gradle.tasks.GenerateBuildConfig
import com.android.build.gradle.tasks.ManifestProcessorTask
import modulecheck.finding.FindingName
import modulecheck.gradle.platforms.Classpath
import modulecheck.gradle.platforms.android.AgpApiAccess
import modulecheck.gradle.platforms.android.AndroidBaseExtension
import modulecheck.gradle.platforms.android.AndroidBaseVariant
import modulecheck.gradle.platforms.android.SafeAgpApiReferenceScope
import modulecheck.gradle.platforms.android.UnsafeDirectAgpApiReference
import modulecheck.gradle.platforms.android.internal.onAndroidPlugin
import modulecheck.gradle.platforms.kotlin.getKotlinExtensionOrNull
import modulecheck.gradle.task.ModuleCheckDependencyResolutionTask
import modulecheck.gradle.task.MultiRuleModuleCheckTask
import modulecheck.gradle.task.SingleRuleModuleCheckTask
import modulecheck.model.sourceset.SourceSetName
import modulecheck.model.sourceset.asSourceSetName
import modulecheck.parsing.gradle.model.GradleProject
import modulecheck.parsing.kotlin.compiler.internal.isKotlinFile
import modulecheck.utils.capitalize
import org.gradle.api.DefaultTask
import org.gradle.api.artifacts.FileCollectionDependency
import org.gradle.api.file.FileCollection
import org.gradle.api.internal.artifacts.dependencies.DefaultSelfResolvingDependency
import org.gradle.api.internal.file.FileCollectionInternal
import org.gradle.api.tasks.TaskCollection
import org.gradle.api.tasks.TaskContainer
import org.gradle.api.tasks.TaskProvider
import org.jetbrains.kotlin.incremental.isJavaFile
import java.io.File
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

  private fun GradleProject.registerResolutionTask(
    rootTasks: List<TaskProvider<*>>
  ) {

    val configFactory = ResolutionConfigFactory()

    onAndroidPlugin(agpApiAccess) {
      handleAndroidPlugin(configFactory, rootTasks)
    }

    pluginManager.withPlugin("java") {
      handleKotlinJvmPlugin(configFactory, rootTasks)
    }

    pluginManager.withPlugin("kotlin") {
      handleKotlinJvmPlugin(configFactory, rootTasks)
    }

    pluginManager.withPlugin("com.jetbrains.kotlin.jvm") {
      handleKotlinJvmPlugin(configFactory, rootTasks)
    }
  }

  private fun GradleProject.handleKotlinJvmPlugin(
    configFactory: ResolutionConfigFactory,
    rootTasks: List<TaskProvider<*>>
  ) {

    getKotlinExtensionOrNull()?.sourceSets
      ?.forEach { sourceSet ->

        val sourceSetName = sourceSet.name.asSourceSetName()

        val configs = sourceSet.relatedConfigurationNames
          .mapNotNull { configurations.findByName(it) }

        val sourceSetCaps = sourceSetName.value.capitalize()

        val androidCfg = project.configurations
          .register("android${sourceSetCaps}ResourceArtifacts") {
            it.isCanBeResolved = true
            it.isCanBeConsumed = true
          }

        val cfgs = configs.map { config ->
          configFactory.create(
            project = project,
            configuration = config
          )
        } + androidCfg

        val resolveTask = project.tasks.register(
          "resolve${sourceSetCaps}Dependencies",
          ModuleCheckDependencyResolutionTask::class.java
        ) { task ->

          task.classpathFile.set(Classpath.reportFile(project, sourceSetName))
          cfgs.forEach { cfg ->
            task.dependsOn(cfg)
            task.inputs.files(cfg)
          }
        }

        rootTasks.forEach { it.dependsOn(resolveTask) }
      }
  }

  @OptIn(UnsafeDirectAgpApiReference::class)
  private fun GradleProject.handleAndroidPlugin(
    configFactory: ResolutionConfigFactory,
    rootTasks: List<TaskProvider<*>>
  ) {
    agpApiAccess.ifSafeOrNull(this) {

      val baseExtension = requireBaseExtension()

      fun register(variant: AndroidBaseVariant, isTestingSourceSet: Boolean) {
        afterAndroidVariants(
          project = this@handleAndroidPlugin,
          sourceSetName = variant.sourceSets.last().name.asSourceSetName(),
          variantName = variant.name,
          isTestingSourceSet = isTestingSourceSet,
          configFactory = configFactory,
          baseExtension = baseExtension,
          rootTasks = rootTasks
        )
      }

      baseExtension.baseVariants().configureEach { variant: AndroidBaseVariant ->

        register(variant, isTestingSourceSet = false)

        variant.androidTestVariantOrNull()?.let { androidTestVariant ->
          register(androidTestVariant, isTestingSourceSet = true)
        }
        variant.unitTestVariantOrNull()?.let { unitTestVariant ->
          register(unitTestVariant, isTestingSourceSet = true)
        }
      }

      fun register(sourceSetName: SourceSetName, isTestingSourceSet: Boolean) {
        afterAndroidVariants(
          project = this@handleAndroidPlugin,
          sourceSetName = sourceSetName,
          variantName = null,
          isTestingSourceSet = isTestingSourceSet,
          configFactory = configFactory,
          baseExtension = baseExtension,
          rootTasks = rootTasks
        )
      }
      register(SourceSetName.MAIN, isTestingSourceSet = false)
      register(SourceSetName.ANDROID_TEST, isTestingSourceSet = true)
      register(SourceSetName.TEST, isTestingSourceSet = true)
    }
  }

  @Suppress("UnstableApiUsage")
  @OptIn(UnsafeDirectAgpApiReference::class)
  fun SafeAgpApiReferenceScope.afterAndroidVariants(
    project: GradleProject,
    sourceSetName: SourceSetName,
    variantName: String?,
    isTestingSourceSet: Boolean,
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

    val androidCfg = project.configurations
      .register("android${sourceSetCaps}ResourceArtifacts") {
        it.isCanBeResolved = true
        it.isCanBeConsumed = true
      }

    val cfgs = configs.map { config ->
      configFactory.create(
        project = project,
        configuration = config
      )
    } + androidCfg

    val resolveTask = project.tasks.register(
      "resolve${sourceSetCaps}Dependencies",
      ModuleCheckDependencyResolutionTask::class.java
    ) { task ->

      task.classpathFile.set(Classpath.reportFile(project, sourceSetName))

      cfgs.forEach { cfg ->
        task.dependsOn(cfg)
        task.inputs.files(cfg)
      }

      fun <T> TaskContainer.variantTask(
        tClass: KClass<T>,
        predicate: (T) -> Boolean = { true }
      ): TaskCollection<T>
        where T : VariantAwareTask,
              T : DefaultTask {
        return withType(tClass.java)
          .matching { it.variantName == variantName && predicate(it) }
      }

      val hasSourceFiles = (sourceSet.kotlin as DefaultAndroidSourceDirectorySet)
        .srcDirs.any { dir -> dir.hasSource() } ||
        (sourceSet.java as DefaultAndroidSourceDirectorySet)
          .srcDirs.any { dir -> dir.hasSource() } ||
        sourceSet.res.srcDirs.any { it.hasSource() }

      val isApplication = baseExtension.isAndroidAppExtension()

      sequenceOf(
        project.tasks.variantTask(ManifestProcessorTask::class) {
          sourceSet.manifest.srcFile.exists()
        },
        project.tasks.variantTask(GenerateBuildConfig::class),
        project.tasks.variantTask(LinkApplicationAndroidResourcesTask::class) {

          when {
            it.name != "process${variantName?.capitalize()}Resources" -> false
            isApplication -> true // hasSourceFiles
            else -> !isTestingSourceSet || hasSourceFiles
          }
        },
        project.tasks.variantTask(GenerateLibraryRFileTask::class) {

          hasSourceFiles && it.name == "generate${variantName?.capitalize()}Resources"
        }
      )
        .forEach { variantTaskCollection ->
          variantTaskCollection.forEach { variantTask ->
            androidCfg.configure {
              it.dependencies.add(variantTask.outputs.files.asDependency())
            }
            task.inputs.files(variantTask.outputs.files)
            task.dependsOn(variantTask)
          }
        }
    }

    rootTasks.forEach { it.dependsOn(resolveTask) }
  }
}

fun File.hasSource() = walkBottomUp()
  .any { file -> file.isFile && (file.isKotlinFile() || file.isJavaFile()) }

fun FileCollection.asDependency(): FileCollectionDependency =
  DefaultSelfResolvingDependency(this as FileCollectionInternal)
