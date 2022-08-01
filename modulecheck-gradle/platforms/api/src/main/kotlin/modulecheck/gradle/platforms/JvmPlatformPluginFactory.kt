/*
 * Copyright (C) 2021-2024 Rick Busarow
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

package modulecheck.gradle.platforms

import modulecheck.gradle.platforms.sourcesets.existingFiles
import modulecheck.model.dependency.Configurations
import modulecheck.model.dependency.JvmPlatformPlugin
import modulecheck.model.dependency.JvmPlatformPlugin.JavaLibraryPlugin
import modulecheck.model.dependency.JvmPlatformPlugin.KotlinJvmPlugin
import modulecheck.model.dependency.McConfiguration
import modulecheck.model.dependency.McSourceSet
import modulecheck.model.dependency.ProjectPath.StringProjectPath
import modulecheck.model.dependency.asConfigurationName
import modulecheck.model.sourceset.SourceSetName
import modulecheck.model.sourceset.asSourceSetName
import modulecheck.parsing.gradle.model.GradleProject
import modulecheck.parsing.gradle.model.GradleSourceSet
import modulecheck.utils.flatMapToSet
import modulecheck.utils.lazy.lazyDeferred
import modulecheck.utils.requireNotNull
import org.gradle.api.plugins.JavaPluginExtension
import org.jetbrains.kotlin.config.JvmTarget
import org.jetbrains.kotlin.config.LanguageVersion
import org.jetbrains.kotlin.gradle.dsl.KotlinProjectExtension
import org.jetbrains.kotlin.gradle.plugin.KotlinSourceSet
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

fun GradleProject.getKotlinExtensionOrNull(): KotlinProjectExtension? =
  extensions.findByName("kotlin") as? KotlinProjectExtension

fun KotlinProjectExtension.toSourceSets(
  kotlinEnvironmentFactory: KotlinEnvironmentFactory,
  parsedConfigurations: Configurations,
  javaExtension: JavaPluginExtension?,
  projectPath: StringProjectPath,
  jvmTarget: JvmTarget,
  kotlinVersion: LanguageVersion?
): Map<SourceSetName, McSourceSet> {
  return buildMap<SourceSetName, McSourceSet> {

    sourceSets.forEach { kotlinSourceSet ->

      val sourceSet = kotlinSourceSet.toMcSourceSet(
        kotlinEnvironmentFactory = kotlinEnvironmentFactory,
        parsedConfigurations = parsedConfigurations,
        javaExtension = javaExtension,
        projectPath = projectPath,
        jvmTarget = jvmTarget,
        kotlinVersion = kotlinVersion
      ) { sourceSetName, configs ->
        parseHierarchy(sourceSetName, configs)
      }

      put(kotlinSourceSet.name.asSourceSetName(), sourceSet)
    }
  }
}

fun KotlinSourceSet.toMcSourceSet(
  kotlinEnvironmentFactory: KotlinEnvironmentFactory,
  parsedConfigurations: Configurations,
  javaExtension: JavaPluginExtension?,
  projectPath: StringProjectPath,
  jvmTarget: JvmTarget,
  kotlinVersion: LanguageVersion?,
  hierarchies: (
    sourceSetName: SourceSetName,
    configurations: List<McConfiguration>
  ) -> Pair<Lazy<List<SourceSetName>>, Lazy<List<SourceSetName>>>
): McSourceSet {
  val sourceSetName = name.asSourceSetName()

  val configs = listOf(
    compileOnlyConfigurationName,
    apiConfigurationName,
    implementationConfigurationName,
    runtimeOnlyConfigurationName
  ).mapNotNull { parsedConfigurations[it.asConfigurationName()] }

  val (
    upstreamLazy,
    downstreamLazy
  ) = hierarchies(sourceSetName, configs)

  val classpath = javaExtension?.sourceSets
    ?.findByName(sourceSetName.value)
    ?.classpath()
    ?.filter { it.exists() }
    ?.filterNotNull()
    ?.distinct()
    .orEmpty()

  kotlinVersion.requireNotNull {
    "kotlin version is null for project -- ${projectPath.value}"
  }

  val jvmFiles = this.kotlin.srcDirs

  val kotlinEnvironmentDeferred = lazyDeferred {
    kotlinEnvironmentFactory.create(
      projectPath = projectPath,
      sourceSetName = sourceSetName,
      classpathFiles = lazyDeferred { classpath },
      sourceDirs = jvmFiles,
      kotlinLanguageVersion = kotlinVersion,
      jvmTarget = jvmTarget
    )
  }

  return McSourceSet(
    name = sourceSetName,
    compileOnlyConfiguration = parsedConfigurations
      .getValue(this.compileOnlyConfigurationName.asConfigurationName()),
    apiConfiguration = parsedConfigurations
      .get(this.apiConfigurationName.asConfigurationName()),
    implementationConfiguration = parsedConfigurations
      .getValue(this.implementationConfigurationName.asConfigurationName()),
    runtimeOnlyConfiguration = parsedConfigurations
      .getValue(this.runtimeOnlyConfigurationName.asConfigurationName()),
    annotationProcessorConfiguration = null,
    jvmFiles = jvmFiles,
    resourceFiles = this.resources.sourceDirectories.files,
    layoutFiles = emptySet(),
    jvmTarget = jvmTarget,
    kotlinEnvironmentDeferred = kotlinEnvironmentDeferred,
    upstreamLazy = upstreamLazy,
    downstreamLazy = downstreamLazy
  )
}

private fun MutableMap<SourceSetName, McSourceSet>.parseHierarchy(
  sourceSetName: SourceSetName,
  configurations: List<McConfiguration>
): Pair<Lazy<List<SourceSetName>>, Lazy<List<SourceSetName>>> {
  // Get the up/downstream configurations for this source set.  Parse the SourceSetName out of the
  // ConfigurationName, and **if that name is in this map**, that SourceSetName must be
  // up/downstream of this particular source set.

  val upstreamLazy = lazy {
    configurations
      .flatMapToSet { it.upstream.map { upstreamConfig -> upstreamConfig.name.toSourceSetName() } }
      .filterNot { it == sourceSetName }
      .filter { this@parseHierarchy.contains(it) }
      .distinct()
  }

  val downstreamLazy = lazy {
    configurations
      .flatMapToSet { it.downstream.map { downstreamConfig -> downstreamConfig.name.toSourceSetName() } }
      .filterNot { it == sourceSetName }
      .filter { this@parseHierarchy.contains(it) }
      .distinct()
  }

  return upstreamLazy to downstreamLazy
}

private fun GradleSourceSet.classpath() = compileClasspath.existingFiles()
  .plus(output.classesDirs.existingFiles())
  .toSet()
