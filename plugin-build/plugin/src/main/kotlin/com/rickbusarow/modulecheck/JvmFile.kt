package com.rickbusarow.modulecheck

import com.github.javaparser.StaticJavaParser
import java.io.File
import org.gradle.api.Project
import org.jetbrains.kotlin.psi.KtFile

sealed class JvmFile {
  abstract val packageFqName: String
  abstract val importDirectives: Set<String>

  data class KotlinFile(val ktFile: KtFile) : JvmFile() {

    override val packageFqName by unsafeLazy { ktFile.packageFqName.asString() }
    override val importDirectives by unsafeLazy {
      ktFile
          .importDirectives
          .mapNotNull { importDirective ->
            importDirective.importPath?.pathStr?.split(".")?.dropLast(1)?.joinToString(".")
          }
          .toSet()
    }
  }

  data class JavaFile(val file: File) : JvmFile() {

    val parsed by unsafeLazy { StaticJavaParser.parse(file) }

    override val packageFqName by unsafeLazy { parsed.packageDeclaration.get().nameAsString }
    override val importDirectives by unsafeLazy {
      parsed.imports.map { it.nameAsString.split(".").dropLast(1).joinToString(".") }.toSet()
    }
  }
}

data class ProjectDependencyDeclaration(val project: Project, val dependent: Project) {

  val position: Position by unsafeLazy {
    dependent.buildFile.readText().lines().positionOf(project)
  }

  data class Position(val row: Int, val column: Int)
}

fun <T> unsafeLazy(initializer: () -> T): Lazy<T> =
    lazy(mode = LazyThreadSafetyMode.NONE, initializer = initializer)
