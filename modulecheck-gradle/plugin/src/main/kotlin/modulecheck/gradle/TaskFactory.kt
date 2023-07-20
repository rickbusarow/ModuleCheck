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

@file:Suppress("TYPEALIAS_EXPANSION_DEPRECATION")

package modulecheck.gradle

import modulecheck.finding.FindingName
import modulecheck.gradle.internal.configuring
import modulecheck.gradle.internal.dependsOn
import modulecheck.gradle.internal.whenElementRegistered
import modulecheck.gradle.platforms.Classpath
import modulecheck.gradle.platforms.android.AgpApiAccess
import modulecheck.gradle.platforms.android.AgpBaseExtension
import modulecheck.gradle.platforms.android.AgpBaseVariant
import modulecheck.gradle.platforms.android.AgpDefaultAndroidSourceDirectorySet
import modulecheck.gradle.platforms.android.AgpGenerateBuildConfig
import modulecheck.gradle.platforms.android.AgpGenerateLibraryRFileTask
import modulecheck.gradle.platforms.android.AgpLinkApplicationAndroidResourcesTask
import modulecheck.gradle.platforms.android.AgpManifestProcessorTask
import modulecheck.gradle.platforms.android.AgpVariantAwareTask
import modulecheck.gradle.platforms.android.SafeAgpApiReferenceScope
import modulecheck.gradle.platforms.android.UnsafeDirectAgpApiReference
import modulecheck.gradle.platforms.android.internal.onAndroidPlugin
import modulecheck.gradle.platforms.internal.GradleConfiguration
import modulecheck.gradle.platforms.internal.GradleProject
import modulecheck.gradle.platforms.internal.UnsafeInternalGradleApiReference
import modulecheck.gradle.platforms.kotlin.getKotlinExtensionOrNull
import modulecheck.gradle.task.ModuleCheckDependencyResolutionTask
import modulecheck.gradle.task.MultiRuleModuleCheckTask
import modulecheck.gradle.task.SingleRuleModuleCheckTask
import modulecheck.model.sourceset.SourceSetName
import modulecheck.model.sourceset.asSourceSetName
import modulecheck.parsing.kotlin.compiler.internal.isKotlinFile
import modulecheck.utils.capitalize
import modulecheck.utils.lazy.unsafeLazy
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
  private val rootProject: GradleProject,
  private val agpApiAccess: AgpApiAccess
) {

  private val configFactory by lazy { ResolutionConfigFactory() }

  // Gradle doesn't use semantic versioning, so for instance `7.4` is "7.4" and not "7.4.0".
  // Fortunately `7.0` was "7.0" and not "7".  It's safe to use a simple string comparison.
  private val disableConfigCache = rootProject.gradle.gradleVersion >= "7.4"

  fun registerRootTasks(settings: ModuleCheckExtension) {
    registerSingleRuleTasks(
      taskName = "moduleCheckSortDependencies",
      findingName = FindingName("sort-dependencies"),
      includeAuto = true
    )
    registerSingleRuleTasks(
      taskName = "moduleCheckSortPlugins",
      findingName = FindingName("sort-plugins"),
      includeAuto = true
    )
    registerSingleRuleTasks(
      taskName = "moduleCheckDepths",
      findingName = FindingName("project-depth"),
      includeAuto = false
    ) {
      settings.checks.depths = true
      settings.reports.depths.enabled = true
    }
    registerSingleRuleTasks(
      taskName = "moduleCheckGraphs",
      findingName = FindingName("project-depth"),
      includeAuto = false
    ) {
      settings.reports.graphs.enabled = true
    }

    registerMultiRuleTasks(
      rootProject = rootProject,
      taskName = "moduleCheck",
      includeAuto = true
    )
  }

  private fun registerSingleRuleTasks(
    taskName: String,
    findingName: FindingName,
    includeAuto: Boolean,
    doFirstAction: (() -> Unit)? = null
  ) = buildList {
    this.add(
      rootProject.tasks.register(taskName, SingleRuleModuleCheckTask::class.java) { task ->
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
      this.add(
        rootProject.tasks.register(
          "${taskName}Auto",
          SingleRuleModuleCheckTask::class.java
        ) { task ->
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

  private fun registerMultiRuleTasks(
    rootProject: GradleProject,
    taskName: String,
    includeAuto: Boolean,
    doFirstAction: (() -> Unit)? = null
  ) {

    val rootTasks = buildList {

      this.add(
        rootProject.tasks.register(taskName, MultiRuleModuleCheckTask::class.java) {
          it.configure(autoCorrect = false, disableConfigCache = disableConfigCache)
        }
      )
      if (includeAuto) {
        this.add(
          rootProject.tasks.register("${taskName}Auto", MultiRuleModuleCheckTask::class.java) {
            it.configure(autoCorrect = true, disableConfigCache = disableConfigCache)
          }
        )
      }
    }

    val resolveTasks = rootProject.allprojects.map { anyProject ->

      @OptIn(UnsafeDirectAgpApiReference::class)
      anyProject.onAndroidPlugin(agpApiAccess) {
        handleAndroidPlugin(anyProject, configFactory, rootTasks)
      }

      anyProject.pluginManager.withPlugin("java") {
        handleKotlinJvmPlugin(anyProject, configFactory, rootTasks)
      }

      anyProject.pluginManager.withPlugin("kotlin") {
        handleKotlinJvmPlugin(anyProject, configFactory, rootTasks)
      }

      anyProject.pluginManager.withPlugin("com.jetbrains.kotlin.jvm") {
        handleKotlinJvmPlugin(anyProject, configFactory, rootTasks)
      }
    }

    rootTasks.forEach { taskProvider ->

      if (doFirstAction != null) {
        taskProvider.configure {
          it.doFirst { doFirstAction() }
          it.dependsOn(resolveTasks)
        }
      }
    }
  }

  private fun handleKotlinJvmPlugin(
    anyProject: GradleProject,
    resolutionConfigFactory: ResolutionConfigFactory,
    rootTasks: List<TaskProvider<*>>
  ) {

    val sourceSetContainer = anyProject.getKotlinExtensionOrNull()?.sourceSets ?: return

    @OptIn(UnsafeInternalGradleApiReference::class)
    sourceSetContainer.whenElementRegistered { name ->

      val sourceSetName = name.asSourceSetName()

      val jarDependencyConfigs = anyProject.project.provider {
        sourceSetContainer.getByName(name)
          .relatedConfigurationNames
          .jarDependencyResolutionConfigs(anyProject.project, resolutionConfigFactory)
      }

      val resolveTask = ModuleCheckDependencyResolutionTask
        .register(project = anyProject.project, sourceSetName = sourceSetName)
        .configuring { task ->

          task.classpathReportFile.set(Classpath.reportFile(anyProject.project, sourceSetName))

          task.classpathToResolve.from(jarDependencyConfigs)
          task.dependsOn(jarDependencyConfigs)
        }

      rootTasks.forEach { it.dependsOn(resolveTask) }
    }
  }

  @OptIn(UnsafeDirectAgpApiReference::class)
  private fun handleAndroidPlugin(
    anyProject: GradleProject,
    configFactory: ResolutionConfigFactory,
    rootTasks: List<TaskProvider<*>>
  ) {

    agpApiAccess.ifSafeOrNull(anyProject) {

      val registered = mutableSetOf<SourceSetName>()

      val baseExtension = requireBaseExtension()

      fun register(variant: AgpBaseVariant, isTestingSourceSet: Boolean) {
        val sourceSetName = variant.sourceSets.last().name.asSourceSetName()
        if (registered.add(sourceSetName)) {
          afterAndroidVariants(
            project = anyProject,
            sourceSetName = sourceSetName,
            variantName = variant.name,
            isTestingSourceSet = isTestingSourceSet,
            resolutionConfigFactory = configFactory,
            baseExtension = baseExtension,
            rootTasks = rootTasks
          )
        }
      }

      fun register(sourceSetName: SourceSetName, isTestingSourceSet: Boolean) {
        if (registered.add(sourceSetName)) {
          afterAndroidVariants(
            project = anyProject,
            sourceSetName = sourceSetName,
            variantName = null,
            isTestingSourceSet = isTestingSourceSet,
            resolutionConfigFactory = configFactory,
            baseExtension = baseExtension,
            rootTasks = rootTasks
          )
        }
      }

      baseExtension.baseVariants().configureEach { variant: AgpBaseVariant ->

        register(variant, isTestingSourceSet = false)

        variant.androidTestVariantOrNull()?.let { androidTestVariant ->
          register(androidTestVariant, isTestingSourceSet = true)
        }
        variant.unitTestVariantOrNull()?.let { unitTestVariant ->
          register(unitTestVariant, isTestingSourceSet = true)
        }

        register(SourceSetName.MAIN, isTestingSourceSet = false)
        register(SourceSetName.ANDROID_TEST, isTestingSourceSet = true)
        register(SourceSetName.TEST, isTestingSourceSet = true)
      }
    }
  }

  @OptIn(UnsafeDirectAgpApiReference::class)
  fun SafeAgpApiReferenceScope.afterAndroidVariants(
    project: GradleProject,
    sourceSetName: SourceSetName,
    variantName: String?,
    isTestingSourceSet: Boolean,
    resolutionConfigFactory: ResolutionConfigFactory,
    baseExtension: AgpBaseExtension,
    rootTasks: List<TaskProvider<*>>
  ) {

    val sourceSet = baseExtension.sourceSets.getByName(sourceSetName.value)

    val androidSdkJarConfigs = sequenceOf(
      "androidApis",
      "androidJdkImage"
    ).mapNotNull { configName -> project.configurations.findByName(configName) }

    val jarDependencyConfigs = project.provider {
      sourceSet.relatedConfigurationNames
        .jarDependencyResolutionConfigs(project, resolutionConfigFactory)
        .plus(androidSdkJarConfigs)
    }

    val sourceSetCaps = sourceSetName.value.capitalize()

    val androidResourceArtifactsConfig = project.configurations
      .register("android${sourceSetCaps}ResourceArtifacts") {
        it.isCanBeResolved = true
        it.isCanBeConsumed = true
      }

    val resolveTask = ModuleCheckDependencyResolutionTask
      .register(project = project, sourceSetName = sourceSetName)
      .configuring { task ->

        task.classpathReportFile.set(Classpath.reportFile(project, sourceSetName))
        task.classpathToResolve.from(androidResourceArtifactsConfig, jarDependencyConfigs)
        task.dependsOn(androidResourceArtifactsConfig, jarDependencyConfigs)

        val hasKotlinSources = (sourceSet.kotlin as AgpDefaultAndroidSourceDirectorySet)
          .srcDirs.any { dir -> dir.hasSource() }

        val hasJavaSources by unsafeLazy {
          (sourceSet.kotlin as AgpDefaultAndroidSourceDirectorySet)
            .srcDirs.any { dir -> dir.hasSource() }
        }

        val hasResSources by unsafeLazy { sourceSet.res.srcDirs.any { it.hasSource() } }

        val hasSourceFiles = hasKotlinSources || hasJavaSources || hasResSources

        val isApplication = baseExtension.isAndroidAppExtension()

        val manifestProcessorTasks = project.tasks
          .variantTask(
            tClass = AgpManifestProcessorTask::class,
            variantName = variantName
          ) { sourceSet.manifest.srcFile.exists() }

        val generateBuildConfigTasks = project.tasks
          .variantTask(
            tClass = AgpGenerateBuildConfig::class,
            variantName = variantName
          )

        val linkResourcesTasks = project.tasks
          .variantTask(
            tClass = AgpLinkApplicationAndroidResourcesTask::class,
            variantName = variantName
          ) { variantTask ->

            when {
              variantTask.name != "process${variantName?.capitalize()}Resources" -> false
              isApplication -> true
              !hasSourceFiles -> false
              else -> !isTestingSourceSet
            }
          }

        val generateRFileTasks = project.tasks
          .variantTask(
            tClass = AgpGenerateLibraryRFileTask::class,
            variantName = variantName
          )

        sequenceOf(
          manifestProcessorTasks,
          generateBuildConfigTasks,
          linkResourcesTasks,
          generateRFileTasks
        )
          .forEach { variantTaskCollection ->
            androidResourceArtifactsConfig.configure { artifactsConfig ->
              val resFiles = variantTaskCollection.flatMap { it.outputs.files }
              artifactsConfig.dependencies.add(project.files(resFiles).asDependency())
              task.inputs.files(resFiles)
            }
            task.dependsOn(variantTaskCollection)
          }
      }

    rootTasks.forEach { it.dependsOn(resolveTask) }
  }

  private fun List<String>.jarDependencyResolutionConfigs(
    project: GradleProject,
    resolutionConfigFactory: ResolutionConfigFactory
  ): List<GradleConfiguration> {
    return mapNotNull { configName -> project.configurations.findByName(configName) }
      .map { config ->
        resolutionConfigFactory.create(project = project, sourceConfiguration = config)
      }
  }

  @UnsafeDirectAgpApiReference
  private fun <T> TaskContainer.variantTask(
    tClass: KClass<T>,
    variantName: String?,
    predicate: (T) -> Boolean = { true }
  ): TaskCollection<T>
    where T : AgpVariantAwareTask,
          T : DefaultTask {
    return withType(tClass.java)
      .matching { it.variantName == variantName && predicate(it) }
  }
}

/** */
fun File.hasSource() = walkBottomUp()
  .any { file -> file.isFile && (file.isKotlinFile() || file.isJavaFile()) }

/** */
fun FileCollection.asDependency(): FileCollectionDependency =
  DefaultSelfResolvingDependency(this as FileCollectionInternal)
