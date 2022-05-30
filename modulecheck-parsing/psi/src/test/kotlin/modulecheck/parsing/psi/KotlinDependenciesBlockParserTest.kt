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

package modulecheck.parsing.psi

import kotlinx.coroutines.runBlocking
import modulecheck.model.dependency.ConfiguredProjectDependency.ConfiguredRuntimeProjectDependency
import modulecheck.parsing.gradle.dsl.ExternalDependencyDeclaration
import modulecheck.parsing.gradle.dsl.ModuleDependencyDeclaration
import modulecheck.parsing.gradle.dsl.UnknownDependencyDeclaration
import modulecheck.parsing.gradle.model.ConfigurationName
import modulecheck.parsing.gradle.model.ProjectPath.StringProjectPath
import modulecheck.parsing.gradle.model.ProjectPath.TypeSafeProjectPath
import modulecheck.project.McProject
import modulecheck.project.test.ProjectTest
import modulecheck.reporting.logging.PrintLogger
import org.junit.jupiter.api.Test

internal class KotlinDependenciesBlockParserTest : ProjectTest() {

  val parser by resets {
    KotlinDependenciesBlockParser(PrintLogger()) { configurationName, projectPath, isTestFixture ->
      ConfiguredRuntimeProjectDependency(configurationName, projectPath, isTestFixture)
    }
  }

  @Test
  fun `external declaration`() {
    val block = parser
      .parse(
        """
       dependencies {
          api("com.foo:bar:1.2.3.4")
       }
        """
      ).single()

    block.settings shouldBe listOf(
      ExternalDependencyDeclaration(
        configName = ConfigurationName.api,
        declarationText = """api("com.foo:bar:1.2.3.4")""",
        statementWithSurroundingText = """   api("com.foo:bar:1.2.3.4")""",
        group = "com.foo",
        moduleName = "bar",
        version = "1.2.3.4"
      )
    )
  }

  @Test
  fun `string extension configuration functions declaration`() {
    val block = parser
      .parse(
        """
       dependencies {
         "api"(project(path = ":core:jvm"))
       }
        """
      ).single()

    block.settings shouldBe listOf(
      ModuleDependencyDeclaration(
        projectPath = StringProjectPath(":core:jvm"),
        projectAccessor = """project(path = ":core:jvm")""",
        configName = ConfigurationName.api,
        declarationText = """"api"(project(path = ":core:jvm"))""",
        statementWithSurroundingText = """  "api"(project(path = ":core:jvm"))"""
      )
    )
  }

  @Test
  fun `declaration's original string should include trailing comment`() {
    val block = parser
      .parse(
        """
       dependencies {
          api(project(":core:jvm")) // trailing comment
          api(project(":core:jvm"))
       }
        """
      ).single()

    block.settings shouldBe listOf(
      ModuleDependencyDeclaration(
        projectPath = StringProjectPath(":core:jvm"),
        projectAccessor = """project(":core:jvm")""",
        configName = ConfigurationName.api,
        declarationText = """api(project(":core:jvm"))""",
        statementWithSurroundingText = """   api(project(":core:jvm")) // trailing comment"""
      ),
      ModuleDependencyDeclaration(
        projectPath = StringProjectPath(":core:jvm"),
        projectAccessor = """project(":core:jvm")""",
        configName = ConfigurationName.api,
        declarationText = """api(project(":core:jvm"))""",
        statementWithSurroundingText = """   api(project(":core:jvm"))"""
      )
    )
  }

  @Test
  fun `declaration with annotation should include annotation with argument`() {
    val block = parser
      .parse(
        """
       dependencies {
          api(project(":core:android"))
          @Suppress("unused-dependency")
          api(project(":core:jvm"))
          testImplementation(project(":core:test"))
       }
        """
      ).single()

    block.settings shouldBe listOf(
      ModuleDependencyDeclaration(
        projectPath = StringProjectPath(":core:android"),
        projectAccessor = """project(":core:android")""",
        configName = ConfigurationName.api,
        declarationText = """api(project(":core:android"))""",
        statementWithSurroundingText = "   api(project(\":core:android\"))",
        suppressed = listOf()
      ),
      ModuleDependencyDeclaration(
        projectPath = StringProjectPath(":core:jvm"),
        projectAccessor = """project(":core:jvm")""",
        configName = ConfigurationName.api,
        declarationText = """api(project(":core:jvm"))""",
        statementWithSurroundingText = "   @Suppress(\"unused-dependency\")\n   api(project(\":core:jvm\"))",
        suppressed = listOf("unused-dependency")
      ),
      ModuleDependencyDeclaration(
        projectPath = StringProjectPath(":core:test"),
        projectAccessor = """project(":core:test")""",
        configName = ConfigurationName.testImplementation,
        declarationText = """testImplementation(project(":core:test"))""",
        statementWithSurroundingText = "   testImplementation(project(\":core:test\"))",
        suppressed = listOf()
      )
    )
  }

  @Test
  fun `dependency block with Suppress annotation with old IDs should include annotation with argument`() {
    val block = parser
      .parse(
        """
       @Suppress("Unused")
       dependencies {
          api(project(":core:android"))
          @Suppress("InheritedDependency")
          api(project(":core:jvm"))
          testImplementation(project(":core:test"))
       }
        """
      ).single()

    block.settings shouldBe listOf(
      ModuleDependencyDeclaration(
        projectPath = StringProjectPath(":core:android"),
        projectAccessor = """project(":core:android")""",
        configName = ConfigurationName.api,
        declarationText = """api(project(":core:android"))""",
        statementWithSurroundingText = "   api(project(\":core:android\"))",
        suppressed = listOf("unused-dependency")
      ),
      ModuleDependencyDeclaration(
        projectPath = StringProjectPath(":core:jvm"),
        projectAccessor = """project(":core:jvm")""",
        configName = ConfigurationName.api,
        declarationText = """api(project(":core:jvm"))""",
        statementWithSurroundingText = "   @Suppress(\"InheritedDependency\")\n   api(project(\":core:jvm\"))",
        suppressed = listOf("inherited-dependency", "unused-dependency")
      ),
      ModuleDependencyDeclaration(
        projectPath = StringProjectPath(":core:test"),
        projectAccessor = """project(":core:test")""",
        configName = ConfigurationName.testImplementation,
        declarationText = """testImplementation(project(":core:test"))""",
        statementWithSurroundingText = "   testImplementation(project(\":core:test\"))",
        suppressed = listOf("unused-dependency")
      )
    )
  }

  @Test
  fun `dependency block with Suppress annotation should include annotation with argument`() {
    val block = parser
      .parse(
        """
       @Suppress("unused-dependency")
       dependencies {
          api(project(":core:android"))
          @Suppress("inherited-dependency")
          api(project(":core:jvm"))
          testImplementation(project(":core:test"))
       }
        """
      ).single()

    block.settings shouldBe listOf(
      ModuleDependencyDeclaration(
        projectPath = StringProjectPath(":core:android"),
        projectAccessor = """project(":core:android")""",
        configName = ConfigurationName.api,
        declarationText = """api(project(":core:android"))""",
        statementWithSurroundingText = "   api(project(\":core:android\"))",
        suppressed = listOf("unused-dependency")
      ),
      ModuleDependencyDeclaration(
        projectPath = StringProjectPath(":core:jvm"),
        projectAccessor = """project(":core:jvm")""",
        configName = ConfigurationName.api,
        declarationText = """api(project(":core:jvm"))""",
        statementWithSurroundingText = "   @Suppress(\"inherited-dependency\")\n   api(project(\":core:jvm\"))",
        suppressed = listOf("inherited-dependency", "unused-dependency")
      ),
      ModuleDependencyDeclaration(
        projectPath = StringProjectPath(":core:test"),
        projectAccessor = """project(":core:test")""",
        configName = ConfigurationName.testImplementation,
        declarationText = """testImplementation(project(":core:test"))""",
        statementWithSurroundingText = "   testImplementation(project(\":core:test\"))",
        suppressed = listOf("unused-dependency")
      )
    )
  }

  @Test
  fun `blank line between dependencies`() {
    val block = parser
      .parse(
        """
       dependencies {
          api(testFixtures(project(":lib1")))

          api(testFixtures(project(":lib2")))
          implementation(testFixtures(project(":lib3")))
       }
        """
      ).single()

    block.settings shouldBe listOf(
      ModuleDependencyDeclaration(
        projectPath = StringProjectPath(":lib1"),
        projectAccessor = """project(":lib1")""",
        configName = ConfigurationName.api,
        declarationText = """api(testFixtures(project(":lib1")))""",
        statementWithSurroundingText = """   api(testFixtures(project(":lib1")))"""
      ),
      ModuleDependencyDeclaration(
        projectPath = StringProjectPath(":lib2"),
        projectAccessor = """project(":lib2")""",
        configName = ConfigurationName.api,
        declarationText = """api(testFixtures(project(":lib2")))""",
        statementWithSurroundingText = """|
           |   api(testFixtures(project(":lib2")))
        """.trimMargin()
      ),
      ModuleDependencyDeclaration(
        projectPath = StringProjectPath(":lib3"),
        projectAccessor = """project(":lib3")""",
        configName = ConfigurationName.implementation,
        declarationText = """implementation(testFixtures(project(":lib3")))""",
        statementWithSurroundingText = """   implementation(testFixtures(project(":lib3")))"""
      )
    )
  }

  @Test
  fun `string module dependency declaration with testFixtures should be parsed`() {
    val block = parser
      .parse(
        """
       dependencies {
          api(testFixtures(project(":core:jvm")))
       }
        """
      ).single()

    block.settings shouldBe listOf(
      ModuleDependencyDeclaration(
        projectPath = StringProjectPath(":core:jvm"),
        projectAccessor = """project(":core:jvm")""",
        configName = ConfigurationName.api,
        declarationText = """api(testFixtures(project(":core:jvm")))""",
        statementWithSurroundingText = """   api(testFixtures(project(":core:jvm")))"""
      )
    )
  }

  @Test
  fun `module dependency with commented out dependency above it`() {
    val block = parser
      .parse(
        """
       dependencies {
          // api(project(":core:dagger"))
          api(testFixtures(project(":core:jvm")))
       }
        """
      ).single()

    block.settings shouldBe listOf(
      ModuleDependencyDeclaration(
        projectPath = StringProjectPath(":core:jvm"),
        projectAccessor = """project(":core:jvm")""",
        configName = ConfigurationName.api,
        declarationText = """api(testFixtures(project(":core:jvm")))""",
        statementWithSurroundingText = "   // api(project(\":core:dagger\"))\n" +
          "   api(testFixtures(project(\":core:jvm\")))"
      )
    )
  }

  @Test
  fun `module dependency with commented out dependency from previous finding above it`() {
    val block = parser
      .parse(
        """
       dependencies {
          // api(project(":core:dagger")) // ModuleCheck finding [unused-dependency]
          api(testFixtures(project(":core:jvm")))
       }
        """
      ).single()

    block.settings shouldBe listOf(
      ModuleDependencyDeclaration(
        projectPath = StringProjectPath(":core:jvm"),
        projectAccessor = """project(":core:jvm")""",
        configName = ConfigurationName.api,
        declarationText = """api(testFixtures(project(":core:jvm")))""",
        statementWithSurroundingText = """   api(testFixtures(project(":core:jvm")))"""
      )
    )
  }

  @Test
  fun `type-safe module dependency declaration with testFixtures should be parsed`() {
    val block = parser
      .parse(
        """
       dependencies {
          api(testFixtures(projects.core.jvm))
       }
        """
      ).single()

    block.settings shouldBe listOf(
      ModuleDependencyDeclaration(
        projectPath = TypeSafeProjectPath("core.jvm"),
        projectAccessor = "projects.core.jvm",
        configName = ConfigurationName.api,
        declarationText = """api(testFixtures(projects.core.jvm))""",
        statementWithSurroundingText = """   api(testFixtures(projects.core.jvm))"""
      )
    )
  }

  @Test
  fun `module dependency with config block should split declarations properly`() {
    val block = parser
      .parse(
        """
       dependencies {
          api(project(":core:test")) {
            exclude(group = "androidx.appcompat")
          }

          api(project(":core:jvm"))
       }
        """
      ).single()

    block.settings shouldBe listOf(
      ModuleDependencyDeclaration(
        projectPath = StringProjectPath(":core:test"),
        projectAccessor = """project(":core:test")""",
        configName = ConfigurationName.api,
        declarationText = """api(project(":core:test")) {
          |     exclude(group = "androidx.appcompat")
          |   }
        """.trimMargin(),
        statementWithSurroundingText = """
          |   api(project(":core:test")) {
          |     exclude(group = "androidx.appcompat")
          |   }
        """.trimMargin()
      ),
      ModuleDependencyDeclaration(
        projectPath = StringProjectPath(":core:jvm"),
        projectAccessor = """project(":core:jvm")""",
        configName = ConfigurationName.api,
        declarationText = "api(project(\":core:jvm\"))",
        statementWithSurroundingText = "\n   api(project(\":core:jvm\"))"
      )
    )
  }

  @Test
  fun `module dependency with config block and preceding declaration should split declarations properly`() {
    val block = parser
      .parse(
        """
       dependencies {
          api(project(":core:jvm"))

          api(project(":core:test")) {
            exclude(group = "androidx.appcompat")
          }
       }
        """
      ).single()

    block.settings shouldBe listOf(
      ModuleDependencyDeclaration(
        projectPath = StringProjectPath(":core:jvm"),
        projectAccessor = """project(":core:jvm")""",
        configName = ConfigurationName.api,
        declarationText = "api(project(\":core:jvm\"))",
        statementWithSurroundingText = "   api(project(\":core:jvm\"))"
      ),
      ModuleDependencyDeclaration(
        projectPath = StringProjectPath(":core:test"),
        projectAccessor = """project(":core:test")""",
        configName = ConfigurationName.api,
        declarationText = """api(project(":core:test")) {
          |     exclude(group = "androidx.appcompat")
          |   }
        """.trimMargin(),
        statementWithSurroundingText = """

          |   api(project(":core:test")) {
          |     exclude(group = "androidx.appcompat")
          |   }
        """.trimMargin()
      )
    )
  }

  @Test
  fun `module dependency with preceding blank line should preserve the blank line`() {
    val block = parser
      .parse(
        """
       dependencies {
          api(project(":core:test"))

          api(project(":core:jvm"))
       }
        """
      ).single()

    block.settings shouldBe listOf(
      ModuleDependencyDeclaration(
        projectPath = StringProjectPath(":core:test"),
        projectAccessor = """project(":core:test")""",
        configName = ConfigurationName.api,
        declarationText = "api(project(\":core:test\"))",
        statementWithSurroundingText = "   api(project(\":core:test\"))"
      ),
      ModuleDependencyDeclaration(
        projectPath = StringProjectPath(":core:jvm"),
        projectAccessor = """project(":core:jvm")""",
        configName = ConfigurationName.api,
        declarationText = "api(project(\":core:jvm\"))",
        statementWithSurroundingText = "\n   api(project(\":core:jvm\"))"
      )
    )
  }

  @Test
  fun `module dependency with two different configs should be recorded twice`() {
    val block = parser
      .parse(
        """
       dependencies {
          implementation(project(":core:jvm"))
          api(project(":core:jvm"))
       }
        """
      ).single()

    block.settings shouldBe listOf(
      ModuleDependencyDeclaration(
        projectPath = StringProjectPath(":core:jvm"),
        projectAccessor = """project(":core:jvm")""",
        configName = ConfigurationName.implementation,
        declarationText = """implementation(project(":core:jvm"))""",
        statementWithSurroundingText = """   implementation(project(":core:jvm"))"""
      ),
      ModuleDependencyDeclaration(
        projectPath = StringProjectPath(":core:jvm"),
        projectAccessor = """project(":core:jvm")""",
        configName = ConfigurationName.api,
        declarationText = """api(project(":core:jvm"))""",
        statementWithSurroundingText = """   api(project(":core:jvm"))"""
      )
    )
  }

  @Test
  fun `declaration's original string should include preceding single-line comment`() {
    val block = parser
      .parse(
        """
       dependencies {
          api("com.foo:bar:1.2.3.4") // inline comment

          // single-line comment
          implementation(project(":core:android"))
       }
        """
      ).single()

    block.settings shouldBe listOf(
      ExternalDependencyDeclaration(
        configName = ConfigurationName.api,
        declarationText = """api("com.foo:bar:1.2.3.4")""",
        statementWithSurroundingText = """   api("com.foo:bar:1.2.3.4") // inline comment""",
        group = "com.foo",
        moduleName = "bar",
        version = "1.2.3.4"
      ),
      ModuleDependencyDeclaration(
        projectPath = StringProjectPath(":core:android"),
        projectAccessor = """project(":core:android")""",
        configName = ConfigurationName.implementation,
        declarationText = """implementation(project(":core:android"))""",
        statementWithSurroundingText = """

       |   // single-line comment
       |   implementation(project(":core:android"))
        """.trimMargin()
      )
    )
  }

  @Test
  fun `declaration's original string should include preceding block comment`() {
    val block = parser
      .parse(
        """
       dependencies {
          api("com.foo:bar:1.2.3.4") // inline comment

          /*
          block comment
          */
          implementation(project(":core:android"))
       }
        """
      ).single()

    block.settings shouldBe listOf(
      ExternalDependencyDeclaration(
        configName = ConfigurationName.api,
        declarationText = """api("com.foo:bar:1.2.3.4")""",
        statementWithSurroundingText = """   api("com.foo:bar:1.2.3.4") // inline comment""",
        group = "com.foo",
        moduleName = "bar",
        version = "1.2.3.4"
      ),
      ModuleDependencyDeclaration(
        projectPath = StringProjectPath(":core:android"),
        projectAccessor = """project(":core:android")""",
        configName = ConfigurationName.implementation,
        declarationText = """implementation(project(":core:android"))""",
        statementWithSurroundingText = """

        |   /*
        |   block comment
        |   */
        |   implementation(project(":core:android"))
        """.trimMargin()
      )
    )
  }

  @Test
  fun `declaration's original string should include preceding in-line block comment`() {
    val block = parser
      .parse(
        """
       dependencies {
          api("com.foo:bar:1.2.3.4") // inline comment
          /* single-line block comment */ implementation(project(":core:android"))
       }
        """
      ).single()

    block.settings shouldBe listOf(
      ExternalDependencyDeclaration(
        configName = ConfigurationName.api,
        declarationText = """api("com.foo:bar:1.2.3.4")""",
        statementWithSurroundingText = """   api("com.foo:bar:1.2.3.4") // inline comment""",
        group = "com.foo",
        moduleName = "bar",
        version = "1.2.3.4"
      ),
      ModuleDependencyDeclaration(
        projectPath = StringProjectPath(":core:android"),
        projectAccessor = """project(":core:android")""",
        configName = ConfigurationName.implementation,
        declarationText = """implementation(project(":core:android"))""",
        statementWithSurroundingText = """   /* single-line block comment */ implementation(project(":core:android"))"""
      )
    )
  }

  @Test
  fun `duplicate module dependency with same config should be recorded twice`() {
    val block = parser
      .parse(
        """
       dependencies {
          api(project(":core:jvm"))
          api (   project(":core:jvm"))
       }
        """
      ).single()

    block.settings shouldBe listOf(
      ModuleDependencyDeclaration(
        projectPath = StringProjectPath(":core:jvm"),
        projectAccessor = """project(":core:jvm")""",
        configName = ConfigurationName.api,
        declarationText = """api(project(":core:jvm"))""",
        statementWithSurroundingText = """   api(project(":core:jvm"))"""
      ),
      ModuleDependencyDeclaration(
        projectPath = StringProjectPath(":core:jvm"),
        projectAccessor = """project(":core:jvm")""",
        configName = ConfigurationName.api,
        declarationText = """api (   project(":core:jvm"))""",
        statementWithSurroundingText = """   api (   project(":core:jvm"))"""
      )
    )
  }

  @Test
  fun `modules declared using type-safe accessors can be looked up using their path`() {
    val block = parser
      .parse(
        """
       dependencies {
          api(projects.core.test)
          implementation(projects.httpLogging)
       }
        """
      ).single()

    block.settings shouldBe listOf(
      ModuleDependencyDeclaration(
        projectPath = TypeSafeProjectPath("core.test"),
        projectAccessor = "projects.core.test",
        configName = ConfigurationName.api,
        declarationText = """api(projects.core.test)""",
        statementWithSurroundingText = """   api(projects.core.test)"""
      ),
      ModuleDependencyDeclaration(
        projectPath = TypeSafeProjectPath("httpLogging"),
        projectAccessor = "projects.httpLogging",
        configName = ConfigurationName.implementation,
        declarationText = """implementation(projects.httpLogging)""",
        statementWithSurroundingText = """   implementation(projects.httpLogging)"""
      )
    )
  }

  @Test
  fun `buildscript dependencies should not be parsed`() {
    val block = parser
      .parse(
        """
        buildscript {
          repositories {
            mavenCentral()
            google()
            jcenter()
            maven("https://plugins.gradle.org/m2/")
            maven("https://oss.sonatype.org/content/repositories/snapshots")
          }
          dependencies {
            classpath("com.android.tools.build:gradle:7.0.2")
            classpath("com.squareup.anvil:gradle-plugin:2.3.4")
            classpath("org.jetbrains.kotlin:kotlin-gradle-plugin:1.5.30")
          }
        }
        dependencies {
          api(libs.ktlint)
        }

        """
      ).single()

    block.settings shouldBe listOf(
      UnknownDependencyDeclaration(
        argument = "libs.ktlint",
        configName = ConfigurationName.api,
        declarationText = "api(libs.ktlint)",
        statementWithSurroundingText = "  api(libs.ktlint)"
      )
    )
  }

  fun KotlinDependenciesBlockParser.parse(
    string: String,
    project: McProject = simpleProject(buildFileText = string.trimIndent())
  ): List<KotlinDependenciesBlock> = runBlocking { parse(project) }
}
