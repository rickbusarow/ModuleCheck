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

    init {
      println(
        """classes
        |
        |${ktFile.text}
        |
        |${ktFile.classes.map { it.name }}
        |
        |${
          ktFile.classes.forEach { klass ->
            klass.allInnerClasses.map { it.name }
          }
        }
      """.trimMargin()
      )
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

data class XmlFile(val customViews: Set<String>, val resourceReferences: Set<String>)

data class ProjectDependencyDeclaration(val project: Project, private val dependent: Project) {

  val position: Position by unsafeLazy { dependent.buildFile.readText().lines().positionOf(project) }

  data class Position(val row: Int, val column: Int)
}

data class UnusedDependency(
  val dependentProject: Project,
  val position: ProjectDependencyDeclaration.Position,
  val dependencyPath: String,
  val configurationName: String
) {
  fun logString(): String {

    val pos = if (position.row == 0 || position.column == 0) "" else "(${position.row}, ${position.column}): "

    return "${dependentProject.buildFile.path}: $pos${dependencyPath}"
  }
}

fun <T> unsafeLazy(initializer: () -> T): Lazy<T> = lazy(mode = LazyThreadSafetyMode.NONE, initializer = initializer)
