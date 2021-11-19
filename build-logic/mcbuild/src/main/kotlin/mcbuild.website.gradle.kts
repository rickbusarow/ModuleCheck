/*
 * Copyright (C) 2021 Rick Busarow
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

import modulecheck.builds.GROUP
import modulecheck.builds.PLUGIN_ID
import modulecheck.builds.VERSION_NAME

/**
 * Looks for all references to ModuleCheck artifacts in the md/mdx files
 * in the un-versioned /website/docs. Updates all versions to the pre-release version.
 */
val updateWebsiteNextDocsVersionRefs by tasks.registering {

  description = "Updates the \"next\" version docs to use the next artifact version"
  group = "website"

  doLast {

    fileTree("$rootDir/website/docs") {
      include("**/*.md*")
    }
      .forEach { file ->
        file.updateModuleCheckVersionRef(VERSION_NAME)
      }
  }
}

/**
 * Updates the "moduleCheck" version in package.json
 */
val updateWebsitePackageJsonVersion by tasks.registering {

  description = "Updates the \"ModuleCheck\" version in package.json"
  group = "website"

  doLast {

    // this isn't very robust, but it's fine for this use-case
    val versionReg = """(\s*"version"\s*:\s*")[^"]*("\s*,)""".toRegex()

    // just in case some child object gets a "version" field, ignore it.
    // This only works if the correct field comes first (which it does).
    var foundOnce = false

    with(File("$rootDir/website/package.json")) {
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

/**
 * Looks for all references to ModuleCheck artifacts in the project README.md
 * to the current released version.
 */
val updateProjectReadmeVersionRefs by tasks.registering {

  description =
    "Updates the project-level README to use the latest published version in maven coordinates"
  group = "documentation"

  doLast {

    File("$rootDir/README.md")
      .updateModuleCheckVersionRef(VERSION_NAME)
  }
}

fun File.updateModuleCheckVersionRef(version: String) {

  val group = GROUP

  val pluginId = PLUGIN_ID

  val pluginRegex =
    """^([^'"\n]*['"])$pluginId[^'"]*(['"].*) version (['"])[^'"]*(['"].*)${'$'}""".toRegex()
  val moduleRegex = """^([^'"\n]*['"])$group:([^:]*):[^'"]*(['"].*)${'$'}""".toRegex()

  val newText = readText()
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

  writeText(newText)
}

val startSite by tasks.registering(Exec::class) {

  description = "launches the local development website"
  group = "website"

  dependsOn(
    updateWebsiteChangelog,
    updateWebsiteNextDocsVersionRefs,
    updateWebsitePackageJsonVersion
  )

  workingDir("$rootDir/website")
  commandLine("yarn", "run", "start")
}

val buildSite by tasks.registering(Exec::class) {

  description = "launches the local development website"
  group = "website"

  dependsOn(
    versionDocs,
    updateWebsiteChangelog,
    updateWebsiteNextDocsVersionRefs,
    updateWebsitePackageJsonVersion
  )

  workingDir("./website")
  commandLine("yarn", "install")
  commandLine("yarn", "run", "build")
}

val versionDocs by tasks.registering(Exec::class) {

  description =
    "creates a new version snapshot of website docs, using the current version defined in gradle.properties"
  group = "website"

  val existingVersions = with(File("$rootDir/website/versions.json")) {
    "\"([^\"]*)\"".toRegex()
      .findAll(readText())
      .flatMap { it.destructured.toList() }
  }

  val version = VERSION_NAME

  enabled = version !in existingVersions

  workingDir("$rootDir/website")
  commandLine("yarn", "run", "docusaurus", "docs:version", version)
}

val updateWebsiteChangelog by tasks.registering(Copy::class) {

  description = "copies the root project's CHANGELOG to the website and updates its formatting"
  group = "website"

  from("changelog.md")
  into("$rootDir/website/src/pages")

  doLast {

// add one hashmark to each header, because GitHub and Docusaurus render them differently
    val changelog = File("$rootDir/website/src/pages/changelog.md")

    val newText = changelog.readText()
      .lines()
      .joinToString("\n") { line ->
        line.replace("^(#+) (.*)".toRegex()) { matchResult ->
          val (hashes, text) = matchResult.destructured

          "$hashes# $text"
        }
      }
    changelog.writeText(newText)
  }
}
