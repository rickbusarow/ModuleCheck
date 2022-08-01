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

package modulecheck.gradle.platforms.sourcesets

import com.squareup.anvil.annotations.ContributesBinding
import modulecheck.dagger.TaskScope
import modulecheck.gradle.platforms.KotlinEnvironmentFactory
import modulecheck.gradle.platforms.getKotlinExtensionOrNull
import modulecheck.gradle.platforms.toSourceSets
import modulecheck.model.dependency.Configurations
import modulecheck.model.dependency.McConfiguration
import modulecheck.model.dependency.McSourceSet
import modulecheck.model.dependency.ProjectPath.StringProjectPath
import modulecheck.model.dependency.SourceSets
import modulecheck.model.dependency.asConfigurationName
import modulecheck.model.sourceset.SourceSetName
import modulecheck.model.sourceset.asSourceSetName
import modulecheck.parsing.gradle.model.GradleProject
import modulecheck.parsing.gradle.model.GradleSourceSet
import modulecheck.utils.flatMapToSet
import modulecheck.utils.lazy.lazyDeferred
import org.gradle.api.plugins.JavaPluginExtension
import javax.inject.Inject

@ContributesBinding(TaskScope::class)
class RealJvmSourceSetsParser @Inject constructor(
  private val kotlinEnvironmentFactory: KotlinEnvironmentFactory
) : JvmSourceSetsParser {

  override fun parse(
    parsedConfigurations: Configurations,
    gradleProject: GradleProject
  ): SourceSets {
    val map = buildMap<SourceSetName, McSourceSet> {
      val kotlinExtensionOrNull = gradleProject.getKotlinExtensionOrNull()

      val javaExtension = gradleProject.extensions
        .findByType(JavaPluginExtension::class.java)

      val jvmTarget = gradleProject.jvmTarget()

      val projectPath = StringProjectPath(gradleProject.path)

      if (kotlinExtensionOrNull != null) {

        putAll(
          kotlinExtensionOrNull.toSourceSets(
            kotlinEnvironmentFactory = kotlinEnvironmentFactory,
            parsedConfigurations = parsedConfigurations,
            javaExtension = javaExtension,
            projectPath = projectPath,
            jvmTarget = jvmTarget,
            kotlinVersion = gradleProject.kotlinLanguageVersionOrNull()
          )
        )
      } else {
        gradleProject.extensions
          .findByType(JavaPluginExtension::class.java)
          ?.sourceSets
          ?.forEach { gradleSourceSet ->

            val sourceSetName = gradleSourceSet.name.asSourceSetName()

            val configs = listOf(
              gradleSourceSet.compileOnlyConfigurationName,
              gradleSourceSet.apiConfigurationName,
              gradleSourceSet.implementationConfigurationName,
              gradleSourceSet.runtimeOnlyConfigurationName
            ).mapNotNull { parsedConfigurations[it.asConfigurationName()] }

            val (
              upstreamLazy,
              downstreamLazy
            ) = parseHierarchy(sourceSetName, configs)

            val jvmFiles = gradleSourceSet.allJava.srcDirs

            val kotlinEnvironmentDeferred = lazyDeferred {
              kotlinEnvironmentFactory.create(
                projectPath = projectPath,
                sourceSetName = sourceSetName,
                classpathFiles = lazyDeferred { gradleSourceSet.classpath() },
                sourceDirs = jvmFiles,
                kotlinLanguageVersion = null,
                jvmTarget = jvmTarget
              )
            }

            put(
              gradleSourceSet.name.asSourceSetName(),
              McSourceSet(
                name = sourceSetName,
                compileOnlyConfiguration = parsedConfigurations
                  .getValue(gradleSourceSet.compileOnlyConfigurationName.asConfigurationName()),
                apiConfiguration = parsedConfigurations
                  .get(gradleSourceSet.apiConfigurationName.asConfigurationName()),
                implementationConfiguration = parsedConfigurations
                  .getValue(gradleSourceSet.implementationConfigurationName.asConfigurationName()),
                runtimeOnlyConfiguration = parsedConfigurations
                  .getValue(gradleSourceSet.runtimeOnlyConfigurationName.asConfigurationName()),
                annotationProcessorConfiguration = null,
                jvmFiles = jvmFiles,
                resourceFiles = gradleSourceSet.resources.sourceDirectories.files,
                layoutFiles = emptySet(),
                jvmTarget = jvmTarget,
                kotlinEnvironmentDeferred = kotlinEnvironmentDeferred,
                upstreamLazy = upstreamLazy,
                downstreamLazy = downstreamLazy
              )
            )
          }
      }
    }

    return SourceSets(map)
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
    .toList()
}
