/*
 * Copyright (C) 2021-2023 Rick Busarow
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

import com.rickbusarow.kgx.checkProjectIsRoot
import com.rickbusarow.kgx.dependOn
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.tasks.Copy
import org.gradle.api.tasks.Sync
import org.gradle.api.tasks.bundling.Zip
import org.jetbrains.dokka.gradle.DokkaMultiModuleTask

abstract class DokkaVersionArchivePlugin : Plugin<Project> {

  override fun apply(target: Project) {

    target.checkProjectIsRoot {
      "Only apply the dokka version archive plugin to a root project."
    }

    val versionWithoutSnapshot = VERSION_NAME.removeSuffix("-SNAPSHOT")

    val dokkaHtmlMultiModuleBuildDir = target.rootDir.resolve("build/dokka/htmlMultiModule")
    val currentVersionBuildDirZip =
      dokkaHtmlMultiModuleBuildDir.resolveSibling("$versionWithoutSnapshot.zip")

    val dokkaArchiveBuildDir = target.rootDir.resolve("build/tmp/dokka-archive")

    val dokkaArchive = target.rootDir.resolve("dokka-archive")

    val taskGroup = "dokka versioning"

    val unzip = target.tasks
      .register("unzipDokkaArchives", Sync::class.java) { task ->
        task.group = taskGroup
        task.description = "Unzips all zip files in $dokkaArchive into $dokkaArchiveBuildDir"

        task.onlyIf { dokkaArchive.exists() }

        task.into(dokkaArchiveBuildDir)

        dokkaArchive
          .walkTopDown()
          .maxDepth(1)
          .filter { file -> file.isFile }
          .filter { file -> file.extension == "zip" }
          .filter { file -> file.nameWithoutExtension != versionWithoutSnapshot }
          .forEach { zipFile -> task.from(target.zipTree(zipFile)) }
      }

    target.tasks.withType(DokkaMultiModuleTask::class.java).dependOn(unzip)

    val zipDokkaArchive = target.tasks
      .register("zipDokkaArchive", Zip::class.java) { task ->
        task.group = taskGroup
        task.description = "Zips the contents of $dokkaArchiveBuildDir"

        task.destinationDirectory.set(dokkaHtmlMultiModuleBuildDir.parentFile)
        task.archiveFileName.set(currentVersionBuildDirZip.name)
        task.outputs.file(currentVersionBuildDirZip)

        task.enabled = versionWithoutSnapshot == VERSION_NAME

        task.from(dokkaHtmlMultiModuleBuildDir) {
          it.into(versionWithoutSnapshot)
          // Don't copy the `older/` directory into the archive, because all navigation is done using
          // the root version's copy.  Archived `older/` directories just waste space.
          it.exclude("older/**")
        }

        task.mustRunAfter(target.tasks.withType(DokkaMultiModuleTask::class.java))
        task.dependsOn("dokkaHtmlMultiModule")
      }

    target.tasks.register("syncDokkaToArchive", Copy::class.java) { task ->
      task.group = taskGroup
      task.description =
        "sync the Dokka output for the current version to /dokka-archive/$versionWithoutSnapshot"

      task.from(currentVersionBuildDirZip)
      task.into(dokkaArchive)
      task.outputs.file(dokkaArchive.resolve("$versionWithoutSnapshot.zip"))

      task.enabled = versionWithoutSnapshot == VERSION_NAME

      task.mustRunAfter(target.tasks.withType(DokkaMultiModuleTask::class.java))
      task.dependsOn(zipDokkaArchive)

      task.onlyIf {

        val destZip = dokkaArchive.resolve("$versionWithoutSnapshot.zip")

        !destZip.exists() || !currentVersionBuildDirZip.zipContentEquals(destZip)
      }
    }

    target.tasks.withType(DokkaMultiModuleTask::class.java).configureEach {
      it.finalizedBy(zipDokkaArchive)
    }
  }
}
