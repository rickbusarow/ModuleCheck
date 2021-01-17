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

package com.rickbusarow.modulecheck.specs

import com.squareup.kotlinpoet.FileSpec
import java.io.File
import java.nio.file.Path

@Suppress("MemberVisibilityCanBePrivate")
public class ProjectSrcSpec private constructor(
  public val dir: Path,
  public val files: List<FileSpec>
) {

  public fun writeIn(path: Path) {
    files.forEach {
      val txt = it.toString()
      File("$path/$dir").mkdirs()
      File("$path/$dir/${it.name}").writeText(txt)
//      it.writeTo(Path.of("$path/$dir"))
    }
  }

  public class Builder internal constructor(public val dir: Path) {

    private val files = mutableListOf<FileSpec>()

    public fun addFile(fileSpec: FileSpec): Builder = apply {
      files.add(fileSpec)
    }

    public fun build(): ProjectSrcSpec = ProjectSrcSpec(dir, files)
  }

  public companion object {

    public fun builder(path: Path): Builder = Builder(path)

    public fun builder(path: String): Builder = Builder(Path.of(path))
  }
}
