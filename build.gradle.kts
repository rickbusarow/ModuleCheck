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

import modulecheck.builds.GROUP
import modulecheck.builds.PLUGIN_ID
import modulecheck.builds.VERSION_NAME
import modulecheck.builds.VERSION_NAME_STABLE

buildscript {
  dependencies {
    classpath(libs.kotlin.gradle.plugin)
    classpath(libs.vanniktech.publish)
  }
}

plugins {
  id("mcbuild.root")
  alias(libs.plugins.dependencyAnalysis)
  alias(libs.plugins.detekt)
  alias(libs.plugins.doks)
  alias(libs.plugins.moduleCheck)
  alias(libs.plugins.taskTree)
}

doks {
  dokSet("readme") {
    docs("README.md", "CHANGELOG.md")

    rule("modulecheck-version") {
      regex = SEMVER
      replacement = VERSION_NAME
    }
    rule("modulecheck-plugin") {
      regex = gradlePluginWithVersion(pluginId = PLUGIN_ID)
      replacement = "$1$2$3$4$VERSION_NAME$6"
    }
    rule("modulecheck-maven") {
      regex = maven(GROUP.escapeRegex())
      replacement = "$1:$2:$VERSION_NAME"
    }

    rule("modulecheck-version-stable") {
      regex = SEMVER
      replacement = VERSION_NAME_STABLE
    }
    rule("modulecheck-plugin-stable") {
      regex = gradlePluginWithVersion(pluginId = PLUGIN_ID)
      replacement = "$1$2$3$4$VERSION_NAME_STABLE$6"
    }
    rule("modulecheck-maven-stable") {
      regex = maven(GROUP.escapeRegex())
      replacement = "$1:$2:$VERSION_NAME_STABLE"
    }
  }
  dokSet("website") {
    docs("website/docs") {
      include("**/*.md", "**/*.mdx")
    }
    sampleCodeSource("modulecheck-gradle/plugin/src/integrationTest/kotlin") {
      include("**/*.kt")
    }

    rule("modulecheck-version") {
      regex = SEMVER
      replacement = VERSION_NAME
    }
    rule("modulecheck-plugin") {
      regex = gradlePluginWithVersion(pluginId = PLUGIN_ID)
      replacement = "$1$2$3$4$VERSION_NAME$6"
    }
    rule("modulecheck-maven") {
      regex = maven(GROUP.escapeRegex())
      replacement = "$1:$2:$VERSION_NAME"
    }

    rule("dollar-raw-string") {
      regex = "\${'$'}".escapeRegex()
      replacement = "$".escapeReplacement()
    }
    rule("buildConfig-version") {
      regex = "\${BuildConfig.version}".escapeRegex()
      replacement = VERSION_NAME.escapeReplacement()
    }

    rule("modulecheck-gradle-config-kotlin") {
      replacement = sourceCode(
        fqName = "modulecheck.gradle.ConfigValidationTest.kotlinConfig",
        bodyOnly = true,
        codeBlockLanguage = "kotlin",
        attributes = "title = root/build.gradle.kts"
      )
    }
    rule("modulecheck-gradle-config-groovy") {
      replacement = sourceCode(
        fqName = "modulecheck.gradle.ConfigValidationTest.groovyConfig",
        bodyOnly = true,
        codeBlockLanguage = "groovy",
        attributes = "title = root/build.gradle"
      )
    }
  }
}

moduleCheck {
  deleteUnused = true
  checks {
    depths = true
    sortDependencies = true
  }
  reports {
    depths.enabled = true
    graphs {
      enabled = true
      outputDir = "$buildDir/reports/modulecheck/graphs"
    }
  }
}

afterEvaluate {

  // Hack for ensuring that when 'publishToMavenLocal' is invoked from the root project,
  // all subprojects are published.  This is used in plugin tests.
  sequenceOf(
    "publishToMavenLocal",
    "publishToMavenLocalNoDokka"
  ).forEach { taskName ->
    tasks.register(taskName) {
      subprojects.forEach { sub ->
        dependsOn(sub.tasks.matching { it.name == taskName })
      }
    }
  }

  sequenceOf(
    "buildHealth",
    "clean",
    "ktlintCheck",
    "ktlintFormat",
    "ktlintCheckGradleScripts",
    "ktlintFormatGradleScripts",
    "moduleCheck",
    "moduleCheckAuto",
    "moduleCheckSortDependenciesAuto",
    "test"
  ).forEach { taskName ->
    tasks.named(taskName).configure {
      dependsOn(gradle.includedBuild("build-logic").task(":$taskName"))
    }
  }
}
