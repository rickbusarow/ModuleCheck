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
import modulecheck.parsing.gradle.ConfigFactory
import modulecheck.parsing.gradle.ConfigurationName
import modulecheck.parsing.gradle.Configurations
import modulecheck.parsing.gradle.MavenCoordinates
import modulecheck.parsing.gradle.SourceSet
import modulecheck.parsing.gradle.SourceSetName
import modulecheck.parsing.gradle.SourceSets
import modulecheck.parsing.gradle.asConfigurationName
import modulecheck.parsing.source.AnvilGradlePlugin
import modulecheck.parsing.source.JavaVersion
import modulecheck.parsing.wiring.FileCache
import modulecheck.parsing.wiring.RealJvmFileProvider
import modulecheck.project.ConfiguredProjectDependency
import modulecheck.project.ExternalDependencies
import modulecheck.project.ExternalDependency
import modulecheck.project.JvmFileProvider
import modulecheck.project.McProject
import modulecheck.project.PrintLogger
import modulecheck.project.ProjectCache
import modulecheck.project.ProjectDependencies
import modulecheck.project.impl.RealMcProject
import modulecheck.testing.createSafely
import modulecheck.testing.requireNotNullOrFail
import modulecheck.utils.requireNotNull
import org.intellij.lang.annotations.Language
import java.io.File

interface McProjectBuilderScope {
  var path: String
  var projectDir: File
  var buildFile: File
  val configurations: MutableMap<ConfigurationName, Config>
  val projectDependencies: ProjectDependencies
  val externalDependencies: ExternalDependencies
  var hasKapt: Boolean
  var hasTestFixturesPlugin: Boolean
  val sourceSets: MutableMap<SourceSetName, SourceSet>
  var anvilGradlePlugin: AnvilGradlePlugin?
  val projectCache: ProjectCache
  val javaSourceVersion: JavaVersion

  fun addDependency(
    configurationName: ConfigurationName,
    project: McProject,
    asTestFixture: Boolean = false
  ) {

    configurationName.maybeAddToSourceSetsAndConfigurations()

    val old = projectDependencies[configurationName].orEmpty()

    val cpd = ConfiguredProjectDependency(configurationName, project, asTestFixture)

    projectDependencies[configurationName] = old + cpd
  }

  fun addExternalDependency(
    configurationName: ConfigurationName,
    coordinates: String,
    asTestFixture: Boolean = false
  ) {

    val maven = MavenCoordinates.parseOrNull(coordinates)
      .requireNotNull {
        "The external coordinate string `$coordinates` must match the Maven coordinate pattern."
      }

    configurationName.maybeAddToSourceSetsAndConfigurations()

    val old = externalDependencies[configurationName].orEmpty()

    val external = ExternalDependency(
      configurationName = configurationName,
      group = maven.group,
      moduleName = maven.moduleName,
      version = maven.version
    )

    externalDependencies[configurationName] = old + external
  }

  private fun ConfigurationName.maybeAddToSourceSetsAndConfigurations() {
    val sourceSetName = toSourceSetName()
    maybeAddSourceSet(sourceSetName)
    // If the configuration is not from Java plugin, then it won't be automatically added from
    // source sets.  Plugins like Kapt don't make their configs inherit from each other,
    // so just add an empty sequence for up/downstream.
    if (this !in sourceSetName.javaConfigurationNames()) {
      configurations[this] = configFactory.create(value)
    }
  }

  fun addSource(
    name: String,
    @Language("kotlin")
    kotlin: String,
    sourceSetName: SourceSetName = SourceSetName.MAIN
  ) {

    val file = File(projectDir, "src/${sourceSetName.value}/$name")
      .createSafely(kotlin.trimIndent())

    val old = maybeAddSourceSet(sourceSetName)

    sourceSets[sourceSetName] = old.copy(jvmFiles = old.jvmFiles + file)
  }

  @Suppress("LongParameterList")
  fun addSourceSet(
    name: SourceSetName,
    jvmFiles: Set<File> = emptySet(),
    resourceFiles: Set<File> = emptySet(),
    layoutFiles: Set<File> = emptySet(),
    upstreamNames: List<SourceSetName> = emptyList(),
    downstreamNames: List<SourceSetName> = emptyList()
  ): SourceSet {

    val old = sourceSets[name]

    require(old == null) {
      "A source set for the name '${name.value}' already exists.  " +
        "You can probably just delete this line?"
    }

    return maybeAddSourceSet(
      name = name,
      jvmFiles = jvmFiles,
      resourceFiles = resourceFiles,
      layoutFiles = layoutFiles,
      upstreamNames = upstreamNames,
      downstreamNames = downstreamNames
    )
  }

  operator fun File.invoke(text: () -> String) {
    writeText(text().trimIndent())
  }

  fun requireSourceSetExists(name: SourceSetName) {
    sourceSets[name]
      .requireNotNullOrFail {
        """
        The source set named `${name.value}` doesn't exist in the `sourceSets` map.

          missing source set name: ${name.value}
          existing source sets: ${sourceSets.keys.map { it.value }}
        """.trimIndent()
      }
  }
}

@Suppress("LongParameterList")
data class JvmMcProjectBuilderScope(
  override var path: String,
  override var projectDir: File,
  override var buildFile: File,
  override val configurations: MutableMap<ConfigurationName, Config> = mutableMapOf(),
  override val projectDependencies: ProjectDependencies = ProjectDependencies(mutableMapOf()),
  override val externalDependencies: ExternalDependencies = ExternalDependencies(mutableMapOf()),
  override var hasKapt: Boolean = false,
  override var hasTestFixturesPlugin: Boolean = false,
  override val sourceSets: MutableMap<SourceSetName, SourceSet> = mutableMapOf(),
  override var anvilGradlePlugin: AnvilGradlePlugin? = null,
  override val projectCache: ProjectCache = ProjectCache(),
  override val javaSourceVersion: JavaVersion = JavaVersion.VERSION_14
) : McProjectBuilderScope

internal fun createProject(
  projectCache: ProjectCache,
  projectDir: File,
  path: String,
  config: McProjectBuilderScope.() -> Unit
): McProject {

  val projectRoot = File(projectDir, path.replace(":", File.separator))
    .also { it.mkdirs() }

  val buildFile = File(projectRoot, "build.gradle.kts")
    .createSafely()

  val builder = JvmMcProjectBuilderScope(
    path = path,
    projectDir = projectRoot,
    buildFile = buildFile,
    projectCache = projectCache
  )
    .also {
      it.maybeAddSourceSet(SourceSetName.MAIN)
      it.maybeAddSourceSet(SourceSetName.TEST)

      it.config()
    }

  return builder.toProject()
}

internal fun McProjectBuilderScope.populateConfigsFromSourceSets() {
  sourceSets
    .keys
    // add main source set configs first so that they can be safely looked up for inheriting configs
    .sortedByDescending { it == SourceSetName.MAIN }
    .flatMap { it.javaConfigurationNames() }
    .forEach { configurationName ->

      val upstream = if (configurationName.toSourceSetName() == SourceSetName.MAIN) {
        emptySequence()
      } else {
        SourceSetName.MAIN.javaConfigurationNames()
          .map { configurations.getValue(it) }
          .asSequence()
      }

      val downstreamNames = if (configurationName.toSourceSetName() != SourceSetName.MAIN) {
        emptySequence()
      } else if (configurationName.isImplementation()) {
        sequenceOf(configurationName.apiVariant())
      } else if (!configurationName.isApi()) {
        emptySequence()
      } else {
        sourceSets.keys
          .asSequence()
          .filter { it != SourceSetName.MAIN }
          .flatMap { it.javaConfigurationNames() }
      }

      val downstream = downstreamNames.map { configurations.getValue(it) }

      configurations.putIfAbsent(
        configurationName,
        Config(
          name = configurationName,
          upstreamSequence = upstream,
          downstreamSequence = downstream
        )
      )
    }
}

internal val McProjectBuilderScope.configFactory
  get() = ConfigFactory(
    { this },
    { configurations.values.asSequence().map { it.name.value } },
    { configurations[asConfigurationName()]?.upstream?.map { it.name.value }.orEmpty() }
  )

internal fun <T : McProjectBuilderScope, R : McProject> T.toProject(
  projectFactory: T.(JvmFileProvider.Factory) -> R
): R {

  populateSourceSets()
  populateConfigsFromSourceSets()

  val jvmFileProviderFactory = JvmFileProvider.Factory { project, sourceSetName ->
    RealJvmFileProvider(
      fileCache = FileCache(), project = project, sourceSetName = sourceSetName
    )
  }

  return projectFactory(jvmFileProviderFactory)
    .also { projectCache[it.path] = it }
}

fun McProjectBuilderScope.toProject(): RealMcProject {
  return toProject { jvmFileProviderFactory ->
    RealMcProject(
      path = path,
      projectDir = projectDir,
      buildFile = buildFile,
      configurations = Configurations(configurations),
      hasKapt = hasKapt,
      hasTestFixturesPlugin = hasTestFixturesPlugin,
      sourceSets = SourceSets(sourceSets),
      projectCache = projectCache,
      anvilGradlePlugin = anvilGradlePlugin,
      logger = PrintLogger(),
      jvmFileProviderFactory = jvmFileProviderFactory,
      javaSourceVersion = javaSourceVersion,
      projectDependencies = lazy { projectDependencies },
      externalDependencies = lazy { externalDependencies },
      buildFileParserFactory = buildFileParserFactory()
    )
  }
}
