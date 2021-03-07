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

package modulecheck.gradle

import modulecheck.api.Project2
import modulecheck.api.context.importsForSourceSetName
import modulecheck.api.context.jvmFilesForSourceSetName
import modulecheck.api.context.possibleReferencesForSourceSetName
import modulecheck.api.files.JavaFile
import modulecheck.api.files.KotlinFile
import modulecheck.core.CouldUseAnvilFinding
import net.swiftzer.semver.SemVer
import kotlin.LazyThreadSafetyMode.NONE

object AnvilFactoryParser {

  private const val anvilMergeComponent = "com.squareup.anvil.annotations.MergeComponent"
  private const val daggerComponent = "dagger.Component"
  private const val daggerInject = "dagger.Inject"
  private const val daggerModule = "dagger.Module"

  @Suppress("MagicNumber")
  private val minimumAnvilVersion = SemVer(2, 0, 11)

  @Suppress("ComplexMethod")
  fun parse(project: Project2): List<CouldUseAnvilFinding> {
    if (project !is Project2Gradle) return emptyList()

    val anvil = project.anvilGradlePlugin()

    if (anvil.generateDaggerFactories) return emptyList()

    val anvilVersion = anvil.version

    val hasAnvil = anvilVersion >= minimumAnvilVersion

    if (!hasAnvil) return emptyList()

    val allImports = project.importsForSourceSetName("main") +
      project.importsForSourceSetName("androidTest") +
      project.importsForSourceSetName("test")

    val maybeExtra by lazy(NONE) {
      project.possibleReferencesForSourceSetName("androidTest") +
        project.possibleReferencesForSourceSetName("main") +
        project.possibleReferencesForSourceSetName("test")
    }

    val createsComponent = allImports.contains(daggerComponent) ||
      allImports.contains(anvilMergeComponent) ||
      maybeExtra.contains(daggerComponent) ||
      maybeExtra.contains(anvilMergeComponent)

    if (createsComponent) return emptyList()

    val usesDaggerInJava = project
      .jvmFilesForSourceSetName("main")
      .filterIsInstance<JavaFile>()
      .any { file ->
        file.imports.contains(daggerInject) ||
          file.imports.contains(daggerModule) ||
          file.maybeExtraReferences.contains(daggerInject) ||
          file.maybeExtraReferences.contains(daggerModule)
      }

    if (usesDaggerInJava) return emptyList()

    val usesDaggerInKotlin = project
      .jvmFilesForSourceSetName("main")
      .filterIsInstance<KotlinFile>()
      .any { file ->
        file.imports.contains(daggerInject) ||
          file.imports.contains(daggerModule) ||
          file.maybeExtraReferences.contains(daggerInject) ||
          file.maybeExtraReferences.contains(daggerModule)
      }

    if (!usesDaggerInKotlin) return emptyList()

    val couldBeAnvil =
      !allImports.contains(daggerComponent) && !maybeExtra.contains(daggerComponent)

    return if (couldBeAnvil) {
      listOf(CouldUseAnvilFinding(project.buildFile, project.path))
    } else {
      listOf()
    }
  }
}
