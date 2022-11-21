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

package modulecheck.project.generation

import modulecheck.config.CodeGeneratorBinding
import modulecheck.model.dependency.ConfigurationName
import modulecheck.model.dependency.Configurations
import modulecheck.model.dependency.PlatformPlugin
import modulecheck.model.dependency.ProjectDependency
import modulecheck.model.dependency.ProjectPath.StringProjectPath
import modulecheck.model.dependency.SourceSets
import modulecheck.model.sourceset.SourceSetName
import modulecheck.parsing.gradle.dsl.BuildFileParser
import modulecheck.parsing.gradle.dsl.internal.RealBuildFileParser
import modulecheck.parsing.groovy.antlr.GroovyAndroidGradleParser
import modulecheck.parsing.groovy.antlr.GroovyDependenciesBlockParser
import modulecheck.parsing.groovy.antlr.GroovyPluginsBlockParser
import modulecheck.parsing.kotlin.compiler.NoContextPsiFileFactory
import modulecheck.parsing.kotlin.compiler.impl.DependencyModuleDescriptorAccess
import modulecheck.parsing.psi.KotlinAndroidGradleParser
import modulecheck.parsing.psi.KotlinDependenciesBlockParser
import modulecheck.parsing.psi.KotlinPluginsBlockParser
import modulecheck.parsing.wiring.JvmFileCache
import modulecheck.parsing.wiring.RealAndroidGradleSettingsProvider
import modulecheck.parsing.wiring.RealDependenciesBlocksProvider
import modulecheck.parsing.wiring.RealJvmFileProvider
import modulecheck.parsing.wiring.RealPluginsBlockProvider
import modulecheck.project.JvmFileProvider
import modulecheck.project.McProject
import modulecheck.project.ProjectCache
import modulecheck.project.ProjectProvider
import modulecheck.project.impl.RealMcProject
import modulecheck.reporting.logging.McLogger
import modulecheck.reporting.logging.PrintLogger
import modulecheck.utils.child
import modulecheck.utils.createSafely
import java.io.File

@PublishedApi
@Suppress("LongParameterList")
internal inline fun <reified T : PlatformPluginBuilder<R>, R : PlatformPlugin> createProject(
  projectCache: ProjectCache,
  dependencyModuleDescriptorAccess: DependencyModuleDescriptorAccess,
  projectDir: File,
  path: String,
  pluginBuilder: T,
  androidPackageOrNull: String?,
  codeGeneratorBindings: List<CodeGeneratorBinding>,
  projectProvider: ProjectProvider,
  config: McProjectBuilder<T>.() -> Unit
): McProject {

  val projectRoot = File(projectDir, path.replace(":", File.separator))
    .also { it.mkdirs() }

  val buildFile = File(projectRoot, "build.gradle.kts")
    .createSafely()

  val builder = McProjectBuilder(
    path = StringProjectPath(path),
    projectDir = projectRoot,
    buildFile = buildFile,
    platformPlugin = pluginBuilder,
    projectCache = projectCache,
    codeGeneratorBindings = codeGeneratorBindings,
    projectProvider = projectProvider,
    dependencyModuleDescriptorAccess = dependencyModuleDescriptorAccess
  )
    .also {
      it.maybeAddSourceSet(SourceSetName.MAIN)

      val testUpstream = mutableListOf(SourceSetName.MAIN)

      if (pluginBuilder is AndroidPlatformPluginBuilder<*>) {
        it.maybeAddSourceSet(SourceSetName.DEBUG, upstreamNames = listOf(SourceSetName.MAIN))
        it.maybeAddSourceSet(SourceSetName.RELEASE, upstreamNames = listOf(SourceSetName.MAIN))
        it.maybeAddSourceSet(
          SourceSetName.ANDROID_TEST,
          upstreamNames = listOf(SourceSetName.MAIN, SourceSetName.DEBUG)
        )

        testUpstream.add(SourceSetName.DEBUG)
      }

      it.maybeAddSourceSet(SourceSetName.TEST, upstreamNames = testUpstream)

      it.config()

      pluginBuilder.sourceSets.validateHierarchy()

      if (pluginBuilder is AndroidPlatformPluginBuilder<*>) {

        requireNotNull(androidPackageOrNull)

        pluginBuilder.manifests.getOrPut(SourceSetName.MAIN) {
          projectRoot.child("src/main/AndroidManifest.xml")
            .createSafely("<manifest package=\"$androidPackageOrNull\" />")
        }
      }
    }

  return builder.buildProject {
    toRealMcProject()
  }
}

@PublishedApi
internal suspend fun SourceSets.toBuilderMap(): MutableMap<SourceSetName, SourceSetBuilder> =
  mapValuesTo(mutableMapOf()) { (_, sourceSet) ->
    SourceSetBuilder.fromSourceSet(sourceSet)
  }

@PublishedApi
internal fun Configurations.toBuilderMap(): MutableMap<ConfigurationName, ConfigBuilder> =
  mapValuesTo(mutableMapOf()) { (_, config) ->
    ConfigBuilder.fromConfig(config)
  }

fun buildFileParserFactory(
  projectDependencyFactory: ProjectDependency.Factory,
  logger: McLogger = PrintLogger()
): BuildFileParser.Factory {
  return BuildFileParser.Factory { invokesConfigurationNames ->

    RealBuildFileParser(
      {
        RealDependenciesBlocksProvider(
          groovyParser = GroovyDependenciesBlockParser(logger, projectDependencyFactory),
          kotlinParser = KotlinDependenciesBlockParser(
            logger,
            NoContextPsiFileFactory(),
            projectDependencyFactory
          ),
          invokesConfigurationNames = invokesConfigurationNames
        )
      },
      {
        RealPluginsBlockProvider(
          groovyParser = GroovyPluginsBlockParser(logger),
          kotlinParser = KotlinPluginsBlockParser(logger),
          buildFile = invokesConfigurationNames.buildFile,
          NoContextPsiFileFactory()
        )
      },
      {
        RealAndroidGradleSettingsProvider(
          groovyParser = GroovyAndroidGradleParser(),
          kotlinParser = KotlinAndroidGradleParser(NoContextPsiFileFactory()),
          buildFile = invokesConfigurationNames.buildFile
        )
      },
      invokesConfigurationNames
    )
  }
}

@PublishedApi
internal inline fun <reified T, reified P, G> T.buildProject(
  projectFactory: T.(JvmFileProvider.Factory) -> McProject
): McProject
  where T : McProjectBuilder<P>,
        P : PlatformPluginBuilder<G>,
        G : PlatformPlugin {

  populateSourceSets()
  platformPlugin.populateConfigsFromSourceSets()
  platformPlugin.sourceSets.populateDownstreams()

  executeDuringBuildActions()

  val jvmFileProviderFactory = RealJvmFileProvider.Factory { JvmFileCache() }

  return projectFactory(jvmFileProviderFactory)
    .also { finalProject ->
      projectCache[finalProject.projectPath] = finalProject
    }
}

inline fun <reified T, reified P, G> T.toRealMcProject(): McProject
  where T : McProjectBuilder<P>,
        P : PlatformPluginBuilder<G>,
        G : PlatformPlugin {
  return buildProject { jvmFileProviderFactory ->

    RealMcProject(
      projectPath = path,
      projectDir = projectDir,
      buildFile = buildFile,
      hasKapt = hasKapt,
      hasTestFixturesPlugin = hasTestFixturesPlugin,
      projectCache = projectCache,
      anvilGradlePlugin = anvilGradlePlugin,
      logger = PrintLogger(),
      jvmFileProviderFactory = jvmFileProviderFactory,
      jvmTarget = jvmTarget,
      buildFileParserFactory = buildFileParserFactory(configuredProjectDependencyFactory),
      platformPlugin = platformPlugin.toPlugin(
        dependencyModuleDescriptorAccess = dependencyModuleDescriptorAccess,
        projectPath = path,
        projectDependencies = projectDependencies,
        externalDependencies = externalDependencies
      )
    )
  }
}
