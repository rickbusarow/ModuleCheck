package com.rickbusarow.modulecheck

import com.github.javaparser.StaticJavaParser
import org.jetbrains.kotlin.psi.KtFile
import java.io.File

sealed class JvmFile {
  abstract val packageFqName: String
  abstract val importDirectives: Set<String>

  data class KotlinFile(val ktFile: KtFile) : JvmFile() {

    override val packageFqName by lazy { ktFile.packageFqName.asString() }
    override val importDirectives by lazy {
      ktFile
        .importDirectives
        .mapNotNull { importDirective ->
          importDirective
            .importPath
            ?.pathStr
            ?.split(".")
            ?.dropLast(1)
            ?.joinToString(".")
        }
        .toSet()
    }
  }

  data class JavaFile(val file: File) : JvmFile() {

    val parsed by lazy { StaticJavaParser.parse(file) }

    override val packageFqName by lazy { parsed.packageDeclaration.get().nameAsString }
    override val importDirectives by lazy {
      parsed.imports.map {
        it.nameAsString
          .split(".")
          .dropLast(1)
          .joinToString(".")
      }.toSet()
    }
  }
}
