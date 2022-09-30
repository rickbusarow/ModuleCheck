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

import com.google.devtools.ksp.gradle.KspTaskJvm
import modulecheck.builds.matrix.VersionsFactoryTestTask
import modulecheck.builds.matrix.VersionsMatrix
import org.gradle.api.Project
import org.gradle.api.Task
import org.gradle.api.tasks.TaskProvider
import org.gradle.kotlin.dsl.configure
import org.gradle.kotlin.dsl.getValue
import org.gradle.kotlin.dsl.provideDelegate
import org.gradle.kotlin.dsl.registering
import org.gradle.kotlin.dsl.withType
import org.jetbrains.kotlin.gradle.dsl.KotlinJvmProjectExtension

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

  val versionsMatrixGenerateFactory by tasks.registering(VersionsFactoryTestTask::class) {

    gradleVersion.set(project.gradlePropertyAsProvider("modulecheck.gradleVersion"))
    agpVersion.set(project.gradlePropertyAsProvider("modulecheck.agpVersion"))
    anvilVersion.set(project.gradlePropertyAsProvider("modulecheck.anvilVersion"))
    kotlinVersion.set(project.gradlePropertyAsProvider("modulecheck.kotlinVersion"))

    exhaustive.set(
      project.gradlePropertyAsProvider<String, Boolean>("modulecheck.exhaustive") {
        it?.toBoolean() ?: false
      }
    )

    this@registering.packageName.set(packageName)
    outDir.set(generatedDirPath)

    // auto-update the ci.yml file whenever re-generating the class
    dependsOn(rootProject.tasks.named("versionsMatrixGenerateYaml"))
  }

  registerSimpleGenerationTaskAsDependency(sourceSetName, versionsMatrixGenerateFactory)

  configure<KotlinJvmProjectExtension> {
    sourceSets.named("main") {
      kotlin.srcDir(generatedDirPath)
    }
  }
}

private fun Project.requireInSyncWithToml() {

  with(VersionsMatrix()) {

    sequenceOf(
      Triple(agpList, "agpList", "androidTools"),
      Triple(anvilList, "anvilList", "square-anvil"),
      Triple(kotlinList, "kotlinList", "kotlin")
    )
      .forEach { (list, listName, alias) ->
        require(list.contains(libsCatalog.version(alias))) {
          "The versions catalog version for '$alias' is ${libsCatalog.version(alias)}.  " +
            "Update the VersionsMatrix list '$listName' to include this new version."
        }
      }

    require(gradleList.contains(gradle.gradleVersion)) {
      "The Gradle version is ${gradle.gradleVersion}.  " +
        "Update the VersionsMatrix list 'gradleList' to include this new version."
    }
  }
}

fun Project.registerSimpleGenerationTaskAsDependency(
  sourceSetName: String,
  taskProvider: TaskProvider<out Task>
) {
  val kotlinTaskSourceSetName = when (sourceSetName) {
    "main" -> ""
    else -> sourceSetName.capitalize()
  }

  val ktlintSourceSetName = sourceSetName.capitalize()

  setOf(
    "compile${kotlinTaskSourceSetName}Kotlin",
    "javaSourcesJar",
    "lintKotlin$ktlintSourceSetName",
    "formatKotlin$ktlintSourceSetName",
    "runKtlintCheckOver${ktlintSourceSetName}SourceSet",
    "runKtlintFormatOver${ktlintSourceSetName}SourceSet"
  ).forEach { taskName ->
    tasks.maybeNamed(taskName) { dependsOn(taskProvider) }
  }

  tasks.withType<KspTaskJvm>()
    .configureEach {
      dependsOn(taskProvider)
    }

  // generate the build properties file during an IDE sync, so no more red squigglies
  rootProject.tasks.named("prepareKotlinBuildScriptModel") {
    dependsOn(taskProvider)
  }
}
