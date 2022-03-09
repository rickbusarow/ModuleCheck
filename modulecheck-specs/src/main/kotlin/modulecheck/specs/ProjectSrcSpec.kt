/*
 * Copyright (C) 2021-2022 Rick Busarow
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

package modulecheck.specs

import com.squareup.kotlinpoet.FileSpec
import modulecheck.specs.ProjectSrcSpecBuilder.RawFile
import org.intellij.lang.annotations.Language
import java.io.File
import java.nio.file.Path

public data class ProjectSrcSpec(
  public var dir: Path,
  public val fileSpecs: MutableList<FileSpec>,
  public val rawFiles: MutableList<RawFile>
) {

  public fun toBuilder(dir: Path): ProjectSrcSpecBuilder = ProjectSrcSpecBuilder(
    dir = dir,
    fileSpecs = fileSpecs,
    rawFiles = rawFiles
  )

  public inline fun edit(
    dir: Path,
    init: ProjectSrcSpecBuilder.() -> Unit
  ): ProjectSrcSpec = toBuilder(dir).apply { init() }.build()

  public fun writeIn(path: Path) {
    rawFiles.forEach {
      File("$path/$dir".fixPath()).mkdirs()
      File("$path/$dir/${it.fileName}".fixPath()).writeText(it.text)
    }
    fileSpecs.forEach {
      it.writeTo(File("$path/$dir"))
    }
  }

  public companion object {

    public operator fun invoke(
      dir: Path,
      init: ProjectSrcSpecBuilder.() -> Unit
    ): ProjectSrcSpec = ProjectSrcSpecBuilder(dir = dir, init = init).build()

    public fun builder(
      dir: Path
    ): ProjectSrcSpecBuilder = ProjectSrcSpecBuilder(dir = dir)
  }
}

public class ProjectSrcSpecBuilder(
  public var dir: Path,
  public val fileSpecs: MutableList<FileSpec> = mutableListOf(),
  public val rawFiles: MutableList<RawFile> = mutableListOf(),
  init: ProjectSrcSpecBuilder.() -> Unit = {}
) : Builder<ProjectSrcSpec> {

  init {
    init()
  }

  public fun addRawFile(fileName: String, text: String) {
    rawFiles.add(RawFile(fileName = fileName, text = text.trimIndent()))
  }

  public fun addXmlFile(
    fileName: String,
    @Language("xml")
    text: String
  ) {
    rawFiles.add(RawFile(fileName = fileName, text = text.trimIndent()))
  }

  public fun addKotlinFile(
    fileName: String,
    @Language("kotlin")
    text: String
  ) {
    rawFiles.add(RawFile(fileName = fileName, text = text.trimIndent()))
  }

  public fun addRawFile(rawFile: RawFile) {
    rawFiles.add(rawFile)
  }

  public fun addFileSpec(fileSpec: FileSpec) {
    fileSpecs.add(fileSpec)
  }

  override fun build(): ProjectSrcSpec = ProjectSrcSpec(dir, fileSpecs, rawFiles)

  public data class RawFile(val fileName: String, val text: String)
}

internal fun String.fixPath(): String = replace("/", File.separator)
