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

package modulecheck.core

import modulecheck.api.*
import modulecheck.api.Config.*
import modulecheck.api.Finding.Position
import modulecheck.api.psi.PsiElementWithSurroundingText
import modulecheck.core.files.KotlinFile
import modulecheck.core.files.XmlFile
import modulecheck.core.internal.*
import modulecheck.core.kapt.KaptParser
import modulecheck.core.parser.*
import modulecheck.core.parser.android.AndroidManifestParser
import modulecheck.core.parser.android.AndroidResourceParser
import modulecheck.psi.*
import modulecheck.psi.internal.*
import org.jetbrains.kotlin.psi.KtCallExpression
import java.io.File
import java.util.concurrent.*

class MCP private constructor(
  val project: Project2
) : Comparable<MCP> {

  init {
    cache[project] = this
  }

  val path: String = project.path

  val dependencies by DependencyParser.parseLazy(project)
  val kaptDependencies by KaptParser.parseLazy(project)

/*  val resolvedMainDependencies by lazy {

    val all = dependencies.main() + allPublicClassPathDependencyDeclarations()

    all.filter { dep ->
      dep.mcp().mainDeclarations.any { it in mainImports }
    }
  }*/

  val overshot by OvershotParser.parseLazy(project)
  val unused by UnusedParser.parseLazy(project)
  val redundant by RedundantParser.parseLazy(project)

  val androidTestFiles = project
    .androidTestJavaRoot
    .jvmFiles(project.bindingContextForSourceSet(SourceSet("androidTest"))) + project
    .androidTestKotlinRoot
    .jvmFiles(project.bindingContextForSourceSet(SourceSet("androidTest")))
  val mainFiles = project
    .mainJavaRoot
    .jvmFiles(project.bindingContextForSourceSet(SourceSet("main"))) + project
    .mainKotlinRoot
    .jvmFiles(project.bindingContextForSourceSet(SourceSet("main")))
  val testFiles = project
    .testJavaRoot
    .jvmFiles(project.bindingContextForSourceSet(SourceSet("test"))) + project
    .testKotlinRoot
    .jvmFiles(project.bindingContextForSourceSet(SourceSet("test")))

  val mainLayoutFiles = project
    .mainLayoutRootOrNull()
    ?.walkTopDown()
    ?.files()
    .orEmpty()
    .map { XmlFile.LayoutFile(it) }

  val androidTestImports = androidTestFiles.flatMap { jvmFile -> jvmFile.imports }.toSet()

  private val references = ConcurrentHashMap<Config, Set<String>>()
  private val files = ConcurrentHashMap<BuildType, Set<JvmFile>>()

/*  fun filesFor(buildType: BuildType) = when(buildType) {
    BuildType.AndroidTest -> androidTestFiles
    Debug ->
    Main -> mainFiles
    Release -> TODO()
    Test -> testFiles
    is BuildType.Custom -> TODO()
  }

  fun referenceFor(config: Config): Set<String> {

    when (config) {
      AndroidTest -> references.getOrPut(Config.AndroidTest) {}
      Api -> TODO()
      CompileOnly -> TODO()
      is Custom -> TODO()
      Implementation -> TODO()
      Kapt -> TODO()
      KaptAndroidTest -> TODO()
      KaptTest -> TODO()
      RuntimeOnly -> TODO()
      TestApi -> TODO()
      TestImplementation -> TODO()
    }
  }*/

  val androidTestExtraPossibleReferences by lazy {
    androidTestFiles
      .flatMap { jvmFile -> jvmFile.maybeExtraReferences }
      .toSet()
  }

  val mainImports by lazy {
    val imports = mainFiles
      .flatMap { jvmFile -> jvmFile.imports }

    val customViewReferences = mainLayoutFiles
      .map { it.customViews }
      .flatten()
      .toSet()

    (imports + customViewReferences).toSet()
  }

  val mainExtraPossibleReferences by lazy {
    mainFiles
      .flatMap { jvmFile -> jvmFile.maybeExtraReferences }
      .toSet()
  }

  val testExtraPossibleReferences by lazy {
    testFiles
      .flatMap { jvmFile -> jvmFile.maybeExtraReferences }
      .toSet()
  }

  val testImports = testFiles.flatMap { jvmFile -> jvmFile.imports }.toSet()

  val isAndroid by lazy { project.isAndroid() }

  val androidPackageOrNull by lazy {

    val manifest = File("${project.srcRoot}/main/AndroidManifest.xml")

    if (!manifest.exists()) return@lazy null

    AndroidManifestParser.parse(manifest)["package"]
  }

  // val androidTestDeclarations by lazy { androidTestFiles.flatMap { it.declarations }.toSet() }

  val mainDeclarations by lazy {

    val rPackage = androidPackageOrNull

    if (isAndroid && rPackage != null) {
      mainFiles.flatMap { it.declarations }.toSet() + "$rPackage.R"
    } else {
      mainFiles.flatMap { it.declarations }.toSet()
    }
  }

/*  val androidTestAndroidResDeclarations by lazy {

    val rPackage = androidPackageOrNull
    val resDir = project.androidTestResRootOrNull()

    if (isAndroid && rPackage != null && resDir != null) {
      AndroidResourceParser.parse(resDir)
    } else emptySet()
  }*/

  val mainAndroidResDeclarations by lazy {

    val rPackage = androidPackageOrNull
    val resDir = project.mainResRootOrNull()

    if (isAndroid && rPackage != null && resDir != null) {
      AndroidResourceParser.parse(resDir)
    } else emptySet()
  }

/*  val testAndroidResDeclarations by lazy {

    val rPackage = androidPackageOrNull
    val resDir = project.testResRootOrNull()

    if (isAndroid && rPackage != null && resDir != null) {
      AndroidResourceParser.parse(resDir)
    } else emptySet()
  }

  val testDeclarations by lazy { testFiles.flatMap { it.declarations }.toSet() }*/
  val mustBeApi by lazy {

    val noIdeaWhereTheyComeFrom = mainFiles
      .filterIsInstance<KotlinFile>()
      .flatMap { kotlinFile ->
        kotlinFile
          .apiReferences
          .filterNot { it in mainDeclarations }
      }.toSet()

    dependencies
      .main()
      .filterNot { it in dependencies.api }
      .filter { cpp ->
        cpp.mcp().mainDeclarations.any { declared ->
          declared in noIdeaWhereTheyComeFrom
        }
      }
  }

  fun dependents() = cache
    .values
    .filter {
      it.dependencies.all()
        .any { dep -> dep.project == this.project }
    }

  fun allPublicClassPathDependencyDeclarations(): Set<ConfiguredProjectDependency> =
    dependencies.api + dependencies.api
      .flatMap {
        it.mcp().allPublicClassPathDependencyDeclarations()
      }

/*  fun inheritedMainDependencyProjects(): List<MCP> {
    return dependencies
      .main()
      .flatMap { pdd ->

        pdd.mcp().inheritedMainDependencyProjects() + pdd.mcp()
          .dependencies
          .api.flatMap {
            it.mcp().inheritedMainDependencyProjects()
          }
      }
  }*/

  fun sourceOf(
    dependencyProject: ConfiguredProjectDependency,
    apiOnly: Boolean = false
  ): MCP? {
    val toCheck = if (apiOnly) {
      dependencies.api
    } else {
      dependencies.main()
    }

    if (dependencyProject in toCheck) return this

    return toCheck.firstOrNull {
      it == dependencyProject || it.mcp().sourceOf(dependencyProject, true) != null
    }
      ?.mcp()
  }

  fun getMainDepth(): Int {
    val all = dependencies.main()

    return if (all.isEmpty()) 0
    else all.map { it.mcp().getMainDepth() }.max()!! + 1
  }

  fun getTestDepth(): Int = if (dependencies.testImplementation.isEmpty()) {
    0
  } else {
    dependencies.testImplementation
      .map { it.mcp().getMainDepth() }
      .max()!! + 1
  }

  val androidTestDepth: Int
    get() = if (dependencies.androidTest.isEmpty()) {
      0
    } else {
      dependencies.androidTest
        .map { it.mcp().getMainDepth() }
        .max()!! + 1
    }

  override fun compareTo(other: MCP): Int = project.path.compareTo(other.project.path)

  fun psiElementIn(
    parent: Project2,
    configuration: Config
  ): PsiElementWithSurroundingText? {
    val kotlinBuildFile = parent.buildFile.asKtsFileOrNull() ?: return null

    val result = DslBlockVisitor("dependencies")
      .parse(kotlinBuildFile)
      ?: return null

    val p = ProjectDependencyDeclarationVisitor(configuration, project.path)

    return result.elements
      .firstOrNull { element ->

        p.find(element.psiElement as KtCallExpression)
      }
  }

  fun positionIn(
    parent: Project2,
    configuration: Config
  ): Position? = parent
    .buildFile
    .readText()
    .lines()
    .positionOf(project, configuration)

  override fun toString(): String {
    return "MCP(project=$project)"
  }

  data class Parsed<T>(
    val androidTest: Set<T>,
    val api: Set<T>,
    val compileOnly: Set<T>,
    val implementation: Set<T>,
    val runtimeOnly: Set<T>,
    val testApi: Set<T>,
    val testImplementation: Set<T>
  ) {
    fun all() =
      androidTest + api + compileOnly + implementation + runtimeOnly + testApi + testImplementation

    fun main() = api + compileOnly + implementation + runtimeOnly
  }

  companion object {
    private val cache = ConcurrentHashMap<Project2, MCP>()

    fun reset() {
      Output.printGreen("                                                          resetting")
      cache.clear()
    }

    fun from(project: Project2): MCP = cache.getOrPut(project) { MCP(project) }
  }
}

@JvmName("CppCollectionToMCP")
fun Collection<ConfiguredProjectDependency>.mcp() = map { MCP.from(it.project) }
fun ConfiguredProjectDependency.mcp() = MCP.from(this.project)

@JvmName("ProjectCollectionToMCP")
fun Collection<Project2>.mcp() = map { MCP.from(it) }
fun Project2.mcp() = MCP.from(this)
