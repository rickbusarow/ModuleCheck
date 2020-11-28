package com.rickbusarow.modulecheck

import com.rickbusarow.modulecheck.internal.*
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.artifacts.ProjectDependency
import java.util.concurrent.ConcurrentHashMap
import kotlin.system.measureTimeMillis

class ModuleCheckPlugin : Plugin<Project> {

  override fun apply(project: Project) {

    project.tasks.register("moduleCheck") {

      description = "verification"

      doLast {

        val cli = Cli()

        lateinit var things: List<IntermediateModuleCheckProject>

        val time = measureTimeMillis {

          things = project.rootProject.allprojects
            .map { it.toModuleCheckProject() }
        }

        val mapped: Map<Project, IntermediateModuleCheckProject> = things.associateBy { it.project }

        val unused = things.flatMap { parent ->

          parent.mainDependencies.mapNotNull { projectDependency ->

            val dp = mapped[projectDependency]

            require(dp != null) {
              """map does not contain ${projectDependency} 
              |
              |${mapped.keys}
            """.trimMargin()
            }

            val used = parent.mainImports.any { importString ->
              when {
                dp.mainPackages.contains(importString) -> true
                else -> parent.mainDependencies.any { childProjectDependency ->
                  val dpp = mapped[childProjectDependency]!!

                  dpp.mainDependencies.contains(projectDependency).also {
                    if (it) {
                      logger.info("project ${parent.path} couldn't find import for $projectDependency but allowing because it's a valid dependency for child ${childProjectDependency}")
                    }
                  }
                }
              }

            }

            if (!used) {
              parent.project to projectDependency.path
            } else null

          }
        }

//        things.groupBy { it.depth }.toSortedMap().forEach { (depth, modules) ->
//          cli.printBlue("""$depth  ${modules.joinToString { it.path }}""")
//        }

        cli.printGreen("total parsing time --> $time milliseconds")

        if (unused.isNotEmpty()) {

          unused.forEach {
            logger.error("unused dependency: ${it.first.buildFile} -- ${it.second}")
//            logger.error("unused dependency: ${it.first.projectDir}/build.gradle.kts: (15, 1): ${it.second}")
          }
        }
      }
    }
  }
}

private val cache = ConcurrentHashMap<Project, ModuleCheckProject>()

fun Project.toModuleCheckProject(): IntermediateModuleCheckProject {

//  println("build file --> ${buildFile}")

  val mainFiles = mainJavaRoot.walkTopDown()
    .files()
    .filter { it.name.endsWith(".kt") }
    .ktFiles()
    .map { JvmFile(it.packageFqName.asString(), it.importDirectives.toSet()) }
    .toList()

  val testFiles = testJavaRoot.walkTopDown()
    .files()
    .filter { it.name.endsWith(".kt") }
    .ktFiles()
    .map { JvmFile(it.packageFqName.asString(), it.importDirectives.toSet()) }
    .toList()

  val mainPackages = mainFiles.map { it.packageFqName }.toSet()

  val xmlParser = AndroidLayoutParser()

  val mainLayoutDependencies = mainLayoutRootOrNull()?.walkTopDown()
    ?.files()
    ?.map { xmlParser.parse(it) }
    ?.flatten()
    ?.toSet().orEmpty()

  val mainImports = mainFiles.flatMap {
    it.importDirectives.mapNotNull { importDirective ->

      importDirective.importPath
        ?.pathStr
        ?.split(".")
        ?.dropLast(1)
        ?.joinToString(".")
    } + mainLayoutDependencies.map { layoutDependency ->
      layoutDependency.split(".")
        .dropLast(1)
        .joinToString(".")
    }
  }.toSet()

  val testPackages = testFiles.map { it.packageFqName }.toSet()
  val testImports = testFiles.flatMap {
    it.importDirectives.mapNotNull { importDirective ->

      importDirective.importPath
        ?.pathStr
        ?.split(".")
        ?.dropLast(1)
        ?.joinToString(".")
    }
  }.toSet()

  val mainDependencyProjects = configurations
    .filter { it.name == "api" || it.name == "implementation" }
    .flatMap { config ->

      config.dependencies
        .withType(ProjectDependency::class.java)
        .map { it.dependencyProject }
    }.toSet()

  val testDependencyProjects = configurations
    .filter { it.name == "testApi" || it.name == "testImplementation" }
    .flatMap { config ->

      config.dependencies
        .withType(ProjectDependency::class.java)
        .map { it.dependencyProject }
    }.toSet()

  return IntermediateModuleCheckProject(
    path = path,
    project = this,
    mainPackages = mainPackages,
    mainImports = mainImports,
    mainDependencies = mainDependencyProjects,
    testPackages = testPackages,
    testImports = testImports,
    testDependencies = testDependencyProjects
  ) { setOf() }
}
