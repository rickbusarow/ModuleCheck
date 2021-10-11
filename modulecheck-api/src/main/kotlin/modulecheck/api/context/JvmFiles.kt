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

package modulecheck.api.context

import modulecheck.api.Project2
import modulecheck.api.SourceSetName
import modulecheck.parsing.JvmFile
import modulecheck.parsing.java.JavaFile
import modulecheck.parsing.psi.KotlinFile
import modulecheck.parsing.psi.internal.asKtFile
import org.jetbrains.kotlin.incremental.isJavaFile
import org.jetbrains.kotlin.incremental.isKotlinFile
import java.io.File
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.ConcurrentMap

data class JvmFiles(
  internal val delegate: ConcurrentMap<SourceSetName, List<JvmFile>>
) : ConcurrentMap<SourceSetName, List<JvmFile>> by delegate,
  ProjectContext.Element {

  override val key: ProjectContext.Key<JvmFiles>
    get() = Key

  companion object Key : ProjectContext.Key<JvmFiles> {
    override operator fun invoke(project: Project2): JvmFiles {
      val map = project
        .sourceSets
        .map { (sourceSetName, _) ->

          sourceSetName to project
            .jvmSourcesForSourceSetName(sourceSetName)
            .flatMap { directory ->
              directory.walkTopDown()
                .asSequence()
                .filter { maybeFile -> maybeFile.isFile }
                .mapNotNull { file ->
                  JvmFile.fromFile(file, project, sourceSetName)
                }.toList()
            }
        }.toMap()

      return JvmFiles(ConcurrentHashMap(map))
    }
  }
}

val ProjectContext.jvmFiles: JvmFiles get() = get(JvmFiles)
fun ProjectContext.jvmFilesForSourceSetName(sourceSetName: SourceSetName): List<JvmFile> =
  jvmFiles[sourceSetName].orEmpty()

fun JvmFile.Companion.fromFile(
  file: File,
  project: Project2,
  sourceSetName: SourceSetName
): JvmFile? {
  return when {
    file.isKotlinFile(listOf("kt")) -> {
      KotlinFile(
        file.asKtFile(),
        project.bindingContextForSourceSetName(sourceSetName)
      )
    }
    file.isJavaFile() -> JavaFile(file)
    else -> null
  }
}
