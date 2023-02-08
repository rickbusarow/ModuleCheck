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

package modulecheck.core

import modulecheck.model.dependency.ConfigurationName
import modulecheck.parsing.source.AnvilGradlePlugin
import modulecheck.runtime.test.ProjectFindingReport.unusedDependency
import modulecheck.runtime.test.RunnerTest
import net.swiftzer.semver.SemVer
import org.junit.jupiter.api.Test

class AnvilScopesTest : RunnerTest() {

  @Test
  fun `module which contributes anvil scopes should not be unused in module which merges that scope`() {

    val lib1 = kotlinProject(":lib1") {
      anvilGradlePlugin = AnvilGradlePlugin(SemVer.parse("2.4.0"), true)

      buildFile {
        """
        plugins {
          kotlin("jvm")
          id("com.squareup.anvil")
        }

        dependencies {
          implementation("javax.inject:javax.inject:1")
        }
        """
      }

      addKotlinSource(
        """
        package com.modulecheck.lib1

        import com.squareup.anvil.annotations.ContributesBinding
        import javax.inject.Inject

        @ContributesBinding(Unit::class)
        class Lib1FooImpl @Inject constructor(): Foo

        interface Foo
        """
      )
    }

    kotlinProject(":lib2") {
      addDependency(ConfigurationName.api, lib1)
      anvilGradlePlugin = AnvilGradlePlugin(SemVer.parse("2.4.0"), false)

      buildFile {
        """
        plugins {
          kotlin("jvm")
          kotlin("kapt")
          id("com.squareup.anvil")
        }

        dependencies {
          api(project(path = ":lib1"))
          implementation("com.google.dagger:dagger:2.38.1")
          kapt("com.google.dagger:dagger-compiler:2.38.1")
        }
        """
      }

      addKotlinSource(
        """
        package com.modulecheck.lib2

        import com.squareup.anvil.annotations.MergeComponent

        @MergeComponent(Unit::class)
        interface AppComponent
        """
      )
    }

    run(
      autoCorrect = false
    ).isSuccess shouldBe true
  }

  @Test
  fun `module which contributes anvil scopes with named argument should not be unused in module which merges that scope`() {

    val lib1 = kotlinProject(":lib1") {
      anvilGradlePlugin = AnvilGradlePlugin(SemVer.parse("2.4.0"), true)
      buildFile {
        """
        plugins {
          kotlin("jvm")
          id("com.squareup.anvil")
        }

        dependencies {
          implementation("javax.inject:javax.inject:1")
        }
        """
      }

      addKotlinSource(
        """
        package com.modulecheck.lib1

        import com.squareup.anvil.annotations.ContributesBinding
        import javax.inject.Inject

        @ContributesBinding(scope = Unit::class)
        class Lib1FooImpl @Inject constructor(): Foo

        interface Foo
        """
      )
    }

    kotlinProject(":lib2") {
      addDependency(ConfigurationName.api, lib1)
      anvilGradlePlugin = AnvilGradlePlugin(SemVer.parse("2.4.0"), false)

      buildFile {
        """
        plugins {
          kotlin("jvm")
          kotlin("kapt")
          id("com.squareup.anvil")
        }

        dependencies {
          api(project(path = ":lib1"))
          implementation("com.google.dagger:dagger:2.38.1")
          kapt("com.google.dagger:dagger-compiler:2.38.1")
        }
        """
      }

      addKotlinSource(
        """
        package com.modulecheck.lib2

        import com.squareup.anvil.annotations.MergeComponent

        @MergeComponent(scope = Unit::class)
        interface AppComponent
        """
      )
    }

    run(
      autoCorrect = false
    ).isSuccess shouldBe true
  }

  @Test
  fun `module which contributes anvil scopes with named argument and wrong order should not be unused in module which merges that scope`() {

    val lib1 = kotlinProject(":lib1") {
      anvilGradlePlugin = AnvilGradlePlugin(SemVer.parse("2.4.0"), true)
      buildFile {
        """
        plugins {
          kotlin("jvm")
          id("com.squareup.anvil")
        }

        dependencies {
          implementation("javax.inject:javax.inject:1")
        }
        """
      }

      addKotlinSource(
        """
        package com.modulecheck.lib1

        import com.squareup.anvil.annotations.ContributesBinding
        import javax.inject.Inject

        @ContributesBinding(boundType = Foo::class, scope = Unit::class)
        class Lib1FooImpl @Inject constructor(): Foo

        interface Foo
        """
      )
    }

    kotlinProject(":lib2") {
      addDependency(ConfigurationName.api, lib1)
      anvilGradlePlugin = AnvilGradlePlugin(SemVer.parse("2.4.0"), false)

      buildFile {
        """
        plugins {
          kotlin("jvm")
          kotlin("kapt")
          id("com.squareup.anvil")
        }

        dependencies {
          api(project(path = ":lib1"))
          implementation("com.google.dagger:dagger:2.38.1")
          kapt("com.google.dagger:dagger-compiler:2.38.1")
        }
        """
      }

      addKotlinSource(
        """
        package com.modulecheck.lib2

        import com.squareup.anvil.annotations.MergeComponent

        @MergeComponent(scope = Unit::class)
        interface AppComponent
        """
      )
    }

    run(
      autoCorrect = false
    ).isSuccess shouldBe true
  }

  @Test
  fun `module which contributes anvil scopes should be unused in module which does not merge that scope`() {

    val lib1 = kotlinProject(":lib1") {
      anvilGradlePlugin = AnvilGradlePlugin(SemVer.parse("2.4.0"), true)
      buildFile {
        """
        plugins {
          kotlin("jvm")
          id("com.squareup.anvil")
        }

        dependencies {
          implementation("javax.inject:javax.inject:1")
        }
        """
      }

      addKotlinSource(
        """
        package com.modulecheck.lib1

        import com.squareup.anvil.annotations.ContributesBinding
        import javax.inject.Inject

        @ContributesBinding(boundType = Foo::class, scope = Unit::class)
        class Lib1FooImpl @Inject constructor(): Foo

        interface Foo
        """
      )
    }

    kotlinProject(":lib2") {
      addDependency(ConfigurationName.api, lib1)
      anvilGradlePlugin = AnvilGradlePlugin(SemVer.parse("2.4.0"), false)

      buildFile {
        """
        plugins {
          kotlin("jvm")
          kotlin("kapt")
          id("com.squareup.anvil")
        }

        dependencies {
          api(project(path = ":lib1"))
          implementation("com.google.dagger:dagger:2.38.1")
          kapt("com.google.dagger:dagger-compiler:2.38.1")
        }
        """
      }

      addKotlinSource(
        """
        package com.modulecheck.lib2

        import com.squareup.anvil.annotations.MergeComponent

        // This is a different scope
        @MergeComponent(scope = String::class)
        interface AppComponent
        """
      )
    }

    run(
      autoCorrect = false
    ).isSuccess shouldBe false

    logger.parsedReport() shouldBe listOf(
      ":lib2" to listOf(
        unusedDependency(
          fixed = false,
          configuration = "api",
          dependency = ":lib1",
          position = "8, 3"
        )
      )
    )
  }
}
