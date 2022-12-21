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

import com.vanniktech.maven.publish.GradlePlugin
import com.vanniktech.maven.publish.JavadocJar.Dokka
import com.vanniktech.maven.publish.KotlinJvm
import com.vanniktech.maven.publish.MavenPublishBaseExtension
import com.vanniktech.maven.publish.SonatypeHost.DEFAULT
import com.vanniktech.maven.publish.tasks.JavadocJar
import com.vanniktech.maven.publish.tasks.SourcesJar
import org.gradle.api.Project
import org.gradle.api.publish.PublishingExtension
import org.gradle.api.publish.maven.MavenPublication
import org.gradle.api.publish.maven.tasks.PublishToMavenRepository
import org.gradle.api.tasks.bundling.Jar
import org.gradle.plugins.signing.Sign
import org.jetbrains.dokka.gradle.AbstractDokkaLeafTask

@Suppress("UnstableApiUsage")
fun Project.configurePublishing(
  artifactId: String
) {
  plugins.applyOnce("com.vanniktech.maven.publish.base")

  version = VERSION_NAME

  var skipDokka = false

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

    if (pluginManager.hasPlugin("java-gradle-plugin")) {
      extension.configure(
        GradlePlugin(
          javadocJar = Dokka(taskName = "dokkaHtml"),
          sourcesJar = true
        )
      )
    } else {
      extension.configure(KotlinJvm(javadocJar = Dokka(taskName = "dokkaHtml"), sourcesJar = true))
    }
  }

  extensions.configure(PublishingExtension::class.java) {
    it.publications
      .filterIsInstance<MavenPublication>()
      .forEach { publication ->
        publication.groupId = GROUP
        publication.artifactId = artifactId
      }
  }

  tasks.register("checkVersionIsSnapshot") {
    it.doLast {
      val expected = "-SNAPSHOT"
      require(VERSION_NAME.endsWith(expected)) {
        "The project's version name must be suffixed with `$expected` when checked in" +
          " to the main branch, but instead it's `$VERSION_NAME`."
      }
    }
  }

  tasks.withType(PublishToMavenRepository::class.java).configureEach {
    it.notCompatibleWithConfigurationCache("See https://github.com/gradle/gradle/issues/13468")
  }

  tasks.withType(Jar::class.java) { it.notCompatibleWithConfigurationCache("") }
  tasks.withType(SourcesJar::class.java) { it.notCompatibleWithConfigurationCache("") }
  tasks.withType(JavadocJar::class.java) { it.notCompatibleWithConfigurationCache("") }
  tasks.withType(Sign::class.java) {
    it.notCompatibleWithConfigurationCache("")
    // skip signing for -LOCAL and -SNAPSHOT publishing
    it.onlyIf {
      !VERSION_NAME.endsWith("SNAPSHOT") && !VERSION_NAME.endsWith("LOCAL")
    }
  }
  tasks.withType(AbstractDokkaLeafTask::class.java) {
    it.onlyIf { !skipDokka }
  }

  // Integration tests require `publishToMavenLocal`, but they definitely don't need Dokka output,
  // and generating kdoc for everything takes forever -- especially on a GitHub Actions server.
  // So for integration tests, skip Dokka tasks.
  val publishToMavenLocalNoDokka = tasks.register("publishToMavenLocalNoDokka") {
    it.notCompatibleWithConfigurationCache("")
    it.doFirst { skipDokka = true }

    it.finalizedBy("publishToMavenLocal")
  }

  tasks.matchingName("publishToMavenLocal").configureEach {
    it.mustRunAfter(publishToMavenLocalNoDokka)
  }
}
