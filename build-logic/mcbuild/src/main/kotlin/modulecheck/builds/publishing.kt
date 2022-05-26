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
import org.gradle.kotlin.dsl.apply
import org.gradle.kotlin.dsl.configure
import org.gradle.plugins.signing.Sign

const val GROUP = "com.rickbusarow.modulecheck"
const val PLUGIN_ID = "com.rickbusarow.module-check"
const val VERSION_NAME = "0.12.2"
const val SOURCE_WEBSITE = "https://github.com/rbusarow/ModuleCheck"
const val DOCS_WEBSITE = "https://rbusarow.github.io/ModuleCheck"

@Suppress("UnstableApiUsage")
fun Project.configurePublishing(
  artifactId: String
) {
  apply(plugin = "com.vanniktech.maven.publish.base")
  apply(plugin = "mcbuild.dokka")

  version = VERSION_NAME

  configure<MavenPublishBaseExtension> {
    publishToMavenCentral(DEFAULT)
    signAllPublications()
    pom {
      description.set("Fast dependency graph linting for Gradle projects")
      name.set(artifactId)
      url.set(SOURCE_WEBSITE)
      licenses {
        license {
          name.set("The Apache Software License, Version 2.0")
          url.set("https://www.apache.org/licenses/LICENSE-2.0.txt")
          distribution.set("repo")
        }
      }
      scm {
        url.set("$SOURCE_WEBSITE/")
        connection.set("scm:git:git://github.com/rbusarow/ModuleCheck.git")
        developerConnection.set("scm:git:ssh://git@github.com/rbusarow/ModuleCheck.git")
      }
      developers {
        developer {
          id.set("rbusarow")
          name.set("Rick Busarow")
        }
      }
    }

    if (pluginManager.hasPlugin("java-gradle-plugin")) {
      configure(GradlePlugin(javadocJar = Dokka(taskName = "dokkaHtml"), sourcesJar = true))
    } else {
      configure(KotlinJvm(javadocJar = Dokka(taskName = "dokkaHtml"), sourcesJar = true))
    }
  }

  configure<PublishingExtension> {
    publications
      .filterIsInstance<MavenPublication>()
      .forEach {
        it.groupId = GROUP
        it.artifactId = artifactId
      }
  }

  tasks.register("checkVersionIsSnapshot") {
    doLast {
      val expected = "-SNAPSHOT"
      require(VERSION_NAME.endsWith(expected)) {
        "The project's version name must be suffixed with `$expected` when checked in" +
          " to the main branch, but instead it's `$VERSION_NAME`."
      }
    }
  }

  tasks.withType(PublishToMavenRepository::class.java).configureEach {
    notCompatibleWithConfigurationCache("See https://github.com/gradle/gradle/issues/13468")
  }

  tasks.withType(Jar::class.java).configureEach {
    notCompatibleWithConfigurationCache("")
  }
  tasks.withType(SourcesJar::class.java).configureEach {
    notCompatibleWithConfigurationCache("")
  }
  tasks.withType(JavadocJar::class.java).configureEach {
    notCompatibleWithConfigurationCache("")
  }
  tasks.withType(Sign::class.java).configureEach {
    notCompatibleWithConfigurationCache("")
  }
}
