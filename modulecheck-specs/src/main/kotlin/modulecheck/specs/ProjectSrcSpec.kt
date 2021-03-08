/*
 * Copyright (C) 2021 Rick Busarow
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
import modulecheck.specs.ProjectSrcSpecBuilder.XmlFile
import java.io.File
import java.nio.file.Path

public data class ProjectSrcSpec(
  public var dir: Path,
  public val fileSpecs: MutableList<FileSpec>,
  public val xmlFiles: MutableList<XmlFile>
) {

  public fun toBuilder(dir: Path): ProjectSrcSpecBuilder = ProjectSrcSpecBuilder(
    dir = dir,
    fileSpecs = fileSpecs,
    xmlFiles = xmlFiles
  )

  public inline fun edit(
    dir: Path,
    init: ProjectSrcSpecBuilder.() -> Unit
  ): ProjectSrcSpec = toBuilder(dir).apply { init() }.build()

  public fun writeIn(path: Path) {
    xmlFiles.forEach {
      File("$path/$dir").mkdirs()
      File("$path/$dir/${it.fileName}").writeText(it.text)
    }
    fileSpecs.forEach {
      it.writeTo(File("$path/$dir"))
      // val txt = it.toString()
      // File("$path/$dir").mkdirs()
      // File("$path/$dir/${ it.packageName.replace(".", "/")}/${it.name}").writeText(txt)
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
  public val xmlFiles: MutableList<XmlFile> = mutableListOf(),
  init: ProjectSrcSpecBuilder.() -> Unit = {}
) : Builder<ProjectSrcSpec> {

  init {
    init()
  }

  public fun addFileSpec(fileSpec: FileSpec) {
    fileSpecs.add(fileSpec)
  }

  public fun addXmlFile(file: XmlFile) {
    xmlFiles.add(file)
  }

  override fun build(): ProjectSrcSpec = ProjectSrcSpec(dir, fileSpecs, xmlFiles)

  public data class XmlFile(val fileName: String, val text: String)
}
