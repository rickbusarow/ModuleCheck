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

import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.tasks.Copy
import org.gradle.api.tasks.Exec
import java.io.File

abstract class WebsitePlugin : Plugin<Project> {
  override fun apply(target: Project) {

    target.checkProjectIsRoot()

    /**
     * Looks for all references to ModuleCheck artifacts in the md/mdx files in the un-versioned
     * /website/docs. Updates all versions to the pre-release version.
     */
    target.tasks.register("checkWebsiteNextDocsVersionRefs") { task ->

      task.description = "Checks the \"next\" version docs for version changes"
      task.group = "website"

      task.doLast {

        target.fileTree("${target.rootDir}/website/docs") {
          it.include("**/*.md*")
        }
          .forEach { file ->
            file.updateModuleCheckVersionRef(VERSION_NAME, failOnChanges = true)
          }
      }
    }

    /**
     * Looks for all references to ModuleCheck artifacts in the md/mdx files in the un-versioned
     * /website/docs. Updates all versions to the pre-release version.
     */
    target.tasks.register("updateWebsiteNextDocsVersionRefs") { task ->

      task.description = "Updates the \"next\" version docs for version changes"
      task.group = "website"

      task.doLast {

        target.fileTree("${target.rootDir}/website/docs") {
          it.include("**/*.md*")
        }
          .forEach { file ->
            file.updateModuleCheckVersionRef(VERSION_NAME, failOnChanges = false)
          }
      }
    }

    /** Updates the "moduleCheck" version in package.json. */
    target.tasks.register("updateWebsitePackageJsonVersion") { task ->

      task.description = "Updates the \"ModuleCheck\" version in package.json"
      task.group = "website"

      task.doLast {

        // this isn't very robust, but it's fine for this use-case
        val versionReg = """(\s*"version"\s*:\s*")[^"]*("\s*,)""".toRegex()

        // just in case some child object gets a "version" field, ignore it.
        // This only works if the correct field comes first (which it does).
        var foundOnce = false

        with(File("${target.rootDir}/website/package.json")) {
          val newText = readText()
            .lines()
            .joinToString("\n") { line ->

              line.replace(versionReg) { matchResult ->

                if (!foundOnce) {
                  foundOnce = true
                  val (prefix, suffix) = matchResult.destructured
                  "$prefix$VERSION_NAME$suffix"
                } else {
                  line
                }
              }
            }
          writeText(newText)
        }
      }
    }

    /** Updates the "moduleCheck" version in package.json. */
    target.tasks.register("checkWebsitePackageJsonVersion") { task ->

      task.description = "Checks the \"ModuleCheck\" version in package.json"
      task.group = "website"

      task.doLast {

        // this isn't very robust, but it's fine for this use-case
        val versionReg = """(\s*"version"\s*:\s*")[^"]*("\s*,)""".toRegex()

        // just in case some child object gets a "version" field, ignore it.
        // This only works if the correct field comes first (which it does).
        var foundOnce = false

        with(File("${target.rootDir}/website/package.json")) {
          val oldText = readText()
          val newText = oldText
            .lines()
            .joinToString("\n") { line ->

              line.replace(versionReg) { matchResult ->

                if (!foundOnce) {
                  foundOnce = true
                  val (prefix, suffix) = matchResult.destructured
                  "$prefix$VERSION_NAME$suffix"
                } else {
                  line
                }
              }
            }
          require(oldText == newText) {
            "The website package.json version is out of date.  " +
              "Run `./gradlew updateWebsitePackageJsonVersion` to update."
          }
        }
      }
    }

    /**
     * Looks for all references to ModuleCheck artifacts in the project README.md to the current
     * released version.
     */
    target.tasks.register("checkProjectReadmeVersionRefs") { task ->

      task.description =
        "Updates the project-level README to use the latest published version in maven coordinates"
      task.group = "documentation"

      task.doLast {

        File("${target.rootDir}/README.md")
          .updateModuleCheckVersionRef(
            VERSION_NAME,
            failOnChanges = true,
            "updateProjectReadmeVersionRefs"
          )
      }
    }

    /**
     * Looks for all references to ModuleCheck artifacts in the project README.md to the current
     * released version.
     */
    target.tasks.register("updateProjectReadmeVersionRefs") { task ->

      task.description = "Checks the project-level README for version changes"
      task.group = "documentation"

      task.doLast {

        File("${target.rootDir}/README.md")
          .updateModuleCheckVersionRef(VERSION_NAME, failOnChanges = false)
      }
    }

    target.tasks.register("yarnInstall", Exec::class.java) { task ->

      task.description = "runs `yarn install` for the website"
      task.group = "website"

      task.workingDir("./website")
      task.commandLine("yarn", "install")
    }

    target.tasks.register("updateWebsiteApiDocs", Copy::class.java) { task ->

      task.description = "creates new Dokka api docs and copies them to the website's static dir"
      task.group = "website"

      task.doFirst {
        target.delete(
          target.fileTree("./website/static/api") {
            it.exclude("**/styles/*")
          }
        )
      }

      task.dependsOn(target.tasks.findByName("knit"))

      task.from(
        target.fileTree("${target.buildDir}/dokka/htmlMultiModule") {
          it.exclude("**/styles/*")
        }
      )

      task.into("./website/static/api")
    }

    val updateWebsiteChangelog =
      target.tasks.register("updateWebsiteChangelog", Copy::class.java) { task ->

        task.description =
          "copies the root project's CHANGELOG to the website and updates its formatting"
        task.group = "website"

        task.from("changelog.md")
        task.into("${target.rootDir}/website/src/pages")

        task.doLast {

          // add one hashmark to each header, because GitHub and Docusaurus render them differently
          val changelog = File("${target.rootDir}/website/src/pages/changelog.md")

          val newText = changelog.readText()
            .lines()
            .joinToString("\n") { line ->
              line.replace("^(#+) (.*)".toRegex()) { matchResult ->
                val (hashes, text) = matchResult.destructured

                "$hashes# $text"
              }
                // relativize all links?
                .replace("https://rbusarow.github.io/ModuleCheck", "")
            }

          require(!newText.contains("http://localhost:3000")) {
            "Don't forget to remove the hard-coded local development site root " +
              "(`http://localhost:3000`) from the ChangeLog."
          }

          changelog.writeText(newText)
        }
      }

    val versionDocs = target.tasks.register("versionDocs", Exec::class.java) { task ->

      task.description = "creates a new version snapshot of website docs, " +
        "using the current version defined in gradle.properties"
      task.group = "website"

      val existingVersions = with(File("${target.rootDir}/website/versions.json")) {
        "\"([^\"]*)\"".toRegex()
          .findAll(readText())
          .flatMap { it.destructured.toList() }
      }

      val devVersions = ".*(?:-SNAPSHOT|-LOCAL)".toRegex()

      val version = VERSION_NAME

      task.enabled = version !in existingVersions && !version.matches(devVersions)

      task.workingDir("${target.rootDir}/website")
      task.commandLine("yarn", "run", "docusaurus", "docs:version", version)
    }

    target.tasks.register("startSite", Exec::class.java) { task ->

      task.description = "launches the local development website"
      task.group = "website"

      task.dependsOn(
        "yarnInstall",
        versionDocs,
        "updateWebsiteApiDocs",
        updateWebsiteChangelog,
        "updateWebsiteNextDocsVersionRefs",
        "updateWebsitePackageJsonVersion"
      )

      task.workingDir("${target.rootDir}/website")
      task.commandLine("yarn", "run", "start")
    }

    target.tasks.register("buildSite", Exec::class.java) { task ->

      task.description = "builds the website"
      task.group = "website"

      task.dependsOn(
        "yarnInstall",
        versionDocs,
        "updateWebsiteApiDocs",
        updateWebsiteChangelog,
        "updateWebsiteNextDocsVersionRefs",
        "updateWebsitePackageJsonVersion"
      )

      task.workingDir("${target.rootDir}/website")
      task.commandLine("yarn", "run", "build")
    }
  }

  private fun File.updateModuleCheckVersionRef(
    version: String,
    failOnChanges: Boolean,
    updateTaskName: String = ""
  ) {
    val group = GROUP

    val pluginId = PLUGIN_ID

    val pluginRegex =
      """^([^'"\n]*['"])$pluginId[^'"]*(['"].*) version (['"])[^'"]*(['"].*)${'$'}""".toRegex()
    val moduleRegex = """^([^'"\n]*['"])$group:([^:]*):[^'"]*(['"].*)${'$'}""".toRegex()

    val oldText = readText()

    val newText = oldText
      .lines()
      .joinToString("\n") { line ->
        line
          .replace(pluginRegex) { matchResult ->

            val (preId, postId, preVersion, postVersion) = matchResult.destructured

            "$preId$pluginId$postId version $preVersion$version$postVersion"
          }
          .replace(moduleRegex) { matchResult ->

            val (config, module, suffix) = matchResult.destructured

            "$config$group:$module:$version$suffix"
          }
      }

    require(oldText == newText || !failOnChanges) {
      "ModuleCheck version references in $path are out of date.  " +
        "Run `./gradlew $updateTaskName` to update."
    }

    writeText(newText)
  }
}
