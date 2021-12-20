/*
 * Copyright (C) 2021 Rick Busarow
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
import modulecheck.parsing.gradle.SourceSet
import modulecheck.parsing.gradle.SourceSetName
import modulecheck.parsing.gradle.SourceSets
import modulecheck.project.ConfiguredProjectDependency
import modulecheck.project.ExternalDependencies
import modulecheck.project.McProject
import modulecheck.project.PrintLogger
import modulecheck.project.ProjectCache
import modulecheck.project.ProjectDependencies
import modulecheck.project.impl.RealMcProject
import modulecheck.project.temp.AnvilGradlePlugin
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
  val sourceSets: MutableMap<SourceSetName, SourceSet>
  var anvilGradlePlugin: AnvilGradlePlugin?
  val projectCache: ProjectCache

  fun addDependency(
    configurationName: ConfigurationName,
    project: McProject,
    asTestFixture: Boolean = false
  ) {

    val sourceSetName = configurationName.toSourceSetName()
    if (!sourceSets.containsKey(sourceSetName)) {
      addSourceSet(sourceSetName)
    }

    val old = projectDependencies[configurationName].orEmpty()

    val cpd = ConfiguredProjectDependency(configurationName, project, asTestFixture)

    projectDependencies[configurationName] = old + cpd
  }

  fun addSource(
    name: String,
    @Language("kotlin")
    content: String,
    sourceSetName: SourceSetName = SourceSetName.MAIN
  ) {

    val file = File(projectDir, "src/${sourceSetName.value}/$name")
      .createSafely(content)

    val old = sourceSets.getOrPut(sourceSetName) { SourceSet(sourceSetName) }

    sourceSets[sourceSetName] = old.copy(jvmFiles = old.jvmFiles + file)
  }

  @Suppress("LongParameterList")
  fun addSourceSet(
    name: SourceSetName,
    classpathFiles: Set<File> = emptySet(),
    outputFiles: Set<File> = emptySet(),
    jvmFiles: Set<File> = emptySet(),
    resourceFiles: Set<File> = emptySet(),
    layoutFiles: Set<File> = emptySet()
  ): SourceSet {

    val new = SourceSet(
      name = name,
      classpathFiles = classpathFiles,
      outputFiles = outputFiles,
      jvmFiles = jvmFiles,
      resourceFiles = resourceFiles,
      layoutFiles = layoutFiles
    )

    val old = sourceSets.put(name, new)

    require(old == null) { "A source set for that name already exists." }

    return new
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
  override val sourceSets: MutableMap<SourceSetName, SourceSet> = mutableMapOf(
    SourceSetName.MAIN to SourceSet(SourceSetName.MAIN)
  ),
  override var anvilGradlePlugin: AnvilGradlePlugin? = null,
  override val projectCache: ProjectCache = ProjectCache()
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

  val builder = JvmMcProjectBuilderScope(path, projectRoot, buildFile, projectCache = projectCache)
    .also { it.config() }

  return builder.toProject()
}

internal fun McProjectBuilderScope.populateConfigs() {
  sourceSets
    .keys
    // add main source set configs first so that they can be safely looked up for inheriting configs
    .sortedByDescending { it == SourceSetName.MAIN }
    .flatMap { it.javaConfigurationNames() }
    .forEach { configurationName ->

      val inherited = if (configurationName.toSourceSetName() == SourceSetName.MAIN) {
        emptySet()
      } else {
        SourceSetName.MAIN.javaConfigurationNames()
          .map { configurations.getValue(it) }
          .toSet()
      }

      configurations.putIfAbsent(
        configurationName,
        Config(
          name = configurationName,
          inherited = inherited
        )
      )
    }
}

internal fun McProjectBuilderScope.populateSourceSets() {
  configurations
    .keys
    .map { it.toSourceSetName() }
    .distinct()
    .filterNot { sourceSets.containsKey(it) }
    .forEach { addSourceSet(it) }
}

fun McProjectBuilderScope.toProject(): RealMcProject {

  populateConfigs()
  populateSourceSets()

  val delegate = RealMcProject(
    path = path,
    projectDir = projectDir,
    buildFile = buildFile,
    configurations = Configurations(configurations),
    hasKapt = hasKapt,
    sourceSets = SourceSets(sourceSets),
    projectCache = projectCache,
    anvilGradlePlugin = anvilGradlePlugin,
    buildFileParser = buildFileParser(buildFile),
    logger = PrintLogger(),
    projectDependencies = lazy { projectDependencies },
    externalDependencies = lazy { externalDependencies }
  )

  return delegate
    .also { projectCache[it.path] = it }
}
