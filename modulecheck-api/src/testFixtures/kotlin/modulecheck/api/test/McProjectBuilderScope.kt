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

package modulecheck.api.test

import modulecheck.api.RealMcProject
import modulecheck.parsing.*
import org.intellij.lang.annotations.Language
import java.io.File
import java.util.concurrent.ConcurrentHashMap

interface McProjectBuilderScope {
  var path: String
  var projectDir: File
  var buildFile: File
  val configurations: MutableMap<ConfigurationName, Config>
  val projectDependencies: ProjectDependencies
  var hasKapt: Boolean
  val sourceSets: MutableMap<SourceSetName, SourceSet>
  var anvilGradlePlugin: AnvilGradlePlugin?
  val projectCache: ConcurrentHashMap<String, McProject>

  fun addDependency(
    configurationName: ConfigurationName,
    project: McProject,
    asTestFixture: Boolean = false
  ) {

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
  override var hasKapt: Boolean = false,
  override val sourceSets: MutableMap<SourceSetName, SourceSet> = mutableMapOf(
    SourceSetName.MAIN to SourceSet(SourceSetName.MAIN)
  ),
  override var anvilGradlePlugin: AnvilGradlePlugin? = null,
  override val projectCache: ConcurrentHashMap<String, McProject> = ConcurrentHashMap()
) : McProjectBuilderScope

internal fun createProject(
  projectCache: ConcurrentHashMap<String, McProject>,
  projectDir: File,
  path: String,
  config: McProjectBuilderScope.() -> Unit
): McProject {

  val projectRoot = File(projectDir, path.replace(":", File.separator))
    .also { it.mkdirs() }

  val buildFile = File(projectRoot, "build.gradle.kts")
    .also { it.createNewFile() }

  val builder = JvmMcProjectBuilderScope(path, projectRoot, buildFile, projectCache = projectCache)
    .also { it.config() }

  builder.sourceSets
    .keys
    .flatMap { it.configurationNames() }
    .forEach { configurationName ->

      builder.configurations.putIfAbsent(
        configurationName,
        Config(
          name = configurationName,
          externalDependencies = emptySet(),
          inherited = emptySet()
        )
      )
    }

  val delegate = RealMcProject(
    path = builder.path,
    projectDir = builder.projectDir,
    buildFile = builder.buildFile,
    configurations = builder.configurations,
    hasKapt = builder.hasKapt,
    sourceSets = builder.sourceSets,
    projectCache = builder.projectCache,
    anvilGradlePlugin = builder.anvilGradlePlugin,
    projectDependencies = lazy { builder.projectDependencies }
  )

  return delegate
    .also { projectCache[it.path] = it }
}
