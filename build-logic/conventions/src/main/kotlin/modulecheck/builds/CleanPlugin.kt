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

import com.rickbusarow.kgx.applyOnce
import com.rickbusarow.kgx.isOrphanedBuildOrGradleDir
import com.rickbusarow.kgx.isOrphanedGradleProperties
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.tasks.Delete
import org.gradle.api.tasks.SourceTask
import org.gradle.language.base.plugins.LifecycleBasePlugin
import java.io.File

abstract class CleanPlugin : Plugin<Project> {
  override fun apply(target: Project) {

    target.plugins.applyOnce("base")

    val deleteEmptyDirs = target.tasks
      .register("deleteEmptyDirs", Delete::class.java) {
        it.description = "Delete all empty directories within a project."
        it.doLast {

          val subprojectDirs = target.subprojects
            .map { it.projectDir.path }

          target.projectDir.walkBottomUp()
            .filter { it.isDirectory }
            .filterNot { dir -> subprojectDirs.any { dir.path.startsWith(it) } }
            .filterNot { it.path.contains(".gradle") }
            .filter { it.listFiles().isNullOrEmpty() }
            .forEach { it.deleteRecursively() }
        }
      }

    target.tasks.named(LifecycleBasePlugin.CLEAN_TASK_NAME) { task ->
      task.dependsOn(deleteEmptyDirs)
    }

    target.tasks.register("cleanGradle", SourceTask::class.java) {
      it.source(".gradle")
      it.doLast {
        target.projectDir.walkBottomUp()
          .filter { it.isDirectory }
          .filter { it.path.contains(".gradle") }
          .all { it.deleteRecursively() }
      }
    }

    if (target == target.rootProject) {
      val deleteOrphanedProjectDirs =
        target.tasks.register("deleteOrphanedProjectDirs", Delete::class.java) { task ->

          task.description = buildString {
            append("Delete any 'build' or `.gradle` directory or `gradle.properties` file ")
            append("without an associated Gradle project.")
          }

          task.doLast {

            val websiteBuildDir = "${target.rootDir}/website/node_modules"

            target.projectDir.walkBottomUp()
              .filterNot { it.path.contains(".git") }
              .filterNot { it.path.startsWith(websiteBuildDir) }
              .filter { it.isOrphanedBuildOrGradleDir() || it.isOrphanedGradleProperties() }
              .forEach(File::deleteRecursively)
          }
        }

      deleteEmptyDirs.configure {
        it.dependsOn(deleteOrphanedProjectDirs)
      }
    }
  }
}
