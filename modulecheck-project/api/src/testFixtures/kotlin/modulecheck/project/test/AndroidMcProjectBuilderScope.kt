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

import modulecheck.parsing.gradle.Config
import modulecheck.parsing.gradle.ConfigurationName
import modulecheck.parsing.gradle.Configurations
import modulecheck.parsing.gradle.ProjectPath.StringProjectPath
import modulecheck.parsing.gradle.SourceSet
import modulecheck.parsing.gradle.SourceSetName
import modulecheck.parsing.gradle.SourceSets
import modulecheck.parsing.groovy.antlr.GroovyAndroidGradleParser
import modulecheck.parsing.groovy.antlr.GroovyDependencyBlockParser
import modulecheck.parsing.groovy.antlr.GroovyPluginsBlockParser
import modulecheck.parsing.psi.KotlinAndroidGradleParser
import modulecheck.parsing.psi.KotlinDependencyBlockParser
import modulecheck.parsing.psi.KotlinPluginsBlockParser
import modulecheck.parsing.source.AnvilGradlePlugin
import modulecheck.parsing.source.JavaVersion
import modulecheck.parsing.wiring.RealAndroidGradleSettingsProvider
import modulecheck.parsing.wiring.RealDependenciesBlocksProvider
import modulecheck.parsing.wiring.RealPluginsBlockProvider
import modulecheck.project.BuildFileParser
import modulecheck.project.ExternalDependencies
import modulecheck.project.McProject
import modulecheck.project.PrintLogger
import modulecheck.project.ProjectCache
import modulecheck.project.ProjectDependencies
import modulecheck.project.impl.RealAndroidMcProject
import modulecheck.testing.createSafely
import modulecheck.utils.child
import org.intellij.lang.annotations.Language
import java.io.File

interface AndroidMcProjectBuilderScope : McProjectBuilderScope {
  var androidResourcesEnabled: Boolean
  var viewBindingEnabled: Boolean
  var kotlinAndroidExtensionEnabled: Boolean
  val manifests: MutableMap<SourceSetName, File>

  fun addResourceFile(
    name: String,
    @Language("xml")
    content: String,
    sourceSetName: SourceSetName = SourceSetName.MAIN
  ) {

    require(!name.startsWith("layout/")) { "use `addLayoutFile` for layout files." }

    val file = File(projectDir, "src/${sourceSetName.value}/res/$name").createSafely(content)

    val old = maybeAddSourceSet(sourceSetName)

    sourceSets[sourceSetName] = old.copy(resourceFiles = old.resourceFiles + file)
  }

  fun addLayoutFile(
    name: String,
    @Language("xml")
    content: String,
    sourceSetName: SourceSetName = SourceSetName.MAIN
  ) {
    val file = File(projectDir, "src/${sourceSetName.value}/res/layout/$name")
      .createSafely(content)

    val old = maybeAddSourceSet(sourceSetName)

    sourceSets[sourceSetName] = old.copy(layoutFiles = old.layoutFiles + file)
  }

  fun addManifest(
    @Language("xml")
    content: String,
    sourceSetName: SourceSetName = SourceSetName.MAIN
  ) {
    val file = File(projectDir, "src/${sourceSetName.value}/AndroidManifest.xml")
      .createSafely(content)

    manifests[sourceSetName] = file
  }
}

data class RealAndroidMcProjectBuilderScope(
  override var path: StringProjectPath,
  override var projectDir: File,
  override var buildFile: File,
  override var androidResourcesEnabled: Boolean = true,
  override var viewBindingEnabled: Boolean = true,
  override var kotlinAndroidExtensionEnabled: Boolean = true,
  override val manifests: MutableMap<SourceSetName, File> = mutableMapOf(),
  override val configurations: MutableMap<ConfigurationName, Config> = mutableMapOf(),
  override val projectDependencies: ProjectDependencies = ProjectDependencies(mutableMapOf()),
  override val externalDependencies: ExternalDependencies = ExternalDependencies(mutableMapOf()),
  override var hasKapt: Boolean = false,
  override var hasTestFixturesPlugin: Boolean = false,
  override val sourceSets: MutableMap<SourceSetName, SourceSet> = mutableMapOf(),
  override var anvilGradlePlugin: AnvilGradlePlugin? = null,
  override val projectCache: ProjectCache = ProjectCache(),
  override val javaSourceVersion: JavaVersion = JavaVersion.VERSION_14
) : AndroidMcProjectBuilderScope

internal fun createAndroidProject(
  projectCache: ProjectCache,
  projectDir: File,
  path: String,
  androidPackage: String,
  config: AndroidMcProjectBuilderScope.() -> Unit
): McProject {

  val projectRoot = File(projectDir, path.replace(":", File.separator))
    .also { it.mkdirs() }

  val buildFile = File(projectRoot, "build.gradle.kts")
    .also { it.createNewFile() }

  val builder = RealAndroidMcProjectBuilderScope(
    path = StringProjectPath(path),
    projectDir = projectRoot,
    buildFile = buildFile,
    projectCache = projectCache
  )
    .also {
      it.maybeAddSourceSet(SourceSetName.MAIN)
      it.maybeAddSourceSet(SourceSetName.DEBUG)
      it.maybeAddSourceSet(SourceSetName.RELEASE)
      it.maybeAddSourceSet(SourceSetName.ANDROID_TEST)
      it.maybeAddSourceSet(SourceSetName.TEST)

      it.config()
    }

  builder.manifests.getOrPut(SourceSetName.MAIN) {
    projectRoot.child("src/main/AndroidManifest.xml")
      .createSafely("<manifest package=\"$androidPackage\" />")
  }

  return builder.toProject()
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

fun AndroidMcProjectBuilderScope.toProject(): RealAndroidMcProject {
  return toProject { jvmFileProviderFactory ->
    RealAndroidMcProject(
      path = path,
      projectDir = projectDir,
      buildFile = buildFile,
      configurations = Configurations(configurations),
      hasKapt = hasKapt,
      hasTestFixturesPlugin = hasTestFixturesPlugin,
      sourceSets = SourceSets(sourceSets),
      projectCache = projectCache,
      anvilGradlePlugin = anvilGradlePlugin,
      androidResourcesEnabled = androidResourcesEnabled,
      viewBindingEnabled = viewBindingEnabled,
      kotlinAndroidExtensionEnabled = kotlinAndroidExtensionEnabled,
      manifests = manifests,
      logger = PrintLogger(),
      jvmFileProviderFactory = jvmFileProviderFactory,
      javaSourceVersion = javaSourceVersion,
      projectDependencies = lazy { projectDependencies },
      externalDependencies = lazy { externalDependencies },
      buildFileParserFactory = buildFileParserFactory()
    )
  }
}
