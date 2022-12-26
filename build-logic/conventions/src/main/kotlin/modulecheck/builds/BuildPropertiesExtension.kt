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

package modulecheck.builds

import org.gradle.api.Project
import org.gradle.api.tasks.TaskProvider
import org.intellij.lang.annotations.Language
import org.jetbrains.kotlin.gradle.dsl.KotlinJvmProjectExtension
import org.jetbrains.kotlin.util.prefixIfNot
import org.jetbrains.kotlin.util.suffixIfNot
import java.io.File

interface BuildPropertiesExtension {

  fun Project.buildProperties(
    sourceSetName: String,
    @Language("kotlin")
    content: String
  ) {
    setUpGeneration(
      sourceSetName = sourceSetName,
      content = content.trimIndent()
    )
  }
}

private fun Project.setUpGeneration(
  sourceSetName: String,
  @Language("kotlin")
  content: String
) {

  val generatedDir = generatedDir(sourceSetName)

  extensions.configure(KotlinJvmProjectExtension::class.java) { extension ->
    extension.sourceSets.named(sourceSetName) {
      it.kotlin.srcDir(generatedDir)
    }
  }

  val (packageName, className) = packageNameToClassName(content)
  val buildPropertiesFile = generatedFile(
    sourceSetName = sourceSetName,
    packageName = packageName,
    className = className
  )

  val genTask = registerTask(
    sourceSetName = sourceSetName,
    generatedDir = generatedDir,
    buildPropertiesFile = buildPropertiesFile,
    content = content
  )

  registerSimpleGenerationTaskAsDependency(
    sourceSetName = sourceSetName,
    taskProvider = genTask
  )
}

private fun Project.registerTask(
  sourceSetName: String,
  generatedDir: File,
  buildPropertiesFile: File,
  content: String
): TaskProvider<ModuleCheckBuildCodeGeneratorTask> {
  val catalogs = rootProject.file(
    "build-logic/core/src/main/kotlin/modulecheck/builds/catalogs.kt"
  )

  val sourceSetTaskName = when (sourceSetName) {
    "main" -> ""
    else -> sourceSetName.capitalize()
  }

  return tasks.register(
    "generate${sourceSetTaskName}BuildProperties",
    ModuleCheckBuildCodeGeneratorTask::class.java
  ) {
    it.inputs.file(catalogs)

    it.outputs.file(buildPropertiesFile)

    it.doLast {
      generatedDir.deleteRecursively()
      buildPropertiesFile.parentFile.mkdirs()
      buildPropertiesFile.writeText(
        content.prefixIfNot("@file:Suppress(\"AbsentOrWrongFileLicense\")\n\n")
          .suffixIfNot("\n")
      )
    }
  }
}

private fun Project.generatedDir(sourceSetName: String): File {
  return buildDir.resolve(
    "generated/sources/buildProperties/kotlin/$sourceSetName"
  )
}

private fun packageNameToClassName(content: String): Pair<String, String> {

  val packageNameRegex = "package (\\S*)".toRegex()

  val lines = content.lines()
    .filter { it.isNotBlank() }

  val packageName = lines.first { it.trim().matches(packageNameRegex) }
    .replace(packageNameRegex) { match ->
      match.destructured.component1().trim()
    }

  val classNameRegex = "(?>internal )?(?>class|object) (\\S*).*".toRegex()

  val typeName = lines.first { it.trim().matches(classNameRegex) }
    .replace(classNameRegex) { match ->
      match.destructured.component1().trim()
    }

  return packageName to typeName
}

private fun Project.generatedFile(
  sourceSetName: String,
  packageName: String,
  className: String
): File {
  return generatedDir(sourceSetName)
    .resolve(packageName.replace(".", File.separator))
    .resolve("$className.kt")
}
