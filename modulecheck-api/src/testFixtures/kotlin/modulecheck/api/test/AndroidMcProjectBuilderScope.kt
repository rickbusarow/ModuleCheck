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

import modulecheck.api.RealAndroidMcProject
import modulecheck.parsing.*
import org.intellij.lang.annotations.Language
import java.io.File
import java.util.concurrent.ConcurrentHashMap

interface AndroidMcProjectBuilderScope : McProjectBuilderScope {
  var androidPackage: String
  var androidResourcesEnabled: Boolean
  var viewBindingEnabled: Boolean
  val manifests: MutableMap<SourceSetName, File>

  fun addResourceFile(
    name: String,
    @Language("xml")
    content: String,
    sourceSetName: SourceSetName = SourceSetName.MAIN
  ) {

    require(!name.startsWith("layout/")) { "use `addLayoutFile` for layout files." }

    val file = File(projectDir, "src/${sourceSetName.value}/res/$name").createSafely(content)

    val old = sourceSets.getOrPut(sourceSetName) { SourceSet(sourceSetName) }

    sourceSets[sourceSetName] = old.copy(resourceFiles = old.resourceFiles + file)
  }

  fun addLayoutFile(
    name: String,
    @Language("xml")
    content: String,
    sourceSetName: SourceSetName = SourceSetName.MAIN
  ) {
    val file = File(projectDir, "src/${sourceSetName.value}/res/layout/$name").createSafely(content)

    val old = sourceSets.getOrPut(sourceSetName) { SourceSet(sourceSetName) }

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
  override var path: String,
  override var projectDir: File,
  override var buildFile: File,
  override var androidPackage: String,
  override var androidResourcesEnabled: Boolean = true,
  override var viewBindingEnabled: Boolean = true,
  override val manifests: MutableMap<SourceSetName, File> = mutableMapOf(),
  override val configurations: MutableMap<ConfigurationName, Config> = mutableMapOf(),
  override val projectDependencies: ProjectDependencies = ProjectDependencies(mutableMapOf()),
  override var hasKapt: Boolean = false,
  override val sourceSets: MutableMap<SourceSetName, SourceSet> = mutableMapOf(
    SourceSetName.MAIN to SourceSet(SourceSetName.MAIN)
  ),
  override var anvilGradlePlugin: AnvilGradlePlugin? = null,
  override val projectCache: ConcurrentHashMap<String, McProject> = ConcurrentHashMap()
) : AndroidMcProjectBuilderScope

internal fun createAndroidProject(
  projectCache: ConcurrentHashMap<String, McProject>,
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
    path = path,
    projectDir = projectRoot,
    buildFile = buildFile,
    androidPackage = androidPackage,
    projectCache = projectCache
  )
    .also { it.config() }

  builder.manifests.getOrPut(SourceSetName.MAIN) {
    File(projectRoot, "src/main/AndroidManifest.xml")
      .createSafely("<manifest package=\"$androidPackage\" />")
  }

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

  val delegate = RealAndroidMcProject(
    path = builder.path,
    projectDir = builder.projectDir,
    buildFile = builder.buildFile,
    configurations = builder.configurations,
    hasKapt = builder.hasKapt,
    sourceSets = builder.sourceSets,
    projectCache = builder.projectCache,
    anvilGradlePlugin = builder.anvilGradlePlugin,
    androidResourcesEnabled = builder.androidResourcesEnabled,
    viewBindingEnabled = builder.viewBindingEnabled,
    androidPackageOrNull = builder.androidPackage,
    manifests = builder.manifests,
    projectDependencies = lazy { builder.projectDependencies }
  )

  return delegate
    .also { projectCache[it.path] = it }
}
