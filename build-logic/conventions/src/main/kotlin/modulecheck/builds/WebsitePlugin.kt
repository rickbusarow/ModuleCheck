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

import com.rickbusarow.kgx.checkProjectIsRoot
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.tasks.Exec
import org.gradle.api.tasks.Sync
import java.io.File

abstract class WebsitePlugin : Plugin<Project> {

  private val Project.readme: File
    get() = file("README.md")

  private val Project.changelog: File
    get() = file("CHANGELOG.md")

  private val Project.websiteDir: File
    get() = rootDir.resolve("website")

  private val Project.websiteDocsDir: File
    get() = websiteDir.resolve("docs")

  private val Project.websiteApiDir: File
    get() = websiteDir.resolve("static/api")

  private val Project.websiteSrcDir: File
    get() = websiteDir.resolve("src")

  private val Project.websitePagesDir: File
    get() = websiteSrcDir.resolve("pages")

  private val Project.websiteChangelog: File
    get() = websitePagesDir.resolve("changelog.md")

  private val Project.websitePackageJson: File
    get() = websiteDir.resolve("package.json")

  override fun apply(target: Project) {

    target.checkProjectIsRoot()

    /**
     * Looks for all references to ModuleCheck artifacts in the md/mdx files in the
     * un-versioned /website/docs. Updates all versions to the pre-release version.
     */
    target.tasks.register("checkWebsiteNextDocsVersionRefs") { task ->

      task.description = "Checks the \"next\" version docs for version changes"
      task.group = "website"

      task.doLast {

        target.fileTree(target.websiteDocsDir) {
          it.include("**/*.md*")
        }
          .forEach { file ->
            file.updateModuleCheckVersionRef(VERSION_NAME, failOnChanges = true)
          }
      }
    }

    /**
     * Looks for all references to ModuleCheck artifacts in the md/mdx files in the
     * un-versioned /website/docs. Updates all versions to the pre-release version.
     */
    target.tasks.register("updateWebsiteNextDocsVersionRefs") { task ->

      task.description = "Updates the \"next\" version docs for version changes"
      task.group = "website"

      task.doLast {

        target.fileTree(target.websiteDocsDir) {
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

        val newText = target.websitePackageJson.readText().updatePackageJsonVersion()

        target.websitePackageJson.writeText(newText)
      }
    }

    /** Updates the "moduleCheck" version in package.json. */
    target.tasks.register("checkWebsitePackageJsonVersion") { task ->

      task.description = "Checks the \"ModuleCheck\" version in package.json"
      task.group = "website"

      task.doLast {

        val oldText = target.websitePackageJson.readText()
        val newText = oldText.updatePackageJsonVersion()
        require(oldText == newText) {
          "The website package.json version is out of date.  " +
            "Run `./gradlew updateWebsitePackageJsonVersion` to update."
        }
      }
    }

    val updateVersionRefs = target.tasks
      .register("updateProjectReadmeVersionRefs") { task ->

        task.description = "Checks the project-level README for version changes"
        task.group = "documentation"

        task.doLast {

          target.readme.updateModuleCheckVersionRef(version = VERSION_NAME, failOnChanges = false)
        }
      }

    target.tasks.register("checkProjectReadmeVersionRefs") { task ->

      task.description =
        "Updates the project-level README to use the latest published version in maven coordinates"
      task.group = "documentation"

      task.mustRunAfter(updateVersionRefs)

      task.doLast {

        target.readme.updateModuleCheckVersionRef(
          version = VERSION_NAME,
          failOnChanges = true,
          updateTaskName = "updateProjectReadmeVersionRefs"
        )
      }
    }

    target.tasks.register("pnpmInstall", Exec::class.java) { task ->

      task.description = "runs `pnpm install` for the website"
      task.group = "website"

      task.workingDir(target.websiteDir)
      task.commandLine("pnpm", "install")
    }

    target.tasks.register("updateWebsiteApiDocs", Sync::class.java) { task ->

      task.description = "creates new Dokka api docs and copies them to the website's static dir"
      task.group = "website"

      task.dependsOn(target.tasks.findByName("knit"))

      task.from(target.tasks.named(DokkaConventionPlugin.DOKKA_HTML_TASK_NAME))

      task.into(target.websiteApiDir)
    }

    val updateWebsiteChangelog =
      target.tasks.register("updateWebsiteChangelog") { task ->

        task.description =
          "copies the root project's CHANGELOG to the website and updates its formatting"
        task.group = "website"

        task.inputs.file(target.changelog)
        task.outputs.file(target.websiteChangelog)

        task.doLast {

          // add one hash mark to each header, because GitHub and Docusaurus render them differently
          val newText = target.changelog.readText()
            .lines()
            .joinToString("\n") { line ->
              line.replace("^(#+) (.*)".toRegex()) { matchResult ->
                val (hashes, text) = matchResult.destructured

                "$hashes# $text"
              }
                // relativize all links?
                .remove(DOCS_WEBSITE)
            }

          require(!newText.contains("http://localhost:3000")) {
            "Don't forget to remove the hard-coded local development site root " +
              "(`http://localhost:3000`) from the ChangeLog."
          }

          target.websiteChangelog.writeText(newText)
        }
      }

    val versionDocs = target.tasks.register("versionDocs", Exec::class.java) { task ->

      task.description = "creates a new version snapshot of website docs, " +
        "using the current version defined in gradle.properties"
      task.group = "website"

      val existingVersions =
        "\"([^\"]*)\"".toRegex()
          .findAll(target.websitePackageJson.readText())
          .flatMap { it.destructured.toList() }

      val devVersions = ".*(?:-SNAPSHOT|-LOCAL)".toRegex()

      val version = VERSION_NAME

      task.enabled = version !in existingVersions && !version.matches(devVersions)

      task.workingDir(target.websiteDir)
      task.commandLine("pnpm", "docusaurus", "docs:version", version)
    }

    target.tasks.register("startSite", Exec::class.java) { task ->

      task.description = "launches the local development website"
      task.group = "website"

      task.dependsOn(
        "pnpmInstall",
        versionDocs,
        "updateWebsiteApiDocs",
        updateWebsiteChangelog,
        "updateWebsiteNextDocsVersionRefs",
        "updateWebsitePackageJsonVersion"
      )

      task.workingDir(target.websiteDir)
      task.commandLine("pnpm", "start")
    }

    target.tasks.register("buildSite", Exec::class.java) { task ->

      task.description = "builds the website"
      task.group = "website"

      task.dependsOn(
        "pnpmInstall",
        versionDocs,
        "updateWebsiteApiDocs",
        updateWebsiteChangelog,
        "updateWebsiteNextDocsVersionRefs",
        "updateWebsitePackageJsonVersion"
      )

      task.workingDir(target.websiteDir)
      task.commandLine("pnpm", "build")
    }
  }

  private fun String.updatePackageJsonVersion(): String {
    // this isn't very robust, but it's fine for this use-case
    val versionReg = """(\s*"version"\s*:\s*")[^"]*("\s*,)""".toRegex()

    // just in case some child object gets a "version" field, ignore it.
    // This only works if the correct field comes first (which it does).
    var foundOnce = false

    return lineSequence()
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
