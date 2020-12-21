/*
 * Copyright (C) 2020 Rick Busarow
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
