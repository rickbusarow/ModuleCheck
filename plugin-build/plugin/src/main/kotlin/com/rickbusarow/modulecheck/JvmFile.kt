package com.rickbusarow.modulecheck

import com.github.javaparser.StaticJavaParser
import org.gradle.api.Project
import org.jetbrains.kotlin.psi.KtFile
import java.io.File

sealed class JvmFile {
  abstract val packageFqName: String
  abstract val importDirectives: Set<String>

  data class KotlinFile(val ktFile: KtFile) : JvmFile() {

    override val packageFqName by unsafeLazy { ktFile.packageFqName.asString() }
    override val importDirectives by unsafeLazy {
      ktFile.importDirectives.mapNotNull { importDirective ->

        importDirective.importPath
          ?.pathStr
          ?.split(".")
          ?.dropLast(1)
          ?.joinToString(".")
      }.toSet()
    }
  }

  data class JavaFile(val file: File) : JvmFile() {

    val parsed by unsafeLazy { StaticJavaParser.parse(file) }

    override val packageFqName by unsafeLazy { parsed.packageDeclaration.get().nameAsString }
    override val importDirectives by unsafeLazy {
      parsed.imports.map {
        it.nameAsString.split(".")
          .dropLast(1)
          .joinToString(".")
      }.toSet()
    }
  }
}

data class ProjectDependencyDeclaration(val project: Project,   val dependent: Project) {

  val position: Position by unsafeLazy {
    dependent.buildFile.readText().lines().positionOf(project)
  }

  data class Position(val row: Int, val column: Int)
}

sealed class DependencyFinding (val problemName: String){
  abstract val dependentProject: Project
  abstract val position: ProjectDependencyDeclaration.Position
  abstract val dependencyPath: String
  abstract val configurationName: String

  data class UnusedDependency(
    override val dependentProject: Project,
    override val position: ProjectDependencyDeclaration.Position,
    override val dependencyPath: String,
    override val configurationName: String
  ) : DependencyFinding("unused")

  data class RedundantDependency(
    override val dependentProject: Project,
    override val position: ProjectDependencyDeclaration.Position,
    override val dependencyPath: String,
    override val configurationName: String,
    val from: List<Project>
  ) : DependencyFinding("redundant") {
    override fun logString(): String = super.logString() + " from: ${from.joinToString { it.path }}"

  }

  open fun logString(): String {

    val pos =
      if (position.row == 0 || position.column == 0) "" else "(${position.row}, ${position.column}): "

    return "${dependentProject.buildFile.path}: $pos${dependencyPath}"
  }

  fun commentOut() {

    val text = dependentProject.buildFile.readText()

    val row = position.row - 1

    val lines = text.lines().toMutableList()

    lines[row] = "//" + lines[row]

    val newText = lines.joinToString("\n")

    dependentProject.buildFile.writeText(newText)


  }
}

fun <T> unsafeLazy(initializer: () -> T): Lazy<T> =
  lazy(mode = LazyThreadSafetyMode.NONE, initializer = initializer)
