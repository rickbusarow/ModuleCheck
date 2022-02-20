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

import com.vanniktech.maven.publish.JavadocJar.Dokka
import com.vanniktech.maven.publish.KotlinJvm
import com.vanniktech.maven.publish.MavenPublishBaseExtension
import com.vanniktech.maven.publish.SonatypeHost.DEFAULT
import org.gradle.api.Project
import org.gradle.api.publish.PublishingExtension
import org.gradle.api.publish.maven.MavenPublication
import org.gradle.kotlin.dsl.apply

const val GROUP = "com.rickbusarow.modulecheck"
const val PLUGIN_ID = "com.rickbusarow.module-check"
const val VERSION_NAME = "0.11.4-SNAPSHOT"

@Suppress("UnstableApiUsage")
fun Project.configurePublishing(
  artifactId: String
) {

  apply(plugin = "com.vanniktech.maven.publish.base")
  apply(plugin = "mcbuild.dokka")

  group = GROUP
  version = VERSION_NAME

  extensions
    .configure(MavenPublishBaseExtension::class.java) {

      publishToMavenCentral(DEFAULT)
      signAllPublications()
      pom {
        description.set("Fast dependency graph linting for Gradle projects")
        name.set(artifactId)
        url.set("https://github.com/rbusarow/ModuleCheck")
        licenses {
          license {
            name.set("The Apache Software License, Version 2.0")
            url.set("https://www.apache.org/licenses/LICENSE-2.0.txt")
            distribution.set("repo")
          }
        }
        scm {
          url.set("https://github.com/rbusarow/ModuleCheck/")
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

      configure(KotlinJvm(javadocJar = Dokka(taskName = "dokkaHtml"), sourcesJar = true))
    }

  extensions
    .configure(PublishingExtension::class.java) {

      publications
        .filterIsInstance<MavenPublication>()
        .forEach { it.artifactId = artifactId }
    }
}
