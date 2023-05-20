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

import com.vanniktech.maven.publish.tasks.JavadocJar
import org.gradle.api.GradleException
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.publish.maven.plugins.MavenPublishPlugin
import org.gradle.api.tasks.TaskCollection
import org.gradle.language.base.plugins.LifecycleBasePlugin
import org.jetbrains.dokka.DokkaConfiguration
import org.jetbrains.dokka.gradle.AbstractDokkaLeafTask
import org.jetbrains.dokka.gradle.AbstractDokkaTask
import org.jetbrains.dokka.gradle.DokkaMultiModuleTask
import org.jetbrains.dokka.gradle.DokkaTaskPartial
import org.jetbrains.dokka.versioning.VersioningConfiguration
import org.jetbrains.dokka.versioning.VersioningPlugin
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile
import org.jmailen.gradle.kotlinter.tasks.FormatTask
import org.jmailen.gradle.kotlinter.tasks.LintTask
import java.net.URL

abstract class DokkaConventionPlugin : Plugin<Project> {
  override fun apply(target: Project) {

    target.plugins.applyOnce("org.jetbrains.dokka")

    target.tasks.withType(AbstractDokkaLeafTask::class.java).configureEach { dokkaTask ->

      // Dokka doesn't support configuration caching
      dokkaTask.notCompatibleWithConfigurationCache("Dokka doesn't support configuration caching")

      dokkaTask.setMustRunAfter(target)

      val fullModuleName = target.path.removePrefix(":")
      dokkaTask.moduleName.set(fullModuleName)

      if (target != target.rootProject && target.file("src/main").exists()) {
        dokkaTask.configureSourceSets(target)
      }
    }

    target.dependencies.add(
      "dokkaPlugin",
      target.libsCatalog.dependency("dokka-versioning")
    )

    fun TaskCollection<out AbstractDokkaTask>.configureVersioning() = configureEach { task ->

      val dokkaArchiveBuildDir = target.rootDir.resolve("build/tmp/dokka-archive")

      require(task is DokkaTaskPartial || task is DokkaMultiModuleTask) {
        """
        DO NOT JUST CONFIGURE `AbstractDokkaTask`!!!
        This will bundle the full dokka archive (all versions) into the javadoc.jar for every single
        module, which currently adds about 8MB per version in the archive. Set up versioning for the
        Multi-Module tasks ONLY. (DokkaTaskPartial is part of the multi-module tasks).
        """.trimIndent()
      }

      task.pluginConfiguration<VersioningPlugin, VersioningConfiguration> {
        version = VERSION_NAME
        olderVersionsDir = dokkaArchiveBuildDir
        renderVersionsNavigationOnAllPages = true
      }
    }

    target.tasks.withType(DokkaTaskPartial::class.java).configureVersioning()
    target.tasks.withType(DokkaMultiModuleTask::class.java).configureVersioning()

    target.plugins.withType(MavenPublishPlugin::class.java).configureEach {

      val checkJavadocJarIsNotVersioned = target.tasks
        .register(
          "checkJavadocJarIsNotVersioned",
          ModuleCheckBuildTask::class.java
        ) { task ->
          task.description =
            "Ensures that generated javadoc.jar artifacts don't include old Dokka versions"
          task.group = "dokka versioning"

          val javadocTasks = target.tasks.withType(JavadocJar::class.java)
          task.dependsOn(javadocTasks)

          task.inputs.files(javadocTasks.map { it.outputs })

          val zipTrees = javadocTasks.map { target.zipTree(it.archiveFile) }

          task.doLast {

            val jsonReg = """older\/($SEMVER_REGEX)\/version\.json""".toRegex()

            val versions = zipTrees.flatMap { tree ->
              tree
                .filter { it.path.startsWith("older/") }
                .filter { it.isFile }
                .mapNotNull { jsonReg.find(it.path)?.groupValues?.get(1) }
            }

            if (versions.isNotEmpty()) {
              throw GradleException("Found old Dokka versions in javadoc.jar: $versions")
            }
          }
        }

      target.tasks.named(LifecycleBasePlugin.CHECK_TASK_NAME)
        .dependsOn(checkJavadocJarIsNotVersioned)
    }
  }

  private fun AbstractDokkaLeafTask.setMustRunAfter(target: Project) {

    // Dokka uses their outputs but doesn't explicitly depend upon them.
    mustRunAfter(target.tasks.withType(KotlinCompile::class.java))
    mustRunAfter(target.tasks.withType(LintTask::class.java))
    mustRunAfter(target.tasks.withType(FormatTask::class.java))
    mustRunAfter(target.tasks.matchingName("generateProtos"))
  }

  private fun AbstractDokkaLeafTask.configureSourceSets(target: Project) {

    dokkaSourceSets.named("main") { sourceSet ->

      sourceSet.documentedVisibilities.set(
        setOf(
          DokkaConfiguration.Visibility.PUBLIC,
          DokkaConfiguration.Visibility.PRIVATE,
          DokkaConfiguration.Visibility.PROTECTED,
          DokkaConfiguration.Visibility.INTERNAL,
          DokkaConfiguration.Visibility.PACKAGE
        )
      )

      sourceSet.languageVersion.set(target.KOTLIN_API)
      sourceSet.jdkVersion.set(target.JVM_TARGET_INT)

      // include all project sources when resolving kdoc samples
      sourceSet.samples.setFrom(target.fileTree(target.file("src")))

      val readmeFile = target.projectDir.resolve("README.md")

      if (readmeFile.exists()) {
        sourceSet.includes.from(readmeFile)
      }

      sourceSet.sourceLink { sourceLinkBuilder ->
        sourceLinkBuilder.localDirectory.set(target.file("src/main"))

        val modulePath = project.path.replace(":", "/")
          .replaceFirst("/", "")

        // URL showing where the source code can be accessed through the web browser
        sourceLinkBuilder.remoteUrl.set(
          URL("https://github.com/RBusarow/ModuleCheck/blob/main/$modulePath/src/main")
        )
        // Suffix which is used to append the line number to the URL. Use #L for GitHub
        sourceLinkBuilder.remoteLineSuffix.set("#L")
      }
    }
  }
}
