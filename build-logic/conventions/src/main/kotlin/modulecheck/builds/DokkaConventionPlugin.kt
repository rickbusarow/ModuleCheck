/*
 * Copyright (C) 2021-2025 Rick Busarow
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
import com.rickbusarow.kgx.dependsOn
import com.rickbusarow.kgx.isRootProject
import com.rickbusarow.kgx.projectDependency
import com.rickbusarow.kgx.withType
import com.vanniktech.maven.publish.tasks.JavadocJar
import org.gradle.api.GradleException
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.publish.maven.plugins.MavenPublishPlugin
import org.gradle.language.base.plugins.LifecycleBasePlugin
import org.jetbrains.dokka.gradle.DokkaExtension
import org.jetbrains.dokka.gradle.engine.parameters.VisibilityModifier
import org.jetbrains.dokka.gradle.engine.plugins.DokkaVersioningPluginParameters
import org.jetbrains.dokka.gradle.tasks.DokkaBaseTask
import org.jetbrains.kotlin.gradle.plugin.extraProperties

abstract class DokkaConventionPlugin : Plugin<Project> {
  override fun apply(target: Project) {

    target.extraProperties.apply {
      set("org.jetbrains.dokka.experimental.gradle.pluginMode", "V2Enabled")
      set("org.jetbrains.dokka.experimental.gradle.pluginMode.noWarn", "true")
    }

    target.plugins.applyOnce("org.jetbrains.dokka")

    target.extensions.configure(DokkaExtension::class.java) { dokka ->

      val fullModuleName = target.path.removePrefix(":")
      dokka.moduleName.set(fullModuleName)
      dokka.moduleVersion.set(target.VERSION_NAME_STABLE)

      dokka.dokkaSourceSets.configureEach { ss ->

        val ssName = ss.name

        ss.documentedVisibilities(*VisibilityModifier.values())

        ss.languageVersion.set(target.KOTLIN_API)
        ss.jdkVersion.set(target.JVM_TARGET_INT)

        // include all project sources when resolving kdoc samples
        ss.samples.setFrom(target.fileTree(target.file("src")))

        if (!target.isRootProject()) {
          val readmeFile = target.projectDir.resolve("README.md")
          if (readmeFile.exists()) {
            ss.includes.from(readmeFile)
          }
        }

        ss.sourceLink { spec ->

          // spec.localDirectory.files(kotlinSS.kotlin.sourceDirectories)
          spec.localDirectory.files("src/$ssName")

          val modulePath = target.path.replace(":", "/")
            .replaceFirst("/", "")

          // URL showing where the source code can be accessed through the web browser
          spec.remoteUrl("${SOURCE_WEBSITE}/blob/main/$modulePath/src/$ssName")
          // Suffix which is used to append the line number to the URL. Use #L for GitHub
          spec.remoteLineSuffix.set("#L")
        }
      }

      target.tasks.withType(DokkaBaseTask::class.java).configureEach { task ->

        if (target.isRootProject()) {
          task.dependsOn("unzipDokkaArchives")
        }

        // Dokka uses their outputs but doesn't explicitly depend upon them.
        // task.mustRunAfter(target.tasks.withType(KotlinCompile::class.java))
        // task.mustRunAfter(target.tasks.withType(KtLintTask::class.java))
      }

      if (target.isRootProject()) {

        val config = target.configurations.getByName("dokka")

        config.dependencies.addAllLater(
          target.provider {
            target.subprojects
              .filter { sub -> sub.subprojects.isEmpty() }
              .map { sub -> target.projectDependency(sub.path) }
          }
        )

        target.dependencies.add("dokkaPlugin", target.libs.dokka.versioning)

        val dokkaArchiveBuildDir = target.rootProject.layout
          .buildDirectory
          .dir("tmp/dokka-archive")

        dokka.pluginsConfiguration.withType<DokkaVersioningPluginParameters>().configureEach { versioning ->
          versioning.version.set(target.VERSION_NAME)
          versioning.olderVersionsDir.set(dokkaArchiveBuildDir)
          versioning.renderVersionsNavigationOnAllPages.set(true)
        }

        // dokka.dokkatooPublications.configureEach {
        //   it.suppressObviousFunctions.set(true)
        // }
      }
    }

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
    const val DOKKA_HTML_TASK_NAME = "dokkaGenerate"
  }
}
