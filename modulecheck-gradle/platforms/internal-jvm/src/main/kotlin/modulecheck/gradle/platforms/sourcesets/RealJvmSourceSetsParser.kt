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

package modulecheck.gradle.platforms.sourcesets

import com.squareup.anvil.annotations.ContributesBinding
import modulecheck.dagger.AppScope
import modulecheck.gradle.platforms.getKotlinExtensionOrNull
import modulecheck.parsing.gradle.model.Config
import modulecheck.parsing.gradle.model.Configurations
import modulecheck.parsing.gradle.model.SourceSet
import modulecheck.parsing.gradle.model.SourceSetName
import modulecheck.parsing.gradle.model.SourceSets
import modulecheck.parsing.gradle.model.asConfigurationName
import modulecheck.parsing.gradle.model.asSourceSetName
import modulecheck.utils.flatMapToSet
import org.gradle.api.plugins.JavaPluginExtension
import org.jetbrains.kotlin.gradle.plugin.KotlinSourceSet
import javax.inject.Inject
import org.gradle.api.Project as GradleProject

@ContributesBinding(AppScope::class)
class RealJvmSourceSetsParser @Inject constructor() : JvmSourceSetsParser {

  override fun parse(
    parsedConfigurations: Configurations,
    gradleProject: GradleProject
  ): SourceSets {

    val map = buildMap<SourceSetName, SourceSet> {

      val kotlinExtensionOrNull = gradleProject.getKotlinExtensionOrNull()

      val jvmTarget = gradleProject.jvmTarget()

      if (kotlinExtensionOrNull != null) {

        kotlinExtensionOrNull.sourceSets
          .forEach { kotlinSourceSet: KotlinSourceSet ->

            val sourceSetName = kotlinSourceSet.name.asSourceSetName()

            val configs = listOf(
              kotlinSourceSet.compileOnlyConfigurationName,
              kotlinSourceSet.apiConfigurationName,
              kotlinSourceSet.implementationConfigurationName,
              kotlinSourceSet.runtimeOnlyConfigurationName
            ).mapNotNull { parsedConfigurations[it.asConfigurationName()] }

            val (
              upstreamLazy,
              downstreamLazy
            ) = parseHierarchy(sourceSetName, configs)

            put(
              kotlinSourceSet.name.asSourceSetName(),
              SourceSet(
                name = sourceSetName,
                compileOnlyConfiguration = parsedConfigurations
                  .getValue(kotlinSourceSet.compileOnlyConfigurationName.asConfigurationName()),
                apiConfiguration = parsedConfigurations
                  .get(kotlinSourceSet.apiConfigurationName.asConfigurationName()),
                implementationConfiguration = parsedConfigurations
                  .getValue(kotlinSourceSet.implementationConfigurationName.asConfigurationName()),
                runtimeOnlyConfiguration = parsedConfigurations
                  .getValue(kotlinSourceSet.runtimeOnlyConfigurationName.asConfigurationName()),
                annotationProcessorConfiguration = null,
                jvmFiles = kotlinSourceSet.kotlin.srcDirs,
                resourceFiles = kotlinSourceSet.resources.sourceDirectories.files,
                layoutFiles = emptySet(),
                jvmTarget = jvmTarget,
                upstreamLazy = upstreamLazy,
                downstreamLazy = downstreamLazy
              )
            )
          }
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

            put(
              gradleSourceSet.name.asSourceSetName(),
              SourceSet(
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
                jvmFiles = gradleSourceSet.allJava.srcDirs,
                resourceFiles = gradleSourceSet.resources.sourceDirectories.files,
                layoutFiles = emptySet(),
                jvmTarget = jvmTarget,
                upstreamLazy = upstreamLazy,
                downstreamLazy = downstreamLazy
              )
            )
          }
      }
    }

    return SourceSets(map)
  }

  private fun MutableMap<SourceSetName, SourceSet>.parseHierarchy(
    sourceSetName: SourceSetName,
    configs: List<Config>
  ): Pair<Lazy<List<SourceSetName>>, Lazy<List<SourceSetName>>> {

    // Get the up/downstream configurations for this source set.  Parse the SourceSetName out of the
    // ConfigurationName, and **if that name is in this map**, that SourceSetName must be
    // up/downstream of this particular source set.

    val upstreamLazy = lazy {
      configs
        .flatMapToSet { it.upstream.map { upstreamConfig -> upstreamConfig.name.toSourceSetName() } }
        .filterNot { it == sourceSetName }
        .filter { this@parseHierarchy.contains(it) }
        .distinct()
    }

    val downstreamLazy = lazy {
      configs
        .flatMapToSet { it.downstream.map { downstreamConfig -> downstreamConfig.name.toSourceSetName() } }
        .filterNot { it == sourceSetName }
        .filter { this@parseHierarchy.contains(it) }
        .distinct()
    }

    return upstreamLazy to downstreamLazy
  }
}
