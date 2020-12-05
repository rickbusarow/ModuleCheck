package com.rickbusarow.modulecheck

import com.rickbusarow.modulecheck.internal.*
import java.util.concurrent.ConcurrentHashMap
import kotlin.reflect.KProperty1
import org.gradle.api.Project
import org.gradle.api.artifacts.ProjectDependency

data class ModuleCheckProject private constructor(val project: Project) : Comparable<ModuleCheckProject> {

  init {
    cache[project] = this
  }

  val path: String = project.path

  val androidTestFiles by unsafeLazy {
    project.androidTestJavaRoot.jvmFiles() + project.androidTestKotlinRoot.jvmFiles()
  }
  val mainFiles by unsafeLazy {
    project.mainJavaRoot.jvmFiles() + project.mainKotlinRoot.jvmFiles()
  }
  val testFiles by unsafeLazy {
    project.testJavaRoot.jvmFiles() + project.testKotlinRoot.jvmFiles()
  }

  val androidTestPackages by unsafeLazy { androidTestFiles.map { it.packageFqName }.toSet() }
  val mainPackages by unsafeLazy { mainFiles.map { it.packageFqName }.toSet() }
  val testPackages by unsafeLazy { testFiles.map { it.packageFqName }.toSet() }

  val mainLayoutFiles by unsafeLazy {
    project.mainLayoutRootOrNull()?.walkTopDown()?.files().orEmpty().map { XmlFile.LayoutFile(it) }
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

  fun getMainDepth(): Int {
    val all = compileOnlyDependencies + apiDependencies + implementationDependencies

    return if (all.isEmpty()) 0
    else (all.map { cache.getValue(it.project).getMainDepth() }.max()!! + 1)
  }

  fun getTestDepth(): Int =
      if (testImplementationDependencies.isEmpty()) {
        0
      } else {
        (testImplementationDependencies.map { cache.getValue(it.project).getMainDepth() }.max()!! +
            1)
      }

  val androidTestDepth: Int
    get() =
        if (androidTestImplementationDependencies.isEmpty()) {
          0
        } else {
          (androidTestImplementationDependencies
              .map { cache.getValue(it.project).getMainDepth() }
              .max()!! + 1)
        }

  fun allPublicClassPathDependencyDeclarations():List<ModuleCheckProject> = apiDependencies + apiDependencies.flatMap {
          it. allPublicClassPathDependencyDeclarations()
        }

  fun inheritedMainDependencyProjects(): List<ModuleCheckProject> {

    val main = apiDependencies + implementationDependencies

    return apiDependencies + main.flatMap { pdd ->

      pdd.inheritedMainDependencyProjects() +
          pdd.apiDependencies.flatMap {
                it. inheritedMainDependencyProjects()
              }
        }
  }

  fun overshotDependencies(): List<DependencyFinding.OverShotDependency> {

    val free = allPublicClassPathDependencyDeclarations()

    val allMain =
        (apiDependencies + implementationDependencies) .toSet()

    return free
        .filterNot { allMain.contains(it ) }
        .filter { inheritedNewProject ->
          inheritedNewProject .mainPackages.any { newProjectPackage ->
            mainImports.contains(newProjectPackage)
          }
        }
        .groupBy { it.project }
        .map { overshot ->
          DependencyFinding.OverShotDependency(
              project,
              overshot.key.path,
              "main",
              overshot.value.map { it }.distinctBy { it.project })
        }
  }

  fun unusedAndroidTest() =
      findUnused(
          androidTestImports,
          ModuleCheckProject::androidTestImplementationDependencies,
          "androidTest")

  fun unusedApi() = findUnused(mainImports, ModuleCheckProject::apiDependencies, "api")

  fun unusedCompileOnly() =
      findUnused(mainImports, ModuleCheckProject::compileOnlyDependencies, "compileOnly")

  fun unusedImplementation() =
      findUnused(mainImports, ModuleCheckProject::implementationDependencies, "implementation")

  fun unusedTestImplementation() =
      findUnused(
          testImports, ModuleCheckProject::testImplementationDependencies, "testImplementation")

  fun redundantAndroidTest(): List<DependencyFinding.RedundantDependency> {
    val inheritedDependencyProjects = inheritedMainDependencyProjects().map { it.project }.toSet()

    return androidTestImplementationDependencies
        .filter { inheritedDependencyProjects.contains(it.project) }
        .map {
          val from =
              inheritedMainDependencyProjects()
                  .filter { inherited -> inherited.project == it.project }
                  .map { it.project }

          DependencyFinding.RedundantDependency(
              project, it.positionIn(project), it.project.path, "androidTest", from)
        }
        .distinctBy { it.position }
  }

  fun redundantMain(): List<DependencyFinding.RedundantDependency> {
    val allMain = (apiDependencies + implementationDependencies)

    val inheritedDependencyProjects = inheritedMainDependencyProjects().map { it.project }.toSet()

    return allMain.filter { inheritedDependencyProjects.contains(it.project) }.map {
      val from =
          inheritedMainDependencyProjects()
              .filter { inherited -> inherited.project == it.project }
              .map { it.project }
      DependencyFinding.RedundantDependency(project, it.positionIn(project), it.project.path, "main", from)
    }
  }

  fun redundantTest(): List<DependencyFinding.RedundantDependency> {
    val inheritedDependencyProjects = inheritedMainDependencyProjects().map { it.project }.toSet()

    return testImplementationDependencies
        .filter { inheritedDependencyProjects.contains(it.project) }
        .map {
          val from =
              inheritedMainDependencyProjects()
                  .filter { inherited -> inherited.project == it.project }
                  .map { it.project }
          DependencyFinding.RedundantDependency(project, it.positionIn(project), it.project.path, "test", from)
        }
  }

  private fun findUnused(
      imports: Set<String>,
      dependencyKProp: KProperty1<ModuleCheckProject, List<ModuleCheckProject>>,
      configurationName: String
  ): List<DependencyFinding.UnusedDependency> =
      dependencyKProp(this)
          // .filterNot { alwaysIgnore.contains(it.project.path)}
          .mapNotNull { projectDependency ->

            val used =
                imports.any { importString ->
                  when {
                    projectDependency.mainPackages.contains(importString) -> true
                    else ->
                        dependencyKProp(this).any { childProjectDependency ->

                          dependencyKProp(childProjectDependency).contains(projectDependency)
                        }
                  }
                }

            if (!used) {
              DependencyFinding.UnusedDependency(
                  project,
                  projectDependency.positionIn(project),
                  projectDependency.project.path,
                  configurationName)
            } else null
          }

  private fun dependencyProjects(configurationName: String) =
      project.configurations.filter { it.name == configurationName }.flatMap { config ->
        config
            .dependencies
            .withType(ProjectDependency::class.java)
            .map {
              ProjectDependencyDeclaration(project = it.dependencyProject, parent = project).moduleCheckProject()
            }
            .toSet()
      }

  fun positionIn(parent: Project): ProjectDependencyDeclaration.Position = parent.buildFile.readText().lines().positionOf(project)

  private fun DependencyFinding.moduleCheckProject() = cache.getValue(dependentProject)
  private fun ProjectDependencyDeclaration.moduleCheckProject() = cache.getValue(project)

  override fun compareTo(other: ModuleCheckProject): Int = path.compareTo(other.path)

  override fun toString(): String {
    return """ModuleCheckProject( path='$path' )"""
  }

  companion object {
    private val cache = ConcurrentHashMap<Project, ModuleCheckProject>()

    fun from(project: Project) = cache.getOrPut(project) { ModuleCheckProject(project) }
  }
}
