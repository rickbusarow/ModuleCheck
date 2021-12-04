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

package modulecheck.core

import modulecheck.core.rule.DepthRule
import modulecheck.core.rule.ModuleCheckRuleFactory
import modulecheck.core.rule.MultiRuleFindingFactory
import modulecheck.core.rule.SingleRuleFindingFactory
import modulecheck.project.ConfigurationName
import modulecheck.runtime.test.RunnerTest
import org.junit.jupiter.api.Test

internal class DepthOutputTest : RunnerTest() {

  val ruleFactory by resets { ModuleCheckRuleFactory() }

  val findingFactory by resets { SingleRuleFindingFactory(DepthRule()) }

  @Test
  fun `main source set depths should be reported`() {

    val runner = runner(
      autoCorrect = false,
      findingFactory = findingFactory
    )

    val lib1 = project(":lib1")

    val lib2 = project(":lib2") {
      addDependency(ConfigurationName.implementation, lib1)
    }

    project(":app") {
      addDependency(ConfigurationName.implementation, lib1)
      addDependency(ConfigurationName.implementation, lib2)
    }

    runner.run(allProjects())

    logger.collectReport()
      .joinToString()
      .clean() shouldBe """
        -- ModuleCheck main source set depth results --
            depth    modules
            0        [:lib1]
            1        [:lib2]
            2        [:app]

        ModuleCheck found 0 issues
        """
  }

  @Test
  fun `reported depths should be from after fixes are applied`() {

    settings.checks.depths = true

    val runner = runner(
      autoCorrect = true,
      findingFactory = MultiRuleFindingFactory(
        settings,
        ruleFactory.create(settings)
      )
    )

    val lib1 = project(":lib1")

    project(":lib2") {
      addDependency(ConfigurationName.implementation, lib1)

      buildFile.writeText(
        """
        plugins {
          kotlin("jvm")
        }

        dependencies {
          implementation(project(path = ":lib1"))
        }
        """.trimIndent()
      )
    }

    runner.run(allProjects()).isSuccess shouldBe true

    logger.collectReport()
      .joinToString()
      .clean() shouldBe """
            :lib2
                   dependency    name                source    build file
                ✔  :lib1         unusedDependency              /lib2/build.gradle.kts: (6, 3):

        -- ModuleCheck main source set depth results --
            depth    modules
            0        [:lib1, :lib2]

        ModuleCheck found 1 issue
        """
  }

  @Test
  fun `test source set depths should not be reported`() {

    val runner = runner(
      autoCorrect = false,
      findingFactory = findingFactory
    )

    val lib1 = project(":lib1")

    val lib2 = project(":lib2") {
      addDependency(ConfigurationName.implementation, lib1)
    }

    project(":app") {
      addDependency(ConfigurationName.implementation, lib1)
      addDependency(ConfigurationName.implementation, lib2)
    }

    lib1.addDependency(ConfigurationName("testImplementation"), lib2)

    runner.run(allProjects())

    logger.collectReport()
      .joinToString()
      .clean() shouldBe """
        -- ModuleCheck main source set depth results --
            depth    modules
            0        [:lib1]
            1        [:lib2]
            2        [:app]

        ModuleCheck found 0 issues
        """
  }

  @Test
  fun `debug source set depth should not be reported even if it's longer`() {

    val runner = runner(
      autoCorrect = false,
      findingFactory = findingFactory
    )

    val lib1 = project(":lib1")
    val debug1 = project(":debug1") {}

    val lib2 = project(":lib2") {
      addDependency(ConfigurationName.implementation, lib1)
    }
    val debug2 = project(":debug2") {
      addDependency(ConfigurationName.implementation, debug1)
      addDependency(ConfigurationName.implementation, lib2)
    }

    project(":app") {
      addDependency(ConfigurationName.implementation, lib1)
      addDependency(ConfigurationName.implementation, lib2)
      addDependency(ConfigurationName("debugImplementation"), debug2)
    }

    runner.run(allProjects())

    logger.collectReport()
      .joinToString()
      .clean() shouldBe """
        -- ModuleCheck main source set depth results --
            depth    modules
            0        [:debug1, :lib1]
            1        [:lib2]
            2        [:app, :debug2]

        ModuleCheck found 0 issues
        """
  }
}
