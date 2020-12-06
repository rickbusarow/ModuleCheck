package com.rickbusarow.modulecheck

import kotlin.reflect.KProperty1

class ProjectFindings(val project: ModuleCheckProject) {

  fun getMainDepth(): Int {
    val all =
      project.dependencies.compileOnlyDependencies + project.dependencies.apiDependencies + project.dependencies.implementationDependencies

    return if (all.isEmpty()) 0
    else all.map { ModuleCheckProject.from(it.project).findings.getMainDepth() }.max()!! + 1
  }

  fun getTestDepth(): Int = if (project.dependencies.testImplementationDependencies.isEmpty()) {
    0
  } else {
    project.dependencies.testImplementationDependencies.map {
      ModuleCheckProject.from(it.project).findings.getMainDepth()
    }.max()!! + 1
  }

  val androidTestDepth: Int
    get() = if (project.dependencies.androidTestImplementationDependencies.isEmpty()) {
      0
    } else {
      project.dependencies.androidTestImplementationDependencies
        .map { ModuleCheckProject.from(it.project).findings.getMainDepth() }
        .max()!! + 1
    }

  fun unusedAndroidTest() = findUnused(
    project.androidTestImports,
    ProjectDependencies::androidTestImplementationDependencies,
    "androidTest"
  )

  fun unusedApi() = findUnused(
    project.mainImports, ProjectDependencies::apiDependencies, "api"
  )

  fun unusedCompileOnly() = findUnused(
    project.mainImports, ProjectDependencies::compileOnlyDependencies, "compileOnly"
  )

  fun unusedImplementation() = findUnused(
    project.mainImports, ProjectDependencies::implementationDependencies, "implementation"
  )

  fun unusedTestImplementation() = findUnused(
    project.testImports,
    ProjectDependencies::testImplementationDependencies,
    "testImplementation"
  )


  private fun findUnused(
    imports: Set<String>,
    dependencyKProp: KProperty1<ProjectDependencies, List<ModuleCheckProject>>,
    configurationName: String
  ): List<DependencyFinding.UnusedDependency> =
    dependencyKProp(project.dependencies)
      // .filterNot { alwaysIgnore.contains(it.project.path)}
      .mapNotNull { projectDependency ->

        val used = imports.any { importString ->
          when {
            projectDependency.mainPackages.contains(importString) -> true
            else ->
              dependencyKProp(project.dependencies).any { childProjectDependency ->

                dependencyKProp(childProjectDependency.dependencies).contains(projectDependency)
              }
          }
        }

        if (!used) {
          DependencyFinding.UnusedDependency(
            project.project,
            projectDependency.positionIn(project.project),
            projectDependency.project.path,
            configurationName
          )
        } else null
      }

  fun overshotDependencies(): List<DependencyFinding.OverShotDependency> {

    val allMain =
      (project.dependencies.apiDependencies + project.dependencies.implementationDependencies).toSet()

    return project.allPublicClassPathDependencyDeclarations()
      .filterNot { allMain.contains(it) }
      .filter { inheritedNewProject ->
        inheritedNewProject.mainPackages.any { newProjectPackage ->
          project.mainImports.contains(newProjectPackage)
        }
      }
      .groupBy { it.project }
      .map { (overshot, _) ->

        val links = ModuleCheckProject.dependentsOf(overshot).filter { it in allMain }

        val linkPosition = links.firstOrNull()?.positionIn(project.project)
          ?: ModuleCheckProject.Position(-1, -1)

        DependencyFinding.OverShotDependency(
          project.project,
          overshot.path,
          "main",
          linkPosition,
          links.map { it }.distinctBy { it.project }
        )
      }
  }

  fun redundantAndroidTest(): List<DependencyFinding.RedundantDependency> {
    val inheritedDependencyProjects =
      project.inheritedMainDependencyProjects().map { it.project }.toSet()

    return project.dependencies.androidTestImplementationDependencies
      .filter { inheritedDependencyProjects.contains(it.project) }
      .map {
        val from = project.inheritedMainDependencyProjects()
          .filter { inherited -> inherited.project == it.project }
          .map { it.project }

        DependencyFinding.RedundantDependency(
          project.project, it.positionIn(project.project), it.project.path, "androidTest", from
        )
      }
      .distinctBy { it.position }
  }

  fun redundantMain(): List<DependencyFinding.RedundantDependency> {
    val allMain =
      (project.dependencies.apiDependencies + project.dependencies.implementationDependencies)

    val inheritedDependencyProjects =
      project.inheritedMainDependencyProjects().map { it.project }.toSet()

    return allMain.filter { inheritedDependencyProjects.contains(it.project) }.map {
      val from = project.inheritedMainDependencyProjects()
        .filter { inherited -> inherited.project == it.project }
        .map { it.project }
      DependencyFinding.RedundantDependency(
        project.project,
        it.positionIn(project.project),
        it.project.path,
        "main",
        from
      )
    }
  }

  fun redundantTest(): List<DependencyFinding.RedundantDependency> {
    val inheritedDependencyProjects =
      project.inheritedMainDependencyProjects().map { it.project }.toSet()

    return project.dependencies.testImplementationDependencies
      .filter { inheritedDependencyProjects.contains(it.project) }
      .map {
        val from = project.inheritedMainDependencyProjects()
          .filter { inherited -> inherited.project == it.project }
          .map { it.project }
        DependencyFinding.RedundantDependency(
          project.project,
          it.positionIn(project.project),
          it.project.path,
          "test",
          from
        )
      }
  }

}
