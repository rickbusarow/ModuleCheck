package com.rickbusarow.modulecheck.testing

import com.squareup.kotlinpoet.FileSpec
import java.nio.file.Path

class ProjectSrcSpec private constructor(
  val dir: Path,
  val files: List<FileSpec>
) {

  fun writeIn(path: Path) {
    files.forEach {
      it.writeTo(Path.of("$path/$dir"))
    }
  }

  class Builder(val dir: Path) {

    private val files = mutableListOf<FileSpec>()

    fun addFile(fileSpec: FileSpec) = apply {
      files.add(fileSpec)
    }

    fun build(): ProjectSrcSpec = ProjectSrcSpec(dir, files)
  }
}
