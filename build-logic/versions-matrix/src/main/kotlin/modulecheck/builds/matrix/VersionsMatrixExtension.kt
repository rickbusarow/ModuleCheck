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

package modulecheck.builds.matrix

import modulecheck.builds.gradlePropertyAsProvider
import modulecheck.builds.libsCatalog
import modulecheck.builds.registerSimpleGenerationTaskAsDependency
import modulecheck.builds.version
import org.gradle.api.Project
import org.jetbrains.kotlin.gradle.dsl.KotlinJvmProjectExtension
import java.io.File

interface VersionsMatrixExtension {

  fun Project.versionsMatrix(
    sourceSetName: String,
    packageName: String
  ) {
    setUpGeneration(
      sourceSetName = sourceSetName,
      packageName = packageName
    )
  }
}

private fun Project.setUpGeneration(
  sourceSetName: String,
  packageName: String
) {

  val generatedDirPath = buildDir.resolve(
    "generated/sources/versionsMatrix/kotlin/main"
  )

  requireInSyncWithToml()

  val versionsMatrixGenerateFactory = tasks.register(
    "versionsMatrixGenerateFactory",
    VersionsFactoryTestTask::class.java
  ) { task ->

    task.gradleVersion.set(project.gradlePropertyAsProvider("modulecheck.gradleVersion"))
    task.agpVersion.set(project.gradlePropertyAsProvider("modulecheck.agpVersion"))
    task.anvilVersion.set(project.gradlePropertyAsProvider("modulecheck.anvilVersion"))
    task.kotlinVersion.set(project.gradlePropertyAsProvider("modulecheck.kotlinVersion"))

    task.exhaustive.set(
      project.gradlePropertyAsProvider<String, Boolean>("modulecheck.exhaustive") {
        it?.toBoolean() ?: false
      }
    )

    task.packageName.set(packageName)
    task.outDir.set(generatedDirPath)

    // auto-update the ci.yml file whenever re-generating the class
    task.dependsOn(rootProject.tasks.named("versionsMatrixGenerateYaml"))
  }

  registerSimpleGenerationTaskAsDependency(sourceSetName, versionsMatrixGenerateFactory)

  extensions.configure(KotlinJvmProjectExtension::class.java) { extension ->
    extension.sourceSets.named("main") { kotlinSourceSet ->
      kotlinSourceSet.kotlin.srcDir(generatedDirPath)
    }
  }
}

private fun Project.requireInSyncWithToml() {

  val simpleName = VersionsMatrix::class.simpleName
  val versionsMatrixRelativePath = VersionsMatrix::class.qualifiedName!!
    .replace('.', File.separatorChar)
    .let { "$it.kt" }

  val versionMatrixFile = rootDir
    .resolve("build-logic/versions-matrix")
    .resolve("src/main/kotlin")
    .resolve(versionsMatrixRelativePath)

  require(versionMatrixFile.exists()) {
    "Could not resolve the $simpleName file: $versionMatrixFile"
  }

  with(VersionsMatrix()) {

    sequenceOf(
      Triple(agpList, "agpList", "androidTools"),
      Triple(anvilList, "anvilList", "square-anvil"),
      Triple(kotlinList, "kotlinList", "kotlin")
    )
      .forEach { (list, listName, alias) ->
        require(list.contains(libsCatalog.version(alias))) {
          "The versions catalog version for '$alias' is ${libsCatalog.version(alias)}.  " +
            "Update the $simpleName list '$listName' to include this new version.\n" +
            "\tfile://$versionMatrixFile"
        }
      }

    require(gradleList.contains(gradle.gradleVersion)) {
      "The Gradle version is ${gradle.gradleVersion}.  " +
        "Update the $simpleName list 'gradleList' to include this new version.\n" +
        "\tfile://$versionMatrixFile"
    }
  }
}
