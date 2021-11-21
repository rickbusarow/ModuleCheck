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

import modulecheck.api.test.ProjectTest
import modulecheck.api.test.ReportingLogger
import modulecheck.api.test.TestSettings
import modulecheck.api.test.createSafely
import modulecheck.core.rule.DepthRule
import modulecheck.core.rule.SingleRuleFindingFactory
import modulecheck.parsing.*
import org.junit.jupiter.api.Test
import java.io.File

internal class GraphVizReportTest : ProjectTest() {

  val baseSettings by resets { TestSettings() }
  val logger by resets { ReportingLogger() }
  val findingFactory by resets { SingleRuleFindingFactory(DepthRule()) }

  fun McProject.graphFile(sourceSet: String = "main"): File {
    return projectDir.child(
      "build",
      "reports",
      "modulecheck",
      "graph",
      "$sourceSet.dot"
    )
  }

  @Test
  fun `graph report should not be created if disabled in settings`() {

    baseSettings.reports.graphs.enabled = false

    val runner = ModuleCheckRunner(
      autoCorrect = false,
      settings = baseSettings,
      findingFactory = findingFactory,
      logger = logger
    )

    val lib1 = project(":lib1")

    val lib2 = project(":lib2") {
      addDependency(ConfigurationName.implementation, lib1)
    }

    project(":app") {
      addDependency(ConfigurationName.implementation, lib1)
      addDependency(ConfigurationName.implementation, lib2)
    }

    val result = runner.run(allProjects())

    result.isSuccess shouldBe true

    lib2.graphFile().exists() shouldBe false
  }

  @Test
  fun `depth report should be created if enabled in settings`() {

    baseSettings.reports.graphs.enabled = true

    val runner = ModuleCheckRunner(
      autoCorrect = false,
      settings = baseSettings,
      findingFactory = findingFactory,
      logger = logger
    )

    val lib1 = project(":lib1")

    val lib2 = project(":lib2") {
      addDependency(ConfigurationName.implementation, lib1)
    }

    val app = project(":app") {
      addDependency(ConfigurationName.implementation, lib1)
      addDependency(ConfigurationName.implementation, lib2)
    }

    val result = runner.run(allProjects())

    result.isSuccess shouldBe true

    app.graphFile().readText() shouldBe """
      strict digraph DependencyGraph {
        ratio=0.5;
        node [style="rounded,filled" shape=box];

        ":app" [fillcolor = "#F89820"];
        ":lib1" [fillcolor = "#F89820"];
        ":lib2" [fillcolor = "#F89820"];

        ":app" -> ":lib1" [style = bold; color = "#007744"];
        ":app" -> ":lib2" [style = bold; color = "#007744"];

        ":lib2" -> ":lib1" [style = bold; color = "#007744"];

        {rank = same; ":lib1";}
        {rank = same; ":lib2";}
        {rank = same; ":app";}
      }
      """
  }

  @Test
  fun `graph should be created for zero-depth source sets if the source set is not empty`() {

    baseSettings.reports.graphs.enabled = true

    val runner = ModuleCheckRunner(
      autoCorrect = false,
      settings = baseSettings,
      findingFactory = findingFactory,
      logger = logger
    )

    val lib1 = project(":lib1") {

      val myFile = File(projectDir, "src/main/kotlin/MyFile.kt").createSafely()

      sourceSets[SourceSetName.MAIN] = SourceSet(
        name = SourceSetName.MAIN,
        jvmFiles = setOf(myFile)
      )
    }

    val result = runner.run(allProjects())

    result.isSuccess shouldBe true

    lib1.graphFile().readText() shouldBe """
      strict digraph DependencyGraph {
        ratio=0.5;
        node [style="rounded,filled" shape=box];

        ":lib1" [fillcolor = "#F89820"];

        {rank = same; ":lib1";}
      }
      """
  }

  @Test
  fun `graph should not be created for an existing source set if it has no files`() {

    baseSettings.reports.graphs.enabled = true

    val runner = ModuleCheckRunner(
      autoCorrect = false,
      settings = baseSettings,
      findingFactory = findingFactory,
      logger = logger
    )

    val lib1 = project(":lib1") {
      sourceSets[SourceSetName.MAIN] = SourceSet(name = SourceSetName.MAIN)
    }

    val result = runner.run(allProjects())

    result.isSuccess shouldBe true

    lib1.graphFile().exists() shouldBe false
  }

  @Test
  fun `test source set graph should be test-specific`() {

    baseSettings.reports.graphs.enabled = true

    val runner = ModuleCheckRunner(
      autoCorrect = false,
      settings = baseSettings,
      findingFactory = findingFactory,
      logger = logger
    )

    val lib1 = project(":lib1") {
      addSourceSet(SourceSetName.TEST)
    }

    val lib2 = project(":lib2") {
      addDependency(ConfigurationName.implementation, lib1)
    }

    val test1 = project(":test1") {
      addDependency(ConfigurationName.implementation, lib1)
      addDependency(ConfigurationName.implementation, lib2)
    }

    val app = project(":app") {
      addDependency(ConfigurationName.implementation, lib1)
      addDependency(ConfigurationName.implementation, lib2)
      addDependency(ConfigurationName.testImplementation, test1)
    }

    lib1.addDependency(ConfigurationName("testImplementation"), lib2)

    val result = runner.run(allProjects())

    result.isSuccess shouldBe true

    app.graphFile().readText() shouldBe """
      strict digraph DependencyGraph {
        ratio=0.5;
        node [style="rounded,filled" shape=box];

        ":app" [fillcolor = "#F89820"];
        ":lib1" [fillcolor = "#F89820"];
        ":lib2" [fillcolor = "#F89820"];

        ":app" -> ":lib1" [style = bold; color = "#007744"];
        ":app" -> ":lib2" [style = bold; color = "#007744"];

        ":lib2" -> ":lib1" [style = bold; color = "#007744"];

        {rank = same; ":lib1";}
        {rank = same; ":lib2";}
        {rank = same; ":app";}
      }
      """

    app.graphFile("test").readText() shouldBe """
      strict digraph DependencyGraph {
        ratio=0.5;
        node [style="rounded,filled" shape=box];

        ":app" [fillcolor = "#F89820"];
        ":lib1" [fillcolor = "#F89820"];
        ":lib2" [fillcolor = "#F89820"];
        ":test1" [fillcolor = "#F89820"];

        ":app" -> ":test1" [style = bold; color = "#000000"];

        ":lib2" -> ":lib1" [style = bold; color = "#007744"];

        ":test1" -> ":lib1" [style = bold; color = "#007744"];
        ":test1" -> ":lib2" [style = bold; color = "#007744"];

        {rank = same; ":lib1";}
        {rank = same; ":lib2";}
        {rank = same; ":test1";}
        {rank = same; ":app";}
      }
      """

    lib1.graphFile("test").readText() shouldBe """
      strict digraph DependencyGraph {
        ratio=0.5;
        node [style="rounded,filled" shape=box];

        ":lib1" [fillcolor = "#F89820"];
        ":lib2" [fillcolor = "#F89820"];

        ":lib1" -> ":lib2" [style = bold; color = "#000000"];

        ":lib2" -> ":lib1" [style = bold; color = "#007744"];

        {rank = same; ":lib2";}
        {rank = same; ":lib1";}
      }
      """
  }

  @Test
  fun `debug source set graph should be debug-specific`() {

    baseSettings.reports.graphs.enabled = true

    val runner = ModuleCheckRunner(
      autoCorrect = false,
      settings = baseSettings,
      findingFactory = findingFactory,
      logger = logger
    )

    val lib1 = project(":lib1")
    val debug1 = project(":debug1") {
      addDependency(ConfigurationName.implementation, lib1)
    }

    val lib2 = project(":lib2") {
      addDependency(ConfigurationName.implementation, lib1)
    }
    val debug2 = project(":debug2") {
      addDependency(ConfigurationName.implementation, debug1)
      addDependency(ConfigurationName.implementation, lib2)
    }

    val app = project(":app") {
      addDependency(ConfigurationName.implementation, lib1)
      addDependency(ConfigurationName.implementation, lib2)
      addDependency(ConfigurationName("debugImplementation"), debug1)
      addDependency(ConfigurationName("debugImplementation"), debug2)
    }

    val result = runner.run(allProjects())

    result.isSuccess shouldBe true

    app.graphFile().readText() shouldBe """
      strict digraph DependencyGraph {
        ratio=0.5;
        node [style="rounded,filled" shape=box];

        ":app" [fillcolor = "#F89820"];
        ":lib1" [fillcolor = "#F89820"];
        ":lib2" [fillcolor = "#F89820"];

        ":app" -> ":lib1" [style = bold; color = "#007744"];
        ":app" -> ":lib2" [style = bold; color = "#007744"];

        ":lib2" -> ":lib1" [style = bold; color = "#007744"];

        {rank = same; ":lib1";}
        {rank = same; ":lib2";}
        {rank = same; ":app";}
      }
      """

    app.graphFile("debug").readText() shouldBe """
      strict digraph DependencyGraph {
        ratio=0.5;
        node [style="rounded,filled" shape=box];

        ":app" [fillcolor = "#F89820"];
        ":debug1" [fillcolor = "#F89820"];
        ":debug2" [fillcolor = "#F89820"];
        ":lib1" [fillcolor = "#F89820"];
        ":lib2" [fillcolor = "#F89820"];

        ":app" -> ":debug1" [style = bold; color = "#000000"];
        ":app" -> ":debug2" [style = bold; color = "#000000"];

        ":debug1" -> ":lib1" [style = bold; color = "#007744"];

        ":debug2" -> ":debug1" [style = bold; color = "#007744"];
        ":debug2" -> ":lib2" [style = bold; color = "#007744"];

        ":lib2" -> ":lib1" [style = bold; color = "#007744"];

        {rank = same; ":lib1";}
        {rank = same; ":debug1"; ":lib2";}
        {rank = same; ":debug2";}
        {rank = same; ":app";}
      }
      """
  }
}
