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

package modulecheck.parsing.kotlin.compiler.impl

import modulecheck.model.dependency.ConfigurationName
import modulecheck.model.dependency.MavenCoordinates
import modulecheck.parsing.source.McName
import modulecheck.parsing.test.McNameTest
import modulecheck.project.test.ProjectTest
import modulecheck.project.test.ProjectTestEnvironment
import modulecheck.reporting.logging.PrintLogger
import modulecheck.utils.lazy.ResetManager
import org.jetbrains.kotlin.config.JvmTarget.JVM_11
import org.jetbrains.kotlin.config.LanguageVersion.KOTLIN_1_7
import org.jetbrains.kotlin.resolve.descriptorUtil.fqNameOrNull
import org.jetbrains.kotlin.serialization.deserialization.descriptors.DeserializedClassDescriptor
import org.jetbrains.kotlin.serialization.deserialization.descriptors.DeserializedMemberDescriptor
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.fail

internal class ModuleDescriptorExperiment : ProjectTest(), McNameTest {

  override val defaultLanguage: McName.CompatibleLanguage
    get() = McName.CompatibleLanguage.KOTLIN

  val ProjectTestEnvironment.lib1
    get() = kotlinProject(":lib1") {
      addKotlinSource(
        """
        package com.lib1

        class Lib1Class
        """
      )
    }

  val ProjectTestEnvironment.project
    get() = kotlinProject(":subject") {
      addDependency(ConfigurationName.api, lib1)
    }

  val McNameTest.JvmFileBuilder.ReferenceBuilder.lib1Class
    get() = kotlin("com.lib1.Lib1Class")

  @Test
  fun `reading all declarations from an external dependency`() = test {

    val file = HostEnvironment.dispatchTest

    val env = List(2) {
      ExternalDependencyDescriptorImpl(
        MavenCoordinates("a", "b", "c"),
        file,
        kotlinLanguageVersion = KOTLIN_1_7,
        jvmTarget = JVM_11,
        logger = PrintLogger(),
        resetManager = ResetManager()
      )
    }

    val protos = env.map { externalDependencyDescriptor ->
      externalDependencyDescriptor.declarations.await()
        .map { declaration ->
          when (declaration) {
            is DeserializedClassDescriptor -> declaration.classProto
            is DeserializedMemberDescriptor -> declaration.proto
            else -> error("?? -- $declaration")
          }
        }
        .map { it.toByteString() }
    }

    env[0].declarations.await()
      .joinToString("\n") {
        "${it.fqNameOrNull()?.asString().orEmpty().padStart(70)} -- " +
          "${it::class.qualifiedName}"
      }
      // TODO <Rick> delete me
      .also(::println)

    fail("show me")
  }
}
