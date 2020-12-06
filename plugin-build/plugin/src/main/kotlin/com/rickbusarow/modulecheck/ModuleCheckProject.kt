package com.rickbusarow.modulecheck

import com.rickbusarow.modulecheck.internal.*
import org.gradle.api.Project
import org.gradle.api.artifacts.ProjectDependency
import java.util.concurrent.ConcurrentHashMap
import kotlin.reflect.KProperty1

class ProjectDependencies(private val project: ModuleCheckProject) {

  val compileOnlyDependencies by unsafeLazy { dependencyProjects("compileOnly") }
  val apiDependencies by unsafeLazy { dependencyProjects("api") }
  val implementationDependencies by unsafeLazy { dependencyProjects("implementation") }
  val mainDependencies by unsafeLazy {
    compileOnlyDependencies + apiDependencies + implementationDependencies
  }
  val testImplementationDependencies by unsafeLazy { dependencyProjects("testImplementation") }
  val androidTestImplementationDependencies by unsafeLazy {
    dependencyProjects("androidTestImplementation")
  }

  private fun dependencyProjects(configurationName: String) =
    project.project
      .configurations
      .filter { it.name == configurationName }
      .flatMap { config ->
        config
          .dependencies
          .withType(ProjectDependency::class.java)
          .map { ModuleCheckProject.from(it.dependencyProject) }
          .toSet()
      }
}

class ModuleCheckProject private constructor(
  val project: Project
) : Comparable<ModuleCheckProject> {

  init {
    cache[project] = this
  }

  val path: String = project.path

  val dependencies = ProjectDependencies(this)

  val androidTestFiles by unsafeLazy {
    project.androidTestJavaRoot.jvmFiles() + project.androidTestKotlinRoot.jvmFiles()
  }
  val mainFiles by unsafeLazy {
    project.mainJavaRoot.jvmFiles() + project.mainKotlinRoot.jvmFiles()
  }
  val testFiles by unsafeLazy {
    project.testJavaRoot.jvmFiles() + project.testKotlinRoot.jvmFiles()
  }

  val mainLayoutViewDependencies by unsafeLazy {
    mainLayoutFiles.map { it.customViews }.flatten().toSet()
  }

  val mainLayoutResourceDependencies by unsafeLazy {
    mainLayoutFiles.map { it.customViews }.flatten().toSet()
  }

  val androidTestImports by unsafeLazy {
    androidTestFiles.flatMap { jvmFile -> jvmFile.importDirectives }.toSet()
  }

  val mainImports by unsafeLazy {
    (mainFiles.flatMap { jvmFile -> jvmFile.importDirectives } + mainLayoutViewDependencies).toSet()
  }

  val testImports by unsafeLazy {
    testFiles.flatMap { jvmFile -> jvmFile.importDirectives }.toSet()
  }

  val androidTestPackages by unsafeLazy { androidTestFiles.map { it.packageFqName }.toSet() }
  val mainPackages by unsafeLazy { mainFiles.map { it.packageFqName }.toSet() }
  val testPackages by unsafeLazy { testFiles.map { it.packageFqName }.toSet() }

  val mainLayoutFiles by unsafeLazy {
    project.mainLayoutRootOrNull()?.walkTopDown()?.files().orEmpty().map { XmlFile.LayoutFile(it) }
  }

  fun getMainDepth(): Int {
    val all =
      dependencies.compileOnlyDependencies + dependencies.apiDependencies + dependencies.implementationDependencies

    return if (all.isEmpty()) 0
    else all.map { cache.getValue(it.project).getMainDepth() }.max()!! + 1
  }

  fun getTestDepth(): Int =
    if (dependencies.testImplementationDependencies.isEmpty()) {
      0
    } else {
      dependencies.testImplementationDependencies.map {
        cache.getValue(it.project).getMainDepth()
      }.max()!! + 1
    }

  val androidTestDepth: Int
    get() =
      if (dependencies.androidTestImplementationDependencies.isEmpty()) {
        0
      } else {
        dependencies.androidTestImplementationDependencies
          .map { cache.getValue(it.project).getMainDepth() }
          .max()!! + 1
      }

  fun allPublicClassPathDependencyDeclarations(): List<ModuleCheckProject> =
    dependencies.apiDependencies + dependencies.apiDependencies.flatMap {
      it.allPublicClassPathDependencyDeclarations()
    }

  fun inheritedMainDependencyProjects(): List<ModuleCheckProject> {

    val main = dependencies.apiDependencies + dependencies.implementationDependencies

    return dependencies.apiDependencies + main.flatMap { pdd ->

      pdd.inheritedMainDependencyProjects() +
        pdd.dependencies.apiDependencies.flatMap {
          it.inheritedMainDependencyProjects()
        }
    }
  }

  fun overshotDependencies(): List<DependencyFinding.OverShotDependency> {

    val allMain =
      (dependencies.apiDependencies + dependencies.implementationDependencies).toSet()

    return allPublicClassPathDependencyDeclarations()
      .filterNot { allMain.contains(it) }
      .filter { inheritedNewProject ->
        inheritedNewProject.mainPackages.any { newProjectPackage ->
          mainImports.contains(newProjectPackage)
        }
      }
      .groupBy { it.project }
      .map { overshot ->
        DependencyFinding.OverShotDependency(
          project,
          overshot.key.path,
          "main",
          overshot.value.map { it }.distinctBy { it.project }
        )
      }
  }

  fun unusedAndroidTest() =
    findUnused(
      androidTestImports,
      ProjectDependencies::androidTestImplementationDependencies,
      "androidTest"
    )

  fun unusedApi() = findUnused(mainImports, ProjectDependencies::apiDependencies, "api")

  fun unusedCompileOnly() =
    findUnused(mainImports, ProjectDependencies::compileOnlyDependencies, "compileOnly")

  fun unusedImplementation() =
    findUnused(mainImports, ProjectDependencies::implementationDependencies, "implementation")

  fun unusedTestImplementation() =
    findUnused(
      testImports, ProjectDependencies::testImplementationDependencies, "testImplementation"
    )

  fun redundantAndroidTest(): List<DependencyFinding.RedundantDependency> {
    val inheritedDependencyProjects = inheritedMainDependencyProjects().map { it.project }.toSet()

    return dependencies.androidTestImplementationDependencies
      .filter { inheritedDependencyProjects.contains(it.project) }
      .map {
        val from =
          inheritedMainDependencyProjects()
            .filter { inherited -> inherited.project == it.project }
            .map { it.project }

        DependencyFinding.RedundantDependency(
          project, it.positionIn(project), it.project.path, "androidTest", from
        )
      }
      .distinctBy { it.position }
  }

  fun redundantMain(): List<DependencyFinding.RedundantDependency> {
    val allMain = (dependencies.apiDependencies + dependencies.implementationDependencies)

    val inheritedDependencyProjects = inheritedMainDependencyProjects().map { it.project }.toSet()

    return allMain.filter { inheritedDependencyProjects.contains(it.project) }.map {
      val from =
        inheritedMainDependencyProjects()
          .filter { inherited -> inherited.project == it.project }
          .map { it.project }
      DependencyFinding.RedundantDependency(
        project,
        it.positionIn(project),
        it.project.path,
        "main",
        from
      )
    }
  }

  fun redundantTest(): List<DependencyFinding.RedundantDependency> {
    val inheritedDependencyProjects = inheritedMainDependencyProjects().map { it.project }.toSet()

    return dependencies.testImplementationDependencies
      .filter { inheritedDependencyProjects.contains(it.project) }
      .map {
        val from =
          inheritedMainDependencyProjects()
            .filter { inherited -> inherited.project == it.project }
            .map { it.project }
        DependencyFinding.RedundantDependency(
          project,
          it.positionIn(project),
          it.project.path,
          "test",
          from
        )
      }
  }

  private fun findUnused(
    imports: Set<String>,
    dependencyKProp: KProperty1<ProjectDependencies, List<ModuleCheckProject>>,
    configurationName: String
  ): List<DependencyFinding.UnusedDependency> =
    dependencyKProp(this.dependencies)
      // .filterNot { alwaysIgnore.contains(it.project.path)}
      .mapNotNull { projectDependency ->

        val used =
          imports.any { importString ->
            when {
              projectDependency.mainPackages.contains(importString) -> true
              else ->
                dependencyKProp(this.dependencies).any { childProjectDependency ->

                  dependencyKProp(childProjectDependency.dependencies).contains(projectDependency)
                }
            }
          }

        if (!used) {
          DependencyFinding.UnusedDependency(
            project,
            projectDependency.positionIn(project),
            projectDependency.project.path,
            configurationName
          )
        } else null
      }

  fun positionIn(parent: Project): ProjectDependencyDeclaration.Position =
    parent.buildFile.readText().lines().positionOf(project)

  private fun DependencyFinding.moduleCheckProject() = cache.getValue(dependentProject)
  private fun ProjectDependencyDeclaration.moduleCheckProject() = cache.getValue(project)

  override fun compareTo(other: ModuleCheckProject): Int = path.compareTo(other.path)

  override fun toString(): String {
    return """ModuleCheckProject( path='$path' )"""
  }

  companion object {
    private val cache = ConcurrentHashMap<Project, ModuleCheckProject>()

    fun reset() {
      cache.clear()
    }

    fun from(project: Project) = cache.getOrPut(project) { ModuleCheckProject(project) }

    fun dependentsOf(project: Project): List<ModuleCheckProject> {

      val dep = from(project)

      return cache.values.filter {
        it.dependencies.implementationDependencies.any { it == dep } ||
          it.dependencies.mainDependencies.any { it == dep } ||
          it.dependencies.testImplementationDependencies.any { it == dep } ||
          it.dependencies.androidTestImplementationDependencies.any { it == dep } ||
          it.dependencies.compileOnlyDependencies.any { it == dep }
      }
    }
  }
}
