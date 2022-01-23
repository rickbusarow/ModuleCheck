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

package modulecheck.parsing.gradle

import modulecheck.utils.capitalize
import modulecheck.utils.mapToSet
import java.io.File

data class SourceSet(
  val name: SourceSetName,
  val classpathFiles: Set<File> = emptySet(),
  val outputFiles: Set<File> = emptySet(),
  val jvmFiles: Set<File> = emptySet(),
  val resourceFiles: Set<File> = emptySet(),
  val layoutFiles: Set<File> = emptySet()
) {
  fun hasExistingSourceFiles() = jvmFiles.hasExistingFiles() ||
    resourceFiles.hasExistingFiles() ||
    layoutFiles.hasExistingFiles()

  private fun Set<File>.hasExistingFiles(): Boolean {
    return any { dir ->
      dir.walkBottomUp()
        .any { file -> file.isFile && file.exists() }
    }
  }
}

@JvmInline
value class SourceSetName(val value: String) {

  fun javaConfigurationNames(): List<ConfigurationName> {

    return if (this == MAIN) {
      ConfigurationName.main()
    } else {
      ConfigurationName.mainConfigurations
        .map {
          "${this.value}${it.capitalize()}"
            .asConfigurationName()
        }
    }
  }

  fun apiConfig(): ConfigurationName {
    return if (this == MAIN) {
      ConfigurationName.api
    } else {
      "${value}Api".asConfigurationName()
    }
  }

  fun implementationConfig(): ConfigurationName {
    return if (this == MAIN) {
      ConfigurationName.implementation
    } else {
      "${value}Implementation".asConfigurationName()
    }
  }

  fun inheritedSourceSetNames(
    hasConfigurations: HasConfigurations,
    includeSelf: Boolean
  ): Set<SourceSetName> {
    val seed = if (includeSelf) {
      mutableSetOf(this, MAIN)
    } else {
      mutableSetOf(MAIN)
    }

    return javaConfigurationNames()
      .flatMapTo(seed) { configurationName ->
        hasConfigurations.configurations[configurationName]
          ?.inherited
          ?.mapToSet { inherited -> inherited.name.toSourceSetName() }
          .orEmpty()
      }
  }

  fun inheritsFrom(
    other: SourceSetName,
    hasConfigurations: HasConfigurations
  ): Boolean {

    val otherConfigNames = other.javaConfigurationNames()

    return javaConfigurationNames()
      .asSequence()
      .mapNotNull { hasConfigurations.configurations[it] }
      .map { config -> config.inherited.mapToSet { it.name } }
      .any { inheritedNames ->
        inheritedNames.any { inherited -> inherited in otherConfigNames }
      }
  }

  override fun toString(): String = "SourceSetName('$value')"

  companion object {
    val ANDROID_TEST = SourceSetName("androidTest")
    val ANVIL = SourceSetName("anvil")
    val DEBUG = SourceSetName("debug")
    val KAPT = SourceSetName("kapt")
    val MAIN = SourceSetName("main")
    val RELEASE = SourceSetName("release")
    val TEST = SourceSetName("test")
    val TEST_FIXTURES = SourceSetName("testFixtures")
  }
}

fun String.toSourceSetName(): SourceSetName = SourceSetName(this)

class SourceSets(
  delegate: Map<SourceSetName, SourceSet>
) : Map<SourceSetName, SourceSet> by delegate
