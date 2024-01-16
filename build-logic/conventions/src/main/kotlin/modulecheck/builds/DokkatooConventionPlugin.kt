/*
 * Copyright (C) 2021-2024 Rick Busarow
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

import com.rickbusarow.kgx.dependsOn
import com.rickbusarow.kgx.isRootProject
import com.rickbusarow.kgx.projectDependency
import com.rickbusarow.ktlint.KtLintTask
import com.vanniktech.maven.publish.tasks.JavadocJar
import dev.adamko.dokkatoo.DokkatooExtension
import dev.adamko.dokkatoo.dokka.plugins.DokkaVersioningPluginParameters
import dev.adamko.dokkatoo.tasks.DokkatooGenerateTask
import org.gradle.api.GradleException
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.publish.maven.plugins.MavenPublishPlugin
import org.gradle.language.base.plugins.LifecycleBasePlugin
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile
import java.net.URI

abstract class DokkatooConventionPlugin : Plugin<Project> {
  override fun apply(target: Project) {

    target.pluginManager.apply(dev.adamko.dokkatoo.DokkatooPlugin::class.java)

    target.extensions.configure(DokkatooExtension::class.java) { dokkatoo ->

      dokkatoo.versions.jetbrainsDokka.set(target.libs.versions.dokka)

      dokkatoo.moduleVersion.set(target.VERSION_NAME_STABLE)
      val fullModuleName = target.path.removePrefix(":")
      dokkatoo.moduleName.set(fullModuleName)

      dokkatoo.dokkatooSourceSets.configureEach { sourceSet ->
        sourceSet.documentedVisibilities(
          dev.adamko.dokkatoo.dokka.parameters.VisibilityModifier.PRIVATE,
          dev.adamko.dokkatoo.dokka.parameters.VisibilityModifier.INTERNAL,
          dev.adamko.dokkatoo.dokka.parameters.VisibilityModifier.PROTECTED,
          dev.adamko.dokkatoo.dokka.parameters.VisibilityModifier.PACKAGE,
          dev.adamko.dokkatoo.dokka.parameters.VisibilityModifier.PUBLIC
        )

        sourceSet.languageVersion.set(target.KOTLIN_API)
        sourceSet.jdkVersion.set(target.JVM_TARGET_INT)

        // include all project sources when resolving kdoc samples
        sourceSet.samples.setFrom(target.fileTree(target.file("src")))

        if (!target.isRootProject()) {
          val readmeFile = target.projectDir.resolve("README.md")
          if (readmeFile.exists()) {
            sourceSet.includes.from(readmeFile)
          }
        }

        sourceSet.sourceLink { sourceLinkBuilder ->
          sourceLinkBuilder.localDirectory.set(target.file("src/main"))

          val modulePath = target.path.replace(":", "/")
            .replaceFirst("/", "")

          // URL showing where the source code can be accessed through the web browser
          sourceLinkBuilder.remoteUrl.set(
            URI("${SOURCE_WEBSITE}/blob/main/$modulePath/src/main")
          )
          // Suffix which is used to append the line number to the URL. Use #L for GitHub
          sourceLinkBuilder.remoteLineSuffix.set("#L")
        }
      }

      target.tasks.withType(DokkatooGenerateTask::class.java).configureEach { task ->

        task.workerMinHeapSize.set("512m")
        task.workerMaxHeapSize.set("1g")

        if (target.isRootProject()) {
          task.dependsOn("unzipDokkaArchives")
        }

        // Dokka uses their outputs but doesn't explicitly depend upon them.
        task.mustRunAfter(target.tasks.withType(KotlinCompile::class.java))
        task.mustRunAfter(target.tasks.withType(KtLintTask::class.java))
      }

      if (target.isRootProject()) {

        val config = target.configurations.getByName("dokkatoo")

        config.dependencies.addAllLater(
          target.provider {
            target.subprojects
              .filter { sub -> sub.subprojects.isEmpty() }
              .map { sub -> target.projectDependency(sub.path) }
          }
        )

        val pluginConfig = "dokkatooPluginHtml"

        target.dependencies.add(pluginConfig, target.libs.dokka.all.modules)
        target.dependencies.add(pluginConfig, target.libs.dokka.versioning)

        val dokkaArchiveBuildDir = target.rootProject.layout
          .buildDirectory
          .dir("tmp/dokka-archive")

        dokkatoo.pluginsConfiguration
          .withType(DokkaVersioningPluginParameters::class.java).configureEach { versioning ->
            versioning.version.set(target.VERSION_NAME)
            versioning.olderVersionsDir.set(dokkaArchiveBuildDir)
            versioning.renderVersionsNavigationOnAllPages.set(true)
          }

        dokkatoo.dokkatooPublications.configureEach {
          it.suppressObviousFunctions.set(true)
        }
      }
    }

    // Make dummy tasks with the original Dokka plugin names, then delegate them to the Dokkatoo tasks
    target.tasks.register("dokkaHtml").dependsOn(DOKKATOO_HTML_TASK_NAME)
    target.tasks.register("dokkaHtmlMultiModule")
      .dependsOn(target.rootProject.tasks.named(DOKKATOO_HTML_TASK_NAME))

    target.plugins.withType(MavenPublishPlugin::class.java).configureEach {

      val checkJavadocJarIsNotVersioned = target.tasks
        .register("checkJavadocJarIsNotVersioned") { task ->

          task.description =
            "Ensures that generated javadoc.jar artifacts don't include old Dokka versions"
          task.group = "dokka versioning"

          val javadocTasks = target.tasks.withType(JavadocJar::class.java)
          task.dependsOn(javadocTasks)

          task.inputs.files(javadocTasks.map { it.outputs })

          val zipTrees = javadocTasks.map { target.zipTree(it.archiveFile) }

          task.doLast {

            val jsonReg = """older/($SEMVER_REGEX)/version\.json""".toRegex()

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

      target.tasks.named(LifecycleBasePlugin.CHECK_TASK_NAME).dependsOn(
        checkJavadocJarIsNotVersioned
      )
    }
  }

  companion object {
    internal const val DOKKATOO_HTML_TASK_NAME = "dokkatooGeneratePublicationHtml"
  }
}
