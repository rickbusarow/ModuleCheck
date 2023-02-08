/*
 * Copyright (C) 2021-2023 Rick Busarow
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

import kotlinx.coroutines.runBlocking
import modulecheck.config.CodeGeneratorBinding
import modulecheck.model.dependency.ConfigurationName
import modulecheck.model.dependency.Configurations
import modulecheck.model.dependency.ExternalDependencies
import modulecheck.model.dependency.HasDependencies
import modulecheck.model.dependency.MavenCoordinates
import modulecheck.model.dependency.ProjectDependencies
import modulecheck.model.dependency.ProjectPath.StringProjectPath
import modulecheck.model.dependency.TypeSafeProjectPathResolver
import modulecheck.model.dependency.impl.RealConfiguredProjectDependencyFactory
import modulecheck.model.dependency.impl.RealExternalDependencyFactory
import modulecheck.model.dependency.javaConfigurationNames
import modulecheck.model.sourceset.SourceSetName
import modulecheck.parsing.gradle.dsl.BuildFileParser
import modulecheck.parsing.gradle.dsl.HasDependencyDeclarations
import modulecheck.parsing.gradle.dsl.InvokesConfigurationNames
import modulecheck.parsing.gradle.dsl.addDependency
import modulecheck.parsing.gradle.dsl.asDeclaration
import modulecheck.parsing.kotlin.compiler.impl.DependencyModuleDescriptorAccess
import modulecheck.parsing.source.AnvilGradlePlugin
import modulecheck.project.McProject
import modulecheck.project.ProjectCache
import modulecheck.project.ProjectProvider
import modulecheck.utils.child
import modulecheck.utils.createSafely
import modulecheck.utils.requireNotNull
import org.intellij.lang.annotations.Language
import org.jetbrains.kotlin.config.JvmTarget
import java.io.File

@Suppress("LongParameterList")
class McProjectBuilder<P : PlatformPluginBuilder<*>>(
  var path: StringProjectPath,
  var projectDir: File,
  override var buildFile: File,
  val platformPlugin: P,
  val codeGeneratorBindings: List<CodeGeneratorBinding>,
  val projectProvider: ProjectProvider,
  val projectCache: ProjectCache,
  val dependencyModuleDescriptorAccess: DependencyModuleDescriptorAccess,
  override val projectDependencies: ProjectDependencies = ProjectDependencies(mapOf()),
  override val externalDependencies: ExternalDependencies = ExternalDependencies(mapOf()),
  override var hasKapt: Boolean = false,
  override var hasTestFixturesPlugin: Boolean = false,
  var anvilGradlePlugin: AnvilGradlePlugin? = null,
  var jvmTarget: JvmTarget = JvmTarget.JVM_11
) : HasDependencyDeclarations, InvokesConfigurationNames, HasDependencies {

  private val _duringBuildActions = mutableListOf<() -> Unit>()

  override val buildFileParser: BuildFileParser
    get() = buildFileParserFactory(configuredProjectDependencyFactory).create(this)
  override val configurations: Configurations
    get() = configurations
  override val hasAnvil: Boolean
    get() = anvilGradlePlugin != null
  override val hasAGP: Boolean
    get() = platformPlugin is AndroidPlatformPluginBuilder<*>

  val configuredProjectDependencyFactory: RealConfiguredProjectDependencyFactory by lazy {
    RealConfiguredProjectDependencyFactory(
      pathResolver = TypeSafeProjectPathResolver(projectProvider),
      generatorBindings = codeGeneratorBindings
    )
  }

  private val externalDependency by lazy {
    RealExternalDependencyFactory(generatorBindings = codeGeneratorBindings)
  }

  fun addDependency(
    configurationName: ConfigurationName,
    project: McProject,
    asTestFixture: Boolean = false,
    addToBuildFile: Boolean = true
  ) {

    configurationName.maybeAddToSourceSetsAndConfigurations()

    val old = projectDependencies[configurationName].orEmpty()

    val newDependency = configuredProjectDependencyFactory
      .create(configurationName, project.projectPath, asTestFixture)

    if (addToBuildFile) {
      onBuild {

        val declarationExists = runBlocking {
          buildFileParser.dependenciesBlocks().any { dependenciesBlock ->
            dependenciesBlock.getOrEmpty(project.projectPath, configurationName, asTestFixture)
              .isNotEmpty()
          }
        }

        if (!declarationExists) {
          val (newDeclaration, tokenOrNull) = runBlocking {
            newDependency.asDeclaration(this@McProjectBuilder)
          }
          addDependency(
            configuredDependency = newDependency,
            newDeclaration = newDeclaration,
            existingMarkerDeclaration = tokenOrNull
          )
        }
      }
    }

    projectDependencies[configurationName] = old + newDependency
  }

  fun addExternalDependency(
    configurationName: ConfigurationName,
    coordinates: String,
    isTestFixture: Boolean = false
  ) {

    val maven = MavenCoordinates.parseOrNull(coordinates)
      .requireNotNull {
        "The external coordinate string `$coordinates` must match the Maven coordinate pattern."
      }

    configurationName.maybeAddToSourceSetsAndConfigurations()

    val old = externalDependencies[configurationName].orEmpty()

    val external = externalDependency.create(
      configurationName = configurationName,
      group = maven.group,
      moduleName = maven.moduleName,
      version = maven.version,
      isTestFixture = isTestFixture
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
      platformPlugin.configurations[this] = ConfigBuilder(
        name = this,
        upstream = mutableListOf(),
        downstream = mutableListOf()
      )
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

    val oldSourceSet = maybeAddSourceSet(sourceSetName)

    val newJvmFiles = oldSourceSet.jvmFiles + file

    val newSourceSet = oldSourceSet.copy(jvmFiles = newJvmFiles)

    platformPlugin.sourceSets[sourceSetName] = newSourceSet
  }

  fun addJavaSource(
    @Language("java")
    java: String,
    sourceSetName: SourceSetName = SourceSetName.MAIN,
    directory: String? = null,
    fileName: String? = null,
    sourceDirName: String = "java"
  ): File {

    val name = fileName ?: "Source.java"

    val packageName = "package (.*);".toRegex()
      .find(java)
      ?.destructured
      ?.component1()
      .orEmpty()

    val file = createJvmPhysicalFile(
      content = java,
      directory = directory,
      packageName = packageName,
      fileSimpleName = name,
      sourceSetName = sourceSetName,
      sourceDirName = sourceDirName
    )

    return addJvmSource(
      file = file,
      sourceSetName = sourceSetName
    )
  }

  fun addKotlinSource(
    @Language("kotlin")
    kotlin: String,
    sourceSetName: SourceSetName = SourceSetName.MAIN,
    directory: String? = null,
    fileName: String? = null,
    sourceDirName: String = "java"
  ): File {

    val name = fileName ?: "Source.kt"

    val packageName = "package (.*)".toRegex()
      .find(kotlin)
      ?.destructured
      ?.component1()
      .orEmpty()

    val file = createJvmPhysicalFile(
      content = kotlin,
      directory = directory,
      packageName = packageName,
      fileSimpleName = name,
      sourceSetName = sourceSetName,
      sourceDirName = sourceDirName
    )

    return addJvmSource(
      file = file,
      sourceSetName = sourceSetName
    )
  }

  private fun createJvmPhysicalFile(
    content: String,
    directory: String?,
    packageName: String,
    fileSimpleName: String,
    sourceSetName: SourceSetName,
    sourceDirName: String = "java"
  ): File {

    val dir = (directory ?: packageName.replace('.', '/'))
      .fixFileSeparators()

    return projectDir
      .child("src", sourceSetName.value, sourceDirName, dir, fileSimpleName)
      .createSafely(content.trimIndent())
  }

  @Suppress("LongParameterList")
  private fun addJvmSource(
    file: File,
    sourceSetName: SourceSetName
  ): File {

    val oldSourceSet = maybeAddSourceSet(sourceSetName)

    val newJvmFiles = oldSourceSet.jvmFiles + file

    val newSourceSet = oldSourceSet.copy(jvmFiles = newJvmFiles)

    platformPlugin.sourceSets[sourceSetName] = newSourceSet

    return file
  }

  /**
   * Replace Windows file separators with Unix ones, just for string comparison in tests
   *
   * @since 0.12.0
   */
  private fun String.fixFileSeparators(): String = replace("/", File.separator)

  fun <T : AndroidPlatformPluginBuilder<*>> McProjectBuilder<T>.addResourceFile(
    name: String,
    @Language("xml")
    content: String,
    sourceSetName: SourceSetName = SourceSetName.MAIN
  ) {

    require(!name.startsWith("layout/")) { "use `addLayoutFile` for layout files." }

    val file = File(projectDir, "src/${sourceSetName.value}/res/$name")
      .createSafely(content.trimIndent())

    val old = maybeAddSourceSet(sourceSetName)

    platformPlugin.sourceSets[sourceSetName] =
      old.copy(resourceFiles = old.resourceFiles + file)
  }

  fun <T : AndroidPlatformPluginBuilder<*>> McProjectBuilder<T>.addLayoutFile(
    name: String,
    @Language("xml")
    content: String,
    sourceSetName: SourceSetName = SourceSetName.MAIN
  ) {
    val file = File(projectDir, "src/${sourceSetName.value}/res/layout/$name")
      .createSafely(content)

    val old = maybeAddSourceSet(sourceSetName)

    platformPlugin.sourceSets[sourceSetName] = old.copy(layoutFiles = old.layoutFiles + file)
  }

  fun <T : AndroidPlatformPluginBuilder<*>> McProjectBuilder<T>.addManifest(
    @Language("xml")
    content: String,
    sourceSetName: SourceSetName = SourceSetName.MAIN
  ) {
    val file = File(projectDir, "src/${sourceSetName.value}/AndroidManifest.xml")
      .createSafely(content)

    platformPlugin.manifests[sourceSetName] = file
  }

  @Suppress("LongParameterList")
  fun addSourceSet(
    name: SourceSetName,
    jvmFiles: Set<File> = emptySet(),
    resourceFiles: Set<File> = emptySet(),
    layoutFiles: Set<File> = emptySet(),
    upstreamNames: List<SourceSetName> = emptyList(),
    downstreamNames: List<SourceSetName> = emptyList()
  ): SourceSetBuilder {

    val old = platformPlugin.sourceSets[name]

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
    platformPlugin.sourceSets.requireSourceSetExists(name)
  }

  private fun onBuild(action: () -> Unit) {
    _duringBuildActions.add(action)
  }

  @PublishedApi
  internal fun executeDuringBuildActions() {
    _duringBuildActions.forEach { it.invoke() }
  }
}
