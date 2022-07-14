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

package modulecheck.project.test

import modulecheck.config.CodeGeneratorBinding
import modulecheck.model.dependency.ProjectDependency
import modulecheck.parsing.gradle.dsl.BuildFileParser
import modulecheck.parsing.gradle.dsl.internal.RealBuildFileParser
import modulecheck.parsing.gradle.model.Configurations
import modulecheck.parsing.gradle.model.PlatformPlugin
import modulecheck.parsing.gradle.model.ProjectPath.StringProjectPath
import modulecheck.parsing.gradle.model.SourceSetName
import modulecheck.parsing.gradle.model.SourceSets
import modulecheck.parsing.groovy.antlr.GroovyAndroidGradleParser
import modulecheck.parsing.groovy.antlr.GroovyDependenciesBlockParser
import modulecheck.parsing.groovy.antlr.GroovyPluginsBlockParser
import modulecheck.parsing.kotlin.compiler.NoContextPsiFileFactory
import modulecheck.parsing.kotlin.compiler.impl.SafeAnalysisResultAccess
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
  safeAnalysisResultAccess: SafeAnalysisResultAccess,
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
    safeAnalysisResultAccess = safeAnalysisResultAccess
  )
    .also {
      it.maybeAddSourceSet(SourceSetName.MAIN)
      it.maybeAddSourceSet(SourceSetName.TEST)

      if (pluginBuilder is AndroidPlatformPluginBuilder<*>) {
        it.maybeAddSourceSet(SourceSetName.DEBUG)
        it.maybeAddSourceSet(SourceSetName.RELEASE)
        it.maybeAddSourceSet(SourceSetName.ANDROID_TEST)
      }

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
internal inline fun <reified T : McProjectBuilder<P>,
  reified P : PlatformPluginBuilder<G>,
  G : PlatformPlugin> T.buildProject(
  projectFactory: T.(JvmFileProvider.Factory) -> McProject
): McProject {

  populateSourceSets()
  platformPlugin.populateConfigsFromSourceSets()
  platformPlugin.sourceSets.populateDownstreams()

  val jvmFileProviderFactory = RealJvmFileProvider.Factory { JvmFileCache() }

  return projectFactory(jvmFileProviderFactory)
    .also { finalProject ->
      projectCache[finalProject.path] = finalProject
    }
}

inline fun <reified T : McProjectBuilder<P>,
  reified P : PlatformPluginBuilder<G>,
  G : PlatformPlugin> T.toRealMcProject(): McProject {
  return buildProject { jvmFileProviderFactory ->

    RealMcProject(
      path = path,
      projectDir = projectDir,
      buildFile = buildFile,
      hasKapt = hasKapt,
      hasTestFixturesPlugin = hasTestFixturesPlugin,
      projectCache = projectCache,
      anvilGradlePlugin = anvilGradlePlugin,
      logger = PrintLogger(),
      jvmFileProviderFactory = jvmFileProviderFactory,
      jvmTarget = jvmTarget,
      projectDependencies = lazy { projectDependencies },
      externalDependencies = lazy { externalDependencies },
      buildFileParserFactory = buildFileParserFactory(configuredProjectDependency),
      platformPlugin = platformPlugin.toPlugin(safeAnalysisResultAccess, projectPath = path)
    )
  }
}

@PublishedApi
internal suspend fun SourceSets.toBuilderMap() = mapValuesTo(mutableMapOf()) { (_, sourceSet) ->
  SourceSetBuilder.fromSourceSet(sourceSet)
}

@PublishedApi
internal fun Configurations.toBuilderMap() = mapValuesTo(mutableMapOf()) { (_, config) ->
  ConfigBuilder.fromConfig(config)
}

fun buildFileParserFactory(
  projectDependency: ProjectDependency.Factory,
  logger: McLogger = PrintLogger()
): BuildFileParser.Factory {
  return BuildFileParser.Factory { invokesConfigurationNames ->

    RealBuildFileParser(
      {
        RealDependenciesBlocksProvider(
          groovyParser = GroovyDependenciesBlockParser(logger, projectDependency),
          kotlinParser = KotlinDependenciesBlockParser(
            logger,
            NoContextPsiFileFactory(),
            projectDependency
          ),
          invokesConfigurationNames = invokesConfigurationNames
        )
      },
      {
        RealPluginsBlockProvider(
          groovyParser = GroovyPluginsBlockParser(logger),
          kotlinParser = KotlinPluginsBlockParser(logger),
          buildFile = invokesConfigurationNames.buildFile, NoContextPsiFileFactory()
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
