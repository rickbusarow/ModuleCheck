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

package modulecheck.reporting.graphviz

import io.kotest.inspectors.forAll
import modulecheck.model.dependency.ConfigurationName
import modulecheck.model.sourceset.SourceSetName
import modulecheck.project.McProject
import modulecheck.project.gen.maybeAddSourceSet
import modulecheck.rule.impl.DepthRule
import modulecheck.runtime.test.RunnerTest
import modulecheck.utils.child
import org.junit.jupiter.api.Test
import java.io.File

internal class GraphVizReportTest : RunnerTest() {

  override val rules = listOf(DepthRule())

  fun McProject.graphFile(sourceSet: String = "main"): File {
    return projectDir.child(
      "build",
      "reports",
      "modulecheck",
      "graphs",
      "$sourceSet.dot"
    )
  }

  @Test
  fun `graph report should not be created if disabled in settings`() {

    settings.reports.graphs.enabled = false

    val lib1 = kotlinProject(":lib1")

    val lib2 = kotlinProject(":lib2") {
      addDependency(ConfigurationName.implementation, lib1)
    }

    kotlinProject(":app") {
      addDependency(ConfigurationName.implementation, lib1)
      addDependency(ConfigurationName.implementation, lib2)
    }

    run(autoCorrect = false).isSuccess shouldBe true

    lib2.graphFile().exists() shouldBe false
  }

  @Test
  fun `graph should be created if enabled in settings`() {

    settings.reports.graphs.enabled = true

    val lib1 = kotlinProject(":lib1")

    val lib2 = kotlinProject(":lib2") {
      addDependency(ConfigurationName.implementation, lib1)
    }

    val app = kotlinProject(":app") {
      addDependency(ConfigurationName.implementation, lib1)
      addDependency(ConfigurationName.implementation, lib2)
    }

    run(autoCorrect = false).isSuccess shouldBe true

    app.graphFile() shouldHaveText """
      strict digraph {
        edge ["dir"="forward"]
        graph ["ratio"="0.5625","rankdir"="TB","label"=<<b>:app -- main</b>>,"labelloc"="t"]
        node ["style"="rounded,filled","shape"="box"]
        {
          edge ["dir"="none"]
          graph ["rank"="same"]
          ":lib1" ["fillcolor"="#F89820"]
        }
        {
          edge ["dir"="none"]
          graph ["rank"="same"]
          ":lib2" ["fillcolor"="#F89820"]
        }
        {
          edge ["dir"="none"]
          graph ["rank"="same"]
          ":app" ["fillcolor"="#F89820"]
        }
        ":app" -> ":lib2" ["arrowhead"="normal","style"="bold","color"="#FF6347"]
        ":app" -> ":lib1" ["arrowhead"="normal","style"="bold","color"="#FF6347"]
        ":lib2" -> ":lib1" ["arrowhead"="normal","style"="bold","color"="#FF6347"]
      }
    """
  }

  // https://github.com/RBusarow/ModuleCheck/issues/575
  @Test
  fun `custom report dir should put all graphs in relative directories`() {

    val graphsDir = testProjectDir.child("graphs")
    fun graph(project: McProject, sourceSetName: SourceSetName): File {
      return graphsDir.child(
        project.path.value.removePrefix(":"),
        "${sourceSetName.value}.dot"
      )
    }

    settings.reports.graphs.enabled = true
    settings.reports.graphs.outputDir = graphsDir.path

    val lib1 = kotlinProject(":lib1")

    val lib2 = kotlinProject(":lib2") {
      addDependency(ConfigurationName.implementation, lib1)
    }

    val app = kotlinProject(":app") {
      addDependency(ConfigurationName.implementation, lib1)
      addDependency(ConfigurationName.implementation, lib2)
    }

    run(autoCorrect = false).isSuccess shouldBe true

    // These are the simple, no-dependency graphs
    listOf(
      lib1 to SourceSetName.MAIN,
      lib1 to SourceSetName.TEST,
      lib2 to SourceSetName.TEST,
      app to SourceSetName.TEST
    ).forAll { (project, sourceSet) ->

      graph(project, sourceSet) shouldHaveText """
        strict digraph {
          edge ["dir"="forward"]
          graph ["ratio"="0.5625","rankdir"="TB","label"=<<b>${project.path.value} -- ${sourceSet.value}</b>>,"labelloc"="t"]
          node ["style"="rounded,filled","shape"="box"]
          {
            edge ["dir"="none"]
            graph ["rank"="same"]
            "${project.path.value}" ["fillcolor"="#F89820"]
          }
        }
        """
    }

    graph(lib2, SourceSetName.MAIN) shouldHaveText """
      strict digraph {
        edge ["dir"="forward"]
        graph ["ratio"="0.5625","rankdir"="TB","label"=<<b>:lib2 -- main</b>>,"labelloc"="t"]
        node ["style"="rounded,filled","shape"="box"]
        {
          edge ["dir"="none"]
          graph ["rank"="same"]
          ":lib1" ["fillcolor"="#F89820"]
        }
        {
          edge ["dir"="none"]
          graph ["rank"="same"]
          ":lib2" ["fillcolor"="#F89820"]
        }
        ":lib2" -> ":lib1" ["arrowhead"="normal","style"="bold","color"="#FF6347"]
      }
    """

    graph(app, SourceSetName.MAIN) shouldHaveText """
      strict digraph {
        edge ["dir"="forward"]
        graph ["ratio"="0.5625","rankdir"="TB","label"=<<b>:app -- main</b>>,"labelloc"="t"]
        node ["style"="rounded,filled","shape"="box"]
        {
          edge ["dir"="none"]
          graph ["rank"="same"]
          ":lib1" ["fillcolor"="#F89820"]
        }
        {
          edge ["dir"="none"]
          graph ["rank"="same"]
          ":lib2" ["fillcolor"="#F89820"]
        }
        {
          edge ["dir"="none"]
          graph ["rank"="same"]
          ":app" ["fillcolor"="#F89820"]
        }
        ":app" -> ":lib2" ["arrowhead"="normal","style"="bold","color"="#FF6347"]
        ":app" -> ":lib1" ["arrowhead"="normal","style"="bold","color"="#FF6347"]
        ":lib2" -> ":lib1" ["arrowhead"="normal","style"="bold","color"="#FF6347"]
      }
    """
  }

  @Test
  fun `graph should be created for zero-depth source sets if the source set is not empty`() {

    settings.reports.graphs.enabled = true

    val lib1 = kotlinProject(":lib1") {
      addKotlinSource("", directory = "com/test", fileName = "MyFile.kt")
    }

    run(autoCorrect = false).isSuccess shouldBe true

    lib1.graphFile() shouldHaveText """
      strict digraph {
        edge ["dir"="forward"]
        graph ["ratio"="0.5625","rankdir"="TB","label"=<<b>:lib1 -- main</b>>,"labelloc"="t"]
        node ["style"="rounded,filled","shape"="box"]
        {
          edge ["dir"="none"]
          graph ["rank"="same"]
          ":lib1" ["fillcolor"="#F89820"]
        }
      }
    """
  }

  @Test
  fun `graph should be created for an existing source set with no files`() {

    settings.reports.graphs.enabled = true

    val lib1 = kotlinProject(":lib1") {
      addKotlinSource("", directory = "com/test", fileName = "MyFile.kt")
    }

    run(autoCorrect = false).isSuccess shouldBe true

    lib1.graphFile() shouldHaveText """
      strict digraph {
        edge ["dir"="forward"]
        graph ["ratio"="0.5625","rankdir"="TB","label"=<<b>:lib1 -- main</b>>,"labelloc"="t"]
        node ["style"="rounded,filled","shape"="box"]
        {
          edge ["dir"="none"]
          graph ["rank"="same"]
          ":lib1" ["fillcolor"="#F89820"]
        }
      }
    """
  }

  @Test
  fun `test source set graph should be test-specific`() {

    settings.reports.graphs.enabled = true

    val lib1 = kotlinProject(":lib1") {
      maybeAddSourceSet(SourceSetName.TEST)
    }

    val lib2 = kotlinProject(":lib2") {
      addDependency(ConfigurationName.implementation, lib1)
    }

    val test1 = kotlinProject(":test1") {
      addDependency(ConfigurationName.implementation, lib1)
      addDependency(ConfigurationName.implementation, lib2)
    }

    val app = kotlinProject(":app") {
      addDependency(ConfigurationName.implementation, lib1)
      addDependency(ConfigurationName.implementation, lib2)
      addDependency(ConfigurationName.testImplementation, test1)
    }

    lib1.addDependency(ConfigurationName("testImplementation"), lib2)

    run(autoCorrect = false).isSuccess shouldBe true

    app.graphFile() shouldHaveText """
      strict digraph {
        edge ["dir"="forward"]
        graph ["ratio"="0.5625","rankdir"="TB","label"=<<b>:app -- main</b>>,"labelloc"="t"]
        node ["style"="rounded,filled","shape"="box"]
        {
          edge ["dir"="none"]
          graph ["rank"="same"]
          ":lib1" ["fillcolor"="#F89820"]
        }
        {
          edge ["dir"="none"]
          graph ["rank"="same"]
          ":lib2" ["fillcolor"="#F89820"]
        }
        {
          edge ["dir"="none"]
          graph ["rank"="same"]
          ":app" ["fillcolor"="#F89820"]
        }
        ":app" -> ":lib2" ["arrowhead"="normal","style"="bold","color"="#FF6347"]
        ":app" -> ":lib1" ["arrowhead"="normal","style"="bold","color"="#FF6347"]
        ":lib2" -> ":lib1" ["arrowhead"="normal","style"="bold","color"="#FF6347"]
      }
    """

    app.graphFile("test") shouldHaveText """
      strict digraph {
        edge ["dir"="forward"]
        graph ["ratio"="0.5625","rankdir"="TB","label"=<<b>:app -- test</b>>,"labelloc"="t"]
        node ["style"="rounded,filled","shape"="box"]
        {
          edge ["dir"="none"]
          graph ["rank"="same"]
          ":lib1" ["fillcolor"="#F89820"]
        }
        {
          edge ["dir"="none"]
          graph ["rank"="same"]
          ":lib2" ["fillcolor"="#F89820"]
        }
        {
          edge ["dir"="none"]
          graph ["rank"="same"]
          ":test1" ["fillcolor"="#F89820"]
        }
        {
          edge ["dir"="none"]
          graph ["rank"="same"]
          ":app" ["fillcolor"="#F89820"]
        }
        ":app" -> ":test1" ["arrowhead"="normal","style"="bold","color"="#FF6347"]
        ":test1" -> ":lib2" ["arrowhead"="normal","style"="bold","color"="#FF6347"]
        ":test1" -> ":lib1" ["arrowhead"="normal","style"="bold","color"="#FF6347"]
        ":lib2" -> ":lib1" ["arrowhead"="normal","style"="bold","color"="#FF6347"]
      }
    """

    lib1.graphFile("test") shouldHaveText """
      strict digraph {
        edge ["dir"="forward"]
        graph ["ratio"="0.5625","rankdir"="TB","label"=<<b>:lib1 -- test</b>>,"labelloc"="t"]
        node ["style"="rounded,filled","shape"="box"]
        {
          edge ["dir"="none"]
          graph ["rank"="same"]
          ":lib1" ["fillcolor"="#F89820"]
        }
        {
          edge ["dir"="none"]
          graph ["rank"="same"]
          ":lib2" ["fillcolor"="#F89820"]
        }
        ":lib1" -> ":lib2" ["arrowhead"="normal","style"="bold","color"="#FF6347"]
        ":lib2" -> ":lib1" ["arrowhead"="normal","style"="bold","color"="#FF6347"]
      }
    """
  }

  @Test
  fun `debug source set graph should be debug-specific`() {

    settings.reports.graphs.enabled = true

    val lib1 = kotlinProject(":lib1")
    val debug1 = kotlinProject(":debug1") {
      addDependency(ConfigurationName.implementation, lib1)
    }

    val lib2 = kotlinProject(":lib2") {
      addDependency(ConfigurationName.implementation, lib1)
    }
    val debug2 = kotlinProject(":debug2") {
      addDependency(ConfigurationName.implementation, debug1)
      addDependency(ConfigurationName.implementation, lib2)
    }

    val app = kotlinProject(":app") {
      addDependency(ConfigurationName.implementation, lib1)
      addDependency(ConfigurationName.implementation, lib2)
      addDependency(ConfigurationName("debugImplementation"), debug1)
      addDependency(ConfigurationName("debugImplementation"), debug2)
    }

    run(autoCorrect = false).isSuccess shouldBe true

    app.graphFile() shouldHaveText """
      strict digraph {
        edge ["dir"="forward"]
        graph ["ratio"="0.5625","rankdir"="TB","label"=<<b>:app -- main</b>>,"labelloc"="t"]
        node ["style"="rounded,filled","shape"="box"]
        {
          edge ["dir"="none"]
          graph ["rank"="same"]
          ":lib1" ["fillcolor"="#F89820"]
        }
        {
          edge ["dir"="none"]
          graph ["rank"="same"]
          ":lib2" ["fillcolor"="#F89820"]
        }
        {
          edge ["dir"="none"]
          graph ["rank"="same"]
          ":app" ["fillcolor"="#F89820"]
        }
        ":app" -> ":lib2" ["arrowhead"="normal","style"="bold","color"="#FF6347"]
        ":app" -> ":lib1" ["arrowhead"="normal","style"="bold","color"="#FF6347"]
        ":lib2" -> ":lib1" ["arrowhead"="normal","style"="bold","color"="#FF6347"]
      }
    """

    app.graphFile("debug") shouldHaveText """
      strict digraph {
        edge ["dir"="forward"]
        graph ["ratio"="0.5625","rankdir"="TB","label"=<<b>:app -- debug</b>>,"labelloc"="t"]
        node ["style"="rounded,filled","shape"="box"]
        {
          edge ["dir"="none"]
          graph ["rank"="same"]
          ":lib1" ["fillcolor"="#F89820"]
        }
        {
          edge ["dir"="none"]
          graph ["rank"="same"]
          ":debug1" ["fillcolor"="#F89820"]
          ":lib2" ["fillcolor"="#F89820"]
        }
        {
          edge ["dir"="none"]
          graph ["rank"="same"]
          ":debug2" ["fillcolor"="#F89820"]
        }
        {
          edge ["dir"="none"]
          graph ["rank"="same"]
          ":app" ["fillcolor"="#F89820"]
        }
        ":app" -> ":debug2" ["arrowhead"="normal","style"="bold","color"="#FF6347"]
        ":app" -> ":debug1" ["arrowhead"="normal","style"="bold","color"="#FF6347"]
        ":debug1" -> ":lib1" ["arrowhead"="normal","style"="bold","color"="#FF6347"]
        ":debug2" -> ":lib2" ["arrowhead"="normal","style"="bold","color"="#FF6347"]
        ":debug2" -> ":debug1" ["arrowhead"="normal","style"="bold","color"="#FF6347"]
        ":lib2" -> ":lib1" ["arrowhead"="normal","style"="bold","color"="#FF6347"]
      }
    """
  }
}
