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

import com.vanniktech.maven.publish.GradlePlugin
import com.vanniktech.maven.publish.JavadocJar.Dokka
import com.vanniktech.maven.publish.KotlinJvm
import com.vanniktech.maven.publish.MavenPublishBaseExtension
import com.vanniktech.maven.publish.SonatypeHost.Companion.DEFAULT
import kotlinx.validation.ApiValidationExtension
import org.gradle.api.NamedDomainObjectProvider
import org.gradle.api.Project
import org.gradle.api.publish.PublishingExtension
import org.gradle.api.publish.maven.MavenPublication
import org.gradle.api.publish.maven.tasks.PublishToMavenRepository
import org.gradle.api.tasks.bundling.Jar
import org.gradle.language.base.plugins.LifecycleBasePlugin
import org.gradle.plugin.devel.GradlePluginDevelopmentExtension
import org.gradle.plugin.devel.PluginDeclaration
import org.gradle.plugins.signing.Sign
import org.jetbrains.dokka.gradle.AbstractDokkaLeafTask

interface PublishingExtension {

  fun Project.published(
    artifactId: String,
    pomDescription: String = "Fast dependency graph linting for Gradle projects"
  ) {

    plugins.apply("com.vanniktech.maven.publish.base")
    plugins.apply("mcbuild.dokka")

    require(pluginManager.hasPlugin("org.jetbrains.kotlin.jvm"))

    configurePublish(
      artifactId = artifactId,
      pomDescription = pomDescription,
      groupId = GROUP
    )
  }

  fun Project.publishedPlugin(
    pluginDeclaration: NamedDomainObjectProvider<PluginDeclaration>,
    groupId: String = GROUP
  ) {

    plugins.apply("com.vanniktech.maven.publish.base")
    plugins.apply("mcbuild.dokka")

    require(pluginManager.hasPlugin("org.jetbrains.kotlin.jvm"))
    require(pluginManager.hasPlugin("java-gradle-plugin"))

    plugins.apply("com.gradle.plugin-publish")

    configurePublishPlugin(groupId, pluginDeclaration)
  }
}

private fun Project.configurePublishPlugin(
  groupId: String,
  pluginDeclaration: NamedDomainObjectProvider<PluginDeclaration>
) {
  applyBinaryCompatibility()

  group = groupId

  plugins.withId("com.gradle.plugin-publish") {

    pluginDeclaration.configure { declaration ->

      requireNotNull(declaration.description) { "A plugin description is required." }

      extensions.configure(GradlePluginDevelopmentExtension::class.java) { pluginDevelopmentExtension ->

        @Suppress("UnstableApiUsage")
        pluginDevelopmentExtension.website.set(SOURCE_WEBSITE)
        @Suppress("UnstableApiUsage")
        pluginDevelopmentExtension.vcsUrl.set("$SOURCE_WEBSITE.git")
      }
    }
  }
}

fun Project.applyBinaryCompatibility() {

  pluginManager.apply("org.jetbrains.kotlinx.binary-compatibility-validator")

  extensions.configure(ApiValidationExtension::class.java) { extension ->

    // Packages that are excluded from public API dumps even if they contain public API
    extension.ignoredPackages = mutableSetOf("sample", "samples")

    // Subprojects that are excluded from API validation
    extension.ignoredProjects = mutableSetOf()
  }

  tasks.matchingName("apiCheck").configureEach { task ->
    task.mustRunAfter("apiDump")
  }
}

private fun Project.versionIsSnapshot(): Boolean {
  return VERSION_NAME.endsWith("-SNAPSHOT")
}

private fun Project.configurePublish(
  artifactId: String,
  pomDescription: String,
  groupId: String
) {

  version = VERSION_NAME

  this@configurePublish.artifactId = artifactId

  if (!versionIsSnapshot()) {
    configureArtifactory()
  }

  @Suppress("UnstableApiUsage")
  extensions.configure(MavenPublishBaseExtension::class.java) { extension ->

    extension.publishToMavenCentral(DEFAULT, automaticRelease = true)

    extension.signAllPublications()

    extension.pom { mavenPom ->
      mavenPom.description.set("Fast dependency graph linting for Gradle projects")
      mavenPom.name.set(artifactId)
      mavenPom.url.set(SOURCE_WEBSITE)
      mavenPom.licenses { licenseSpec ->
        licenseSpec.license { pomLicense ->
          pomLicense.name.set("The Apache Software License, Version 2.0")
          pomLicense.url.set("https://www.apache.org/licenses/LICENSE-2.0.txt")
          pomLicense.distribution.set("repo")
        }
      }
      mavenPom.scm { scm ->
        scm.url.set("$SOURCE_WEBSITE/")
        scm.connection.set("scm:git:git://github.com/rbusarow/ModuleCheck.git")
        scm.developerConnection.set("scm:git:ssh://git@github.com/rbusarow/ModuleCheck.git")
      }
      mavenPom.developers { developerSpec ->
        developerSpec.developer { developer ->
          developer.id.set("rbusarow")
          developer.name.set("Rick Busarow")
        }
      }
    }

    when {
      // The plugin-publish plugin handles its artifacts
      pluginManager.hasPlugin("com.gradle.plugin-publish") -> {}

      // handle publishing plugins if they're not going to the plugin portal
      pluginManager.hasPlugin("java-gradle-plugin") -> {
        extension.configure(
          GradlePlugin(
            javadocJar = Dokka(taskName = "dokkaHtml"),
            sourcesJar = true
          )
        )
      }

      pluginManager.hasPlugin("com.github.johnrengelman.shadow") -> {
        extension.configure(
          KotlinJvm(
            javadocJar = Dokka(taskName = "dokkaHtml"),
            sourcesJar = true
          )
        )
        applyBinaryCompatibility()
      }

      else -> {
        extension.configure(
          KotlinJvm(
            javadocJar = Dokka(taskName = "dokkaHtml"),
            sourcesJar = true
          )
        )
        applyBinaryCompatibility()
      }
    }

    extensions.configure(PublishingExtension::class.java) { publishingExtension ->
      publishingExtension.publications.withType(MavenPublication::class.java) { publication ->
        publication.artifactId = artifactId
        publication.pom.description.set(pomDescription)
        publication.groupId = groupId
      }
    }
  }

  registerCoordinatesStringsCheckTask(groupId = groupId, artifactId = artifactId)
  registerSnapshotVersionCheckTask()
  configureSkipDokka()

  tasks.withType(PublishToMavenRepository::class.java) {
    it.notCompatibleWithConfigurationCache("See https://github.com/gradle/gradle/issues/13468")
  }
  tasks.withType(Jar::class.java) {
    it.notCompatibleWithConfigurationCache("")
  }
  tasks.withType(Sign::class.java) {
    it.notCompatibleWithConfigurationCache("")
    // skip signing for -SNAPSHOT publishing
    it.onlyIf { !(version as String).endsWith("SNAPSHOT") }
  }
}

private fun Project.configureArtifactory() {
  extensions.configure(PublishingExtension::class.java) { extension ->
    extension.repositories { repositoryHandler ->

      repositoryHandler.maven { repository ->
        repository.name = "artifactory"

        repository.url = if (versionIsSnapshot()) {
          error("need snapshot permissions for artifactory")
          // uri("https://maven.global.square/artifactory/snapshots/")
        } else {
          uri("https://maven.global.square/artifactory/releases/")
        }

        repository.credentials { credentials ->
          credentials.username = project.property("SQUARE_NEXUS_USERNAME") as String
          credentials.password = project.property("SQUARE_NEXUS_PASSWORD") as String
        }
      }
    }
  }
}

private fun Project.registerCoordinatesStringsCheckTask(
  groupId: String,
  artifactId: String
) {

  val checkTask = tasks.registerOnce(
    "checkMavenCoordinatesStrings",
    ModuleCheckBuildTask::class.java
  ) { task ->
    task.group = "publishing"
    task.description = "checks that the project's maven group and artifact ID are valid for Maven"

    task.doLast {

      val allowedRegex = "^[A-Za-z0-9_\\-.]+$".toRegex()

      check(groupId.matches(allowedRegex)) {

        val actualString = when {
          groupId.isEmpty() -> "<<empty string>>"
          else -> groupId
        }
        "groupId ($actualString) is not a valid Maven identifier ($allowedRegex)."
      }

      check(artifactId.matches(allowedRegex)) {

        val actualString = when {
          artifactId.isEmpty() -> "<<empty string>>"
          else -> artifactId
        }
        "artifactId ($actualString) is not a valid Maven identifier ($allowedRegex)."
      }
    }
  }

  tasks.named(LifecycleBasePlugin.CHECK_TASK_NAME) { task ->
    task.dependsOn(checkTask)
  }
}

private fun Project.registerSnapshotVersionCheckTask() {
  tasks.registerOnce(
    "checkVersionIsSnapshot",
    ModuleCheckBuildTask::class.java
  ) { task ->
    task.group = "publishing"
    task.description = "ensures that the project version has a -SNAPSHOT suffix"
    val versionString = version as String
    task.doLast {
      val expected = "-SNAPSHOT"
      require(versionString.endsWith(expected)) {
        "The project's version name must be suffixed with `$expected` when checked in" +
          " to the main branch, but instead it's `$versionString`."
      }
    }
  }
  tasks.registerOnce("checkVersionIsNotSnapshot", ModuleCheckBuildTask::class.java) { task ->
    task.group = "publishing"
    task.description = "ensures that the project version does not have a -SNAPSHOT suffix"
    val versionString = version as String
    task.doLast {
      require(!versionString.endsWith("-SNAPSHOT")) {
        "The project's version name cannot have a -SNAPSHOT suffix, but it was $versionString."
      }
    }
  }
}

/**
 * Integration tests require `publishToMavenLocal`, but they definitely don't need
 * Dokka output, and generating kdoc for everything takes forever -- especially
 * on a GitHub Actions server. So for integration tests, skip Dokka tasks.
 */
private fun Project.configureSkipDokka() {

  if (tasks.names.contains("setSkipDokka")) {
    return
  }

  var skipDokka = false
  val setSkipDokka = tasks.register(
    "setSkipDokka",
    ModuleCheckBuildTask::class.java
  ) { task ->

    task.group = "publishing"
    task.description = "sets `skipDokka` to true before `publishToMavenLocal` is evaluated."

    task.doFirst { skipDokka = true }
    task.onlyIf { true }
  }

  tasks.register("publishToMavenLocalNoDokka", ModuleCheckBuildTask::class.java) {

    it.group = "publishing"
    it.description = "Delegates to `publishToMavenLocal`, " +
      "but skips Dokka generation and does not include a javadoc .jar."

    it.doFirst { skipDokka = true }
    it.dependsOn(setSkipDokka)
    it.onlyIf { true }
    it.dependsOn("publishToMavenLocal")
  }

  tasks.matching { it.name == "javaDocReleaseGeneration" }.configureEach {
    it.onlyIf { !skipDokka }
  }
  tasks.withType(AbstractDokkaLeafTask::class.java) {
    it.onlyIf { !skipDokka }
  }

  tasks.named("publishToMavenLocal") {
    it.mustRunAfter(setSkipDokka)
  }
}
