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

package modulecheck.parsing.kotlin.compiler.impl

import io.kotest.matchers.types.shouldBeSameInstanceAs
import modulecheck.parsing.gradle.model.ConfigurationName
import modulecheck.parsing.gradle.model.SourceSetName
import modulecheck.parsing.kotlin.compiler.internal.isKotlinFile
import modulecheck.project.test.ProjectTest
import org.jetbrains.kotlin.cli.common.config.kotlinSourceRoots
import org.junit.jupiter.api.Test

class RealKotlinEnvironmentTest : ProjectTest() {

  @Test
  fun `a java-only project has an empty BindingContext`() = test {

    val first = kotlinProject(":first") {
      addKotlinSource(
        """
        package com.first

        object Butt2()
        """,
        fileName = "Butt2.kt"
      )
    }
    val lib1 = javaProject(":lib1") {
      addJavaSource(
        """
        package com.lib1;

        class Source{}
        """
      )
    }
    val lib2 = kotlinProject(":lib2") {
      addDependency(ConfigurationName.api, lib1)

      addJavaSource(
        """
        package com.lib2;

        import com.lib1.Source;

        class Source{}
        """
      )
      addKotlinSource("""object Butt""", fileName = "Butt.kt")
    }

    val ke = lib2.sourceSets.getValue(SourceSetName.MAIN).kotlinEnvironmentDeferred.await()

    val compilerConfiguration = ke.compilerConfiguration
    val intelliJProject = ke.coreEnvironment.project

    val ktf1 = lib2.sourceSets[SourceSetName.MAIN]!!.jvmFiles
      .first { it.isKotlinFile() }
    val javaf1 = lib2.sourceSets[SourceSetName.MAIN]!!.jvmFiles
      .first { it.isKotlinFile() }

    val sourceFromCC = compilerConfiguration.kotlinSourceRoots

    val factory = RealMcPsiFileFactory(ke)

    val kt1 = factory.createKotlin(ktf1)
    val kt2 = factory.createKotlin(ktf1)

    kt1 shouldBeSameInstanceAs kt2

    val java1 = factory.createKotlin(javaf1)
    val java2 = factory.createKotlin(javaf1)

    java1 shouldBeSameInstanceAs java2

    require(java1 === java2) { "java" }

    ke.bindingContext.await()

    require(false) { "show me" }
  }
}
