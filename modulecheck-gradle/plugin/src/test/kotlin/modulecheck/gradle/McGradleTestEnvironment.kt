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

package modulecheck.gradle

import com.rickbusarow.kase.ParamTestEnvironmentFactory
import com.rickbusarow.kase.files.TestLocation
import modulecheck.config.CodeGeneratorBinding
import modulecheck.config.internal.defaultCodeGeneratorBindings
import modulecheck.gradle.internal.BuildProperties
import modulecheck.parsing.kotlin.compiler.impl.DependencyModuleDescriptorAccess
import modulecheck.project.ProjectCache
import modulecheck.project.generation.ProjectCollector
import modulecheck.testing.HasTestVersions
import modulecheck.testing.McTestVersions
import modulecheck.testing.TestEnvironment
import modulecheck.testing.clean
import modulecheck.utils.createSafely
import modulecheck.utils.letIf
import modulecheck.utils.remove
import modulecheck.utils.resolve
import org.gradle.testkit.runner.BuildResult
import org.gradle.testkit.runner.GradleRunner
import org.gradle.testkit.runner.TaskOutcome
import java.io.File
import kotlin.text.RegexOption.IGNORE_CASE

class McGradleTestEnvironment(
  override val testVersions: McTestVersions,
  override val projectCache: ProjectCache,
  testVariantNames: List<String>,
  testLocation: TestLocation
) : TestEnvironment(testVariantNames, testLocation),
  ProjectCollector,
  HasTestVersions {

  override val root: File get() = workingDir

  val kotlinVersion get() = testVersions.kotlinVersion
  val agpVersion get() = testVersions.agpVersion
  val gradleVersion get() = testVersions.gradleVersion
  val anvilVersion get() = testVersions.anvilVersion

  override val dependencyModuleDescriptorAccess: DependencyModuleDescriptorAccess by lazy {
    DependencyModuleDescriptorAccess(projectCache)
  }

  override val codeGeneratorBindings: List<CodeGeneratorBinding> by lazy {
    defaultCodeGeneratorBindings()
  }

  @Suppress("PropertyName", "VariableNaming")
  val DEFAULT_BUILD_FILE by lazy {
    """
      buildscript {
        dependencies {
          classpath("com.android.tools.build:gradle:$agpVersion")
          classpath("org.jetbrains.kotlin:kotlin-gradle-plugin:$kotlinVersion")
        }
      }

      plugins {
        id("com.rickbusarow.module-check")
      }
    """.trimIndent()
  }

  val rootBuild by lazy {
    root.resolve("build.gradle.kts")
      .createSafely(DEFAULT_BUILD_FILE, overwrite = false)
  }

  @Suppress("PropertyName", "VariableNaming")
  val DEFAULT_SETTINGS_FILE by lazy {
    """
      rootProject.name = "root"

      pluginManagement {
        repositories {
          gradlePluginPortal()
          mavenCentral()
          mavenLocal()
          google()
        }
        resolutionStrategy {
          eachPlugin {
            if (requested.id.id.startsWith("com.android")) {
              useVersion("$agpVersion")
            }
            if (requested.id.id == "com.rickbusarow.module-check") {
              useVersion("${BuildProperties.version}")
            }
            if (requested.id.id.startsWith("org.jetbrains.kotlin")) {
              useVersion("$kotlinVersion")
            }
            if (requested.id.id == "com.squareup.anvil") {
              useVersion("$anvilVersion")
            }
          }
        }
      }
      dependencyResolutionManagement {
        @Suppress("UnstableApiUsage")
        repositories {
          mavenCentral()
          mavenLocal()
          google()
        }
      }
    """.trimIndent()
  }

  val rootSettings by lazy {
    root.resolve("settings.gradle.kts")
      .createSafely(DEFAULT_SETTINGS_FILE)
  }

  val rootProject by lazy {
    rootBuild
    rootSettings
    root
  }

  val gradleRunner: GradleRunner by lazy {
    GradleRunner.create()
      .forwardOutput()
      .withGradleVersion(gradleVersion)
      // .withTestKitDir(testKitDir)
      // .withPluginClasspath()
      .withDebug(true)
      .withProjectDir(workingDir)
  }

  constructor(params: GradleTestEnvironmentParams) : this(
    testVersions = params.testVersions,
    projectCache = params.projectCache,
    testStackFrame = params.testStackFrame,
    testVariantNames = params.testVariantNames
  )

  // Make sure that every project in the cache is also added to the root project's settings file
  private fun addIncludes() {
    val includes = projectCache.values.map { it.projectPath.value }
      .joinToString(separator = "\n", prefix = "\n", postfix = "\n") { "include(\"$it\")" }
    rootSettings.appendText(includes)
  }

  fun build(vararg tasks: String, withPluginClasspath: Boolean, stacktrace: Boolean): BuildResult {

    // make sure that the root project is initialized
    rootProject
    addIncludes()

    return gradleRunner
      .letIf(withPluginClasspath) { it.withPluginClasspath() }
      .withArguments(tasks.toList().letIf(stacktrace) { it + "--stacktrace" })
      .build()
  }

  fun shouldSucceed(
    vararg tasks: String,
    withPluginClasspath: Boolean = false,
    stacktrace: Boolean = true,
    assertions: BuildResult.() -> Unit = {}
  ): BuildResult {
    val result = build(
      *tasks,
      withPluginClasspath = withPluginClasspath,
      stacktrace = stacktrace
    )

    result.tasks.last().outcome shouldBe TaskOutcome.SUCCESS

    result.assertions()

    return result
  }

  fun shouldFail(vararg tasks: String): BuildResult {
    // make sure that the root project is initialized
    rootProject
    addIncludes()

    val result = gradleRunner.withArguments(*tasks)
      .buildAndFail()

    result.tasks.last().outcome shouldBe TaskOutcome.FAILED

    return result
  }

  infix fun BuildResult.withTrimmedMessage(message: String) {
    val trimmed = output
      .clean(workingDir)
      .remove(
        "FAILURE: Build failed with an exception.",
        "* What went wrong:",
        "* Try:",
        "> Run with --stacktrace option to get the stack trace.",
        "> Run with --info or --debug option to get more log output.",
        "> Run with --scan to get full insights.",
        "* Get more help at https://help.gradle.org",
        "Daemon will be stopped at the end of the build after running out of JVM memory"
      )
      // remove standard Gradle output noise
      .remove(
        """Execution failed for task ':moduleCheck(?:Auto|)'.""".toRegex(IGNORE_CASE),
        "> Task [^\\n]*".toRegex(),
        ".*Run with --.*".toRegex(),
        """See https://docs\.gradle\.org/[^/]+/userguide/command_line_interface\.html#sec:command_line_warnings""".toRegex(),
        "BUILD (?:SUCCESSFUL|FAILED) in .*".toRegex(),
        """\d+ actionable tasks?: \d+ executed""".toRegex(),
        """> ModuleCheck found \d+ issues? which (?:was|were) not auto-corrected.""".toRegex()
      )
      .removeDuration()
      .remove("\u200B")
      .trim()

    trimmed shouldBe message
  }

  /**
   * replace `ModuleCheck found 2 issues in 1.866 seconds.` with `ModuleCheck found 2 issues`
   *
   * @since 0.12.0
   */
  fun String.removeDuration(): String {
    return replace(durationSuffixRegex) { it.destructured.component1() }
  }

  companion object {
    protected val durationSuffixRegex: Regex =
      """(ModuleCheck found \d+ issues?) in [\d.]+ seconds\.[\s\S]*""".toRegex()
  }
}

class McGradleTestEnvironmentFactory : ParamTestEnvironmentFactory<McTestVersions, McGradleTestEnvironment> {
  override fun create(
    params: McTestVersions,
    names: List<String>,
    location: TestLocation
  ): McGradleTestEnvironment = McGradleTestEnvironment(
    testVersions = params,
    projectCache = ProjectCache(),
    testVariantNames = names,
    testLocation = location
  )
}
