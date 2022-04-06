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

import modulecheck.parsing.gradle.BuildFileParser
import modulecheck.parsing.gradle.Configurations
import modulecheck.parsing.gradle.PlatformPlugin
import modulecheck.parsing.gradle.ProjectPath.StringProjectPath
import modulecheck.parsing.gradle.SourceSetName
import modulecheck.parsing.gradle.SourceSets
import modulecheck.parsing.groovy.antlr.GroovyAndroidGradleParser
import modulecheck.parsing.groovy.antlr.GroovyDependencyBlockParser
import modulecheck.parsing.groovy.antlr.GroovyPluginsBlockParser
import modulecheck.parsing.psi.KotlinAndroidGradleParser
import modulecheck.parsing.psi.KotlinDependencyBlockParser
import modulecheck.parsing.psi.KotlinPluginsBlockParser
import modulecheck.parsing.wiring.FileCache
import modulecheck.parsing.wiring.RealAndroidGradleSettingsProvider
import modulecheck.parsing.wiring.RealAndroidRNameProvider
import modulecheck.parsing.wiring.RealDependenciesBlocksProvider
import modulecheck.parsing.wiring.RealJvmFileProvider
import modulecheck.parsing.wiring.RealPluginsBlockProvider
import modulecheck.project.JvmFileProvider
import modulecheck.project.McProject
import modulecheck.project.PrintLogger
import modulecheck.project.ProjectCache
import modulecheck.project.impl.RealMcProject
import modulecheck.testing.createSafely
import modulecheck.utils.child
import java.io.File

@PublishedApi
@Suppress("LongParameterList")
internal inline fun <reified T : PlatformPluginBuilder<R>, R : PlatformPlugin> createProject(
  projectCache: ProjectCache,
  projectDir: File,
  path: String,
  pluginBuilder: T,
  androidPackageOrNull: String?,
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
    projectCache = projectCache
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

  val jvmFileProviderFactory = JvmFileProvider.Factory { project, sourceSetName ->
    RealJvmFileProvider(
      fileCache = FileCache(),
      project = project,
      sourceSetName = sourceSetName,
      androidRNameProvider = RealAndroidRNameProvider(project, sourceSetName)
    )
  }

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
      javaSourceVersion = javaSourceVersion,
      projectDependencies = lazy { projectDependencies },
      externalDependencies = lazy { externalDependencies },
      buildFileParserFactory = buildFileParserFactory(),
      platformPlugin = platformPlugin.toPlugin()
    )
  }
}

@PublishedApi
internal fun SourceSets.toBuilderMap() = mapValuesTo(mutableMapOf()) { (_, sourceSet) ->
  SourceSetBuilder.fromSourceSet(sourceSet)
}

@PublishedApi
internal fun Configurations.toBuilderMap() = mapValuesTo(mutableMapOf()) { (_, config) ->
  ConfigBuilder.fromConfig(config)
}

fun buildFileParserFactory(): BuildFileParser.Factory {

  return BuildFileParser.Factory { invokesConfigurationNames ->

    BuildFileParser(
      {
        RealDependenciesBlocksProvider(
          groovyParser = GroovyDependencyBlockParser(),
          kotlinParser = KotlinDependencyBlockParser(),
          invokesConfigurationNames = invokesConfigurationNames
        )
      },
      {
        RealPluginsBlockProvider(
          groovyParser = GroovyPluginsBlockParser(),
          kotlinParser = KotlinPluginsBlockParser(),
          buildFile = invokesConfigurationNames.buildFile
        )
      },
      {
        RealAndroidGradleSettingsProvider(
          groovyParser = GroovyAndroidGradleParser(),
          kotlinParser = KotlinAndroidGradleParser(),
          buildFile = invokesConfigurationNames.buildFile
        )
      },
      invokesConfigurationNames
    )
  }
}
