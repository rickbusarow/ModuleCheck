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

package modulecheck.parsing.groovy.antlr

import modulecheck.model.dependency.ProjectDependency.RuntimeProjectDependency
import modulecheck.parsing.gradle.dsl.ExternalDependencyDeclaration
import modulecheck.parsing.gradle.dsl.ModuleDependencyDeclaration
import modulecheck.parsing.gradle.dsl.UnknownDependencyDeclaration
import modulecheck.parsing.gradle.model.ConfigurationName
import modulecheck.parsing.gradle.model.ProjectPath.StringProjectPath
import modulecheck.parsing.gradle.model.ProjectPath.TypeSafeProjectPath
import modulecheck.reporting.logging.PrintLogger
import modulecheck.testing.BaseTest
import modulecheck.utils.child
import modulecheck.utils.createSafely
import org.intellij.lang.annotations.Language
import org.junit.jupiter.api.Test

internal class GroovyDependenciesBlockParserTest : BaseTest() {

  @Test
  fun `external declaration`() = parse(
    """
    dependencies {
       api 'com.foo:bar:1.2.3.4'
    }
    """
  ) {

    settings shouldBe listOf(
      ExternalDependencyDeclaration(
        configName = ConfigurationName.api,
        declarationText = "api 'com.foo:bar:1.2.3.4'",
        statementWithSurroundingText = "   api 'com.foo:bar:1.2.3.4'",
        group = "com.foo",
        moduleName = "bar",
        version = "1.2.3.4"
      )
    )
  }

  @Test
  fun `declaration's original string should include trailing comment`() = parse(
    """
    dependencies {
       api project(':core:jvm') // trailing comment
       api project(':core:jvm')
    }
    """
  ) {

    settings shouldBe listOf(
      ModuleDependencyDeclaration(
        projectPath = StringProjectPath(":core:jvm"),
        projectAccessor = "project(':core:jvm')",
        configName = ConfigurationName.api,
        declarationText = """api project(':core:jvm')""",
        statementWithSurroundingText = """   api project(':core:jvm') // trailing comment"""
      ),
      ModuleDependencyDeclaration(
        projectPath = StringProjectPath(":core:jvm"),
        projectAccessor = "project(':core:jvm')",
        configName = ConfigurationName.api,
        declarationText = """api project(':core:jvm')""",
        statementWithSurroundingText = """   api project(':core:jvm')"""
      )
    )
  }

  @Test
  fun `suppression which doesn't match finding name regex should be ignored`() = parse(
    """
        //noinspection DSL_SCOPE_VIOLATION
       dependencies {
         api project(':core:android')
         api project(':core:jvm')
       }
        """
  ) {

    allSuppressions.values.flatten() shouldBe emptyList()
  }

  @Test
  fun `declaration with noinspection with old IDs should include suppressed with argument`() =
    parse(
      """
    dependencies {
      api project(':core:android')
      //noinspection Unused
      //noinspection MustBeApi
      api project(':core:jvm')
      testImplementation project(':core:test')
    }
    """
    ) {

      settings shouldBe listOf(
        ModuleDependencyDeclaration(
          projectPath = StringProjectPath(":core:android"),
          projectAccessor = "project(':core:android')",
          configName = ConfigurationName.api,
          declarationText = """api project(':core:android')""",
          statementWithSurroundingText = "  api project(':core:android')",
          suppressed = listOf()
        ),
        ModuleDependencyDeclaration(
          projectPath = StringProjectPath(":core:jvm"),
          projectAccessor = "project(':core:jvm')",
          configName = ConfigurationName.api,
          declarationText = """api project(':core:jvm')""",
          statementWithSurroundingText = "  //noinspection Unused\n" +
            "  //noinspection MustBeApi\n" +
            "  api project(':core:jvm')",
          suppressed = listOf("unused-dependency", "must-be-api")
        ),
        ModuleDependencyDeclaration(
          projectPath = StringProjectPath(":core:test"),
          projectAccessor = "project(':core:test')",
          configName = ConfigurationName.testImplementation,
          declarationText = """testImplementation project(':core:test')""",
          statementWithSurroundingText = "  testImplementation project(':core:test')",
          suppressed = listOf()
        )
      )
    }

  @Test
  fun `declaration with noinspection should include suppressed with argument`() = parse(
    """
    dependencies {
      api project(':core:android')
      //noinspection unused-dependency
      //noinspection must-be-api
      api project(':core:jvm')
      testImplementation project(':core:test')
    }
    """
  ) {

    settings shouldBe listOf(
      ModuleDependencyDeclaration(
        projectPath = StringProjectPath(":core:android"),
        projectAccessor = "project(':core:android')",
        configName = ConfigurationName.api,
        declarationText = """api project(':core:android')""",
        statementWithSurroundingText = "  api project(':core:android')",
        suppressed = listOf()
      ),
      ModuleDependencyDeclaration(
        projectPath = StringProjectPath(":core:jvm"),
        projectAccessor = "project(':core:jvm')",
        configName = ConfigurationName.api,
        declarationText = """api project(':core:jvm')""",
        statementWithSurroundingText = "  //noinspection unused-dependency\n" +
          "  //noinspection must-be-api\n" +
          "  api project(':core:jvm')",
        suppressed = listOf("unused-dependency", "must-be-api")
      ),
      ModuleDependencyDeclaration(
        projectPath = StringProjectPath(":core:test"),
        projectAccessor = "project(':core:test')",
        configName = ConfigurationName.testImplementation,
        declarationText = """testImplementation project(':core:test')""",
        statementWithSurroundingText = "  testImplementation project(':core:test')",
        suppressed = listOf()
      )
    )
  }

  @Test
  fun `dependency block with noinspection comment with old IDs should suppress those warnings in all declarations`() =
    parse(
      """
      //noinspection MustBeApi
      //noinspection Unused
      dependencies {
        api project(':core:android')
        //noinspection InheritedDependency
        api project(':core:jvm')
      }
      """
    ) {

      blockSuppressed shouldBe listOf("must-be-api", "unused-dependency")

      settings shouldBe listOf(
        ModuleDependencyDeclaration(
          projectPath = StringProjectPath(":core:android"),
          projectAccessor = "project(':core:android')",
          configName = ConfigurationName.api,
          declarationText = """api project(':core:android')""",
          statementWithSurroundingText = "  api project(':core:android')",
          suppressed = listOf("must-be-api", "unused-dependency")
        ),
        ModuleDependencyDeclaration(
          projectPath = StringProjectPath(":core:jvm"),
          projectAccessor = "project(':core:jvm')",
          configName = ConfigurationName.api,
          declarationText = """api project(':core:jvm')""",
          statementWithSurroundingText = "  //noinspection InheritedDependency\n  api project(':core:jvm')",
          suppressed = listOf("inherited-dependency", "must-be-api", "unused-dependency")
        )
      )
    }

  @Test
  fun `dependency block with noinspection comment should suppress those warnings in all declarations`() =
    parse(
      """
      //noinspection must-be-api
      //noinspection unused-dependency
      dependencies {
        api project(':core:android')
        //noinspection inherited-dependency
        api project(':core:jvm')
      }
      """
    ) {

      blockSuppressed shouldBe listOf("must-be-api", "unused-dependency")

      settings shouldBe listOf(
        ModuleDependencyDeclaration(
          projectPath = StringProjectPath(":core:android"),
          projectAccessor = "project(':core:android')",
          configName = ConfigurationName.api,
          declarationText = """api project(':core:android')""",
          statementWithSurroundingText = "  api project(':core:android')",
          suppressed = listOf("must-be-api", "unused-dependency")
        ),
        ModuleDependencyDeclaration(
          projectPath = StringProjectPath(":core:jvm"),
          projectAccessor = "project(':core:jvm')",
          configName = ConfigurationName.api,
          declarationText = """api project(':core:jvm')""",
          statementWithSurroundingText = "  //noinspection inherited-dependency\n  api project(':core:jvm')",
          suppressed = listOf("inherited-dependency", "must-be-api", "unused-dependency")
        )
      )
    }

  @Test
  fun `string module dependency declaration with testFixtures should be parsed`() = parse(
    """
    dependencies {
       api testFixtures(project(':core:jvm'))
    }
    """
  ) {

    settings shouldBe listOf(
      ModuleDependencyDeclaration(
        projectPath = StringProjectPath(":core:jvm"),
        projectAccessor = "project(':core:jvm')",
        configName = ConfigurationName.api,
        declarationText = """api testFixtures(project(':core:jvm'))""",
        statementWithSurroundingText = """   api testFixtures(project(':core:jvm'))"""
      )
    )
  }

  @Test
  fun `type-safe module dependency declaration with testFixtures should be parsed`() = parse(
    """
    dependencies {
      api testFixtures(projects.core.jvm)
    }
    """
  ) {

    settings shouldBe listOf(
      ModuleDependencyDeclaration(
        projectPath = TypeSafeProjectPath("core.jvm"),
        projectAccessor = "projects.core.jvm",
        configName = ConfigurationName.api,
        declarationText = """api testFixtures(projects.core.jvm)""",
        statementWithSurroundingText = """  api testFixtures(projects.core.jvm)"""
      )
    )
  }

  @Test
  fun `module dependency with config block should split declarations properly`() = parse(
    """
    dependencies {
      api project(':core:test') {
        exclude group: 'androidx.appcompat'
      }

      api project(':core:jvm')
    }
    """
  ) {

    settings shouldBe listOf(
      ModuleDependencyDeclaration(
        projectPath = StringProjectPath(":core:test"),
        projectAccessor = "project(':core:test')",
        configName = ConfigurationName.api,
        declarationText = """api project(':core:test') {
        |    exclude group: 'androidx.appcompat'
        |  }
        """.trimMargin(),
        statementWithSurroundingText = """  api project(':core:test') {
        |    exclude group: 'androidx.appcompat'
        |  }
        """.trimMargin()
      ),
      ModuleDependencyDeclaration(
        projectPath = StringProjectPath(":core:jvm"),
        projectAccessor = "project(':core:jvm')",
        configName = ConfigurationName.api,
        declarationText = "api project(':core:jvm')",
        statementWithSurroundingText = "\n  api project(':core:jvm')"
      )
    )
  }

  @Test
  fun `module dependency with commented out dependency above it`() = parse(
    """
    dependencies {
      // api project(':core:test')
      api project(':core:jvm')
    }
    """
  ) {

    settings shouldBe listOf(
      ModuleDependencyDeclaration(
        projectPath = StringProjectPath(":core:jvm"),
        projectAccessor = "project(':core:jvm')",
        configName = ConfigurationName.api,
        declarationText = "api project(':core:jvm')",
        statementWithSurroundingText = "  // api project(':core:test')\n  api project(':core:jvm')"
      )
    )
  }

  @Test
  fun `module dependency with commented out dependency from previous finding above it`() = parse(
    """
    dependencies {
      // api project(':core:test') // ModuleCheck finding [unused-dependency]
      api project(':core:jvm')
    }
    """
  ) {

    settings shouldBe listOf(
      ModuleDependencyDeclaration(
        projectPath = StringProjectPath(":core:jvm"),
        projectAccessor = "project(':core:jvm')",
        configName = ConfigurationName.api,
        declarationText = "api project(':core:jvm')",
        statementWithSurroundingText = "  api project(':core:jvm')"
      )
    )
  }

  @Test
  fun `module dependency with config block and preceding declaration should split declarations properly`() =
    parse(
      """
      dependencies {
        api project(':core:jvm')

        api project(':core:test') {
          exclude group: 'androidx.appcompat'
        }
      }
      """
    ) {

      settings shouldBe listOf(
        ModuleDependencyDeclaration(
          projectPath = StringProjectPath(":core:jvm"),
          projectAccessor = "project(':core:jvm')",
          configName = ConfigurationName.api,
          declarationText = "api project(':core:jvm')",
          statementWithSurroundingText = "  api project(':core:jvm')"
        ),
        ModuleDependencyDeclaration(
          projectPath = StringProjectPath(":core:test"),
          projectAccessor = "project(':core:test')",
          configName = ConfigurationName.api,
          declarationText = """api project(':core:test') {
          |    exclude group: 'androidx.appcompat'
          |  }
          """.trimMargin(),
          statementWithSurroundingText = """

          |  api project(':core:test') {
          |    exclude group: 'androidx.appcompat'
          |  }
          """.trimMargin()
        )
      )
    }

  @Test
  fun `module dependency with preceding blank line should preserve the blank line`() = parse(
    """
    dependencies {
       api project(':core:test')

       api project(':core:jvm')
    }
    """
  ) {

    settings shouldBe listOf(
      ModuleDependencyDeclaration(
        projectPath = StringProjectPath(":core:test"),
        projectAccessor = "project(':core:test')",
        configName = ConfigurationName.api,
        declarationText = "api project(':core:test')",
        statementWithSurroundingText = "   api project(':core:test')"
      ),
      ModuleDependencyDeclaration(
        projectPath = StringProjectPath(":core:jvm"),
        projectAccessor = "project(':core:jvm')",
        configName = ConfigurationName.api,
        declarationText = "api project(':core:jvm')",
        statementWithSurroundingText = "\n   api project(':core:jvm')"
      )
    )
  }

  @Test
  fun `module dependency with two different configs should be recorded twice`() = parse(
    """
    dependencies {
       implementation project(':core:jvm')
       api project(':core:jvm')
    }
    """
  ) {

    settings shouldBe listOf(
      ModuleDependencyDeclaration(
        projectPath = StringProjectPath(":core:jvm"),
        projectAccessor = "project(':core:jvm')",
        configName = ConfigurationName.implementation,
        declarationText = """implementation project(':core:jvm')""",
        statementWithSurroundingText = """   implementation project(':core:jvm')"""
      ),
      ModuleDependencyDeclaration(
        projectPath = StringProjectPath(":core:jvm"),
        projectAccessor = "project(':core:jvm')",
        configName = ConfigurationName.api,
        declarationText = """api project(':core:jvm')""",
        statementWithSurroundingText = """   api project(':core:jvm')"""
      )
    )
  }

  @Test
  fun `declaration's original string should include preceding single-line comment`() = parse(
    """
    dependencies {
      api("com.foo:bar:1.2.3.4") // inline comment

      // single-line comment
      implementation project(':core:android')
    }
    """
  ) {

    settings shouldBe listOf(
      ExternalDependencyDeclaration(
        configName = ConfigurationName.api,
        declarationText = "api(\"com.foo:bar:1.2.3.4\")",
        statementWithSurroundingText = "  api(\"com.foo:bar:1.2.3.4\") // inline comment",
        group = "com.foo",
        moduleName = "bar",
        version = "1.2.3.4"
      ),
      ModuleDependencyDeclaration(
        projectPath = StringProjectPath(":core:android"),
        projectAccessor = "project(':core:android')",
        configName = ConfigurationName.implementation,
        declarationText = """implementation project(':core:android')""",
        statementWithSurroundingText = """

        |  // single-line comment
        |  implementation project(':core:android')
        """.trimMargin()
      )
    )
  }

  @Test
  fun `declaration's original string should include preceding block comment`() = parse(
    """
    dependencies {
      api 'com.foo:bar:1.2.3.4' // inline comment

      /*
      block comment
      */
      implementation project(':core:android')
    }
    """
  ) {

    settings shouldBe listOf(
      ExternalDependencyDeclaration(
        configName = ConfigurationName.api,
        declarationText = "api 'com.foo:bar:1.2.3.4'",
        statementWithSurroundingText = "  api 'com.foo:bar:1.2.3.4' // inline comment",
        group = "com.foo",
        moduleName = "bar",
        version = "1.2.3.4"
      ),
      ModuleDependencyDeclaration(
        projectPath = StringProjectPath(":core:android"),
        projectAccessor = "project(':core:android')",
        configName = ConfigurationName.implementation,
        declarationText = """implementation project(':core:android')""",
        statementWithSurroundingText = """

        |  /*
        |  block comment
        |  */
        |  implementation project(':core:android')
        """.trimMargin()
      )
    )
  }

  @Test
  fun `declaration's original string should include preceding in-line block comment`() = parse(
    """
    dependencies {
       api 'com.foo:bar:1.2.3.4' // inline comment
       /* single-line block comment */ implementation project(':core:android')
    }
    """
  ) {

    settings shouldBe listOf(
      ExternalDependencyDeclaration(
        configName = ConfigurationName.api,
        declarationText = "api 'com.foo:bar:1.2.3.4'",
        statementWithSurroundingText = "   api 'com.foo:bar:1.2.3.4' // inline comment",
        group = "com.foo",
        moduleName = "bar",
        version = "1.2.3.4"
      ),
      ModuleDependencyDeclaration(
        projectPath = StringProjectPath(":core:android"),
        projectAccessor = "project(':core:android')",
        configName = ConfigurationName.implementation,
        declarationText = """implementation project(':core:android')""",
        statementWithSurroundingText = """   /* single-line block comment */ implementation project(':core:android')"""
      )
    )
  }

  @Test
  fun `duplicate module dependency with same config should be recorded twice`() = parse(
    """
    dependencies {
      api project(':core:jvm')
      api project(':core:jvm')
    }
    """
  ) {

    settings shouldBe listOf(
      ModuleDependencyDeclaration(
        projectPath = StringProjectPath(":core:jvm"),
        projectAccessor = "project(':core:jvm')",
        configName = ConfigurationName.api,
        declarationText = """api project(':core:jvm')""",
        statementWithSurroundingText = """  api project(':core:jvm')"""
      ),
      ModuleDependencyDeclaration(
        projectPath = StringProjectPath(":core:jvm"),
        projectAccessor = "project(':core:jvm')",
        configName = ConfigurationName.api,
        declarationText = """api project(':core:jvm')""",
        statementWithSurroundingText = """  api project(':core:jvm')"""
      )
    )
  }

  @Test
  fun `modules declared using type-safe accessors can be looked up using their path`() = parse(
    """
    dependencies {
      api projects.core.test
      implementation projects.httpLogging
    }
    """
  ) {

    settings shouldBe listOf(
      ModuleDependencyDeclaration(
        projectPath = TypeSafeProjectPath("core.test"),
        projectAccessor = "projects.core.test",
        configName = ConfigurationName.api,
        declarationText = """api projects.core.test""",
        statementWithSurroundingText = """  api projects.core.test"""
      ),
      ModuleDependencyDeclaration(
        projectPath = TypeSafeProjectPath("httpLogging"),
        projectAccessor = "projects.httpLogging",
        configName = ConfigurationName.implementation,
        declarationText = """implementation projects.httpLogging""",
        statementWithSurroundingText = """  implementation projects.httpLogging"""
      )
    )
  }

  @Test
  fun `buildscript dependencies should not be parsed`() = parse(
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
        classpath 'com.android.tools.build:gradle:7.0.2'
        classpath 'com.squareup.anvil:gradle-plugin:2.3.4'
        classpath 'org.jetbrains.kotlin:kotlin-gradle-plugin:1.5.30'
      }
    }
    dependencies {
      api libs.ktlint
    }

    """
  ) {

    lambdaContent shouldBe "  api libs.ktlint\n"

    settings shouldBe listOf(
      UnknownDependencyDeclaration(
        argument = "libs.ktlint",
        configName = ConfigurationName.api,
        declarationText = "api libs.ktlint",
        statementWithSurroundingText = "  api libs.ktlint"
      )
    )
  }

  inline fun parse(
    @Language("groovy")
    fileText: String,
    assertions: GroovyDependenciesBlock.() -> Unit
  ) {
    testProjectDir.child("build.gradle")
      .createSafely(fileText.trimIndent())
      .let { file ->
        GroovyDependenciesBlockParser(
          PrintLogger()
        ) { configurationName, projectPath, isTestFixture ->
          RuntimeProjectDependency(configurationName, projectPath, isTestFixture)
        }.parse(file)
      }
      .single()
      .assertions()
  }
}
