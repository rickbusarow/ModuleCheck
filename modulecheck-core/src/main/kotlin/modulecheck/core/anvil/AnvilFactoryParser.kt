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

package modulecheck.core.anvil

import kotlinx.coroutines.flow.filterIsInstance
import modulecheck.api.context.jvmFilesForSourceSetName
import modulecheck.api.context.references
import modulecheck.finding.FindingName
import modulecheck.parsing.gradle.SourceSetName
import modulecheck.parsing.source.JavaFile
import modulecheck.parsing.source.KotlinFile
import modulecheck.parsing.source.asExplicitKotlinReference
import modulecheck.project.McProject
import modulecheck.utils.any
import modulecheck.utils.containsAny
import net.swiftzer.semver.SemVer

object AnvilFactoryParser {

  private val anvilMergeComponent =
    "com.squareup.anvil.annotations.MergeComponent".asExplicitKotlinReference()
  private val daggerComponent = "dagger.Component".asExplicitKotlinReference()

  private val daggerInject = "dagger.Inject".asExplicitKotlinReference()
  private val daggerModule = "dagger.Module".asExplicitKotlinReference()

  @Suppress("MagicNumber")
  private val minimumAnvilVersion = SemVer(2, 0, 11)

  @Suppress("ComplexMethod")
  suspend fun parse(findingName: FindingName, project: McProject): List<CouldUseAnvilFinding> {
    val anvil = project.anvilGradlePlugin ?: return emptyList()

    if (anvil.generateDaggerFactories) return emptyList()

    val anvilVersion = anvil.version

    val hasAnvil = anvilVersion >= minimumAnvilVersion

    if (!hasAnvil) return emptyList()

    val allRefs = project.references().all()

    val componentOrMergeComponent = listOf(daggerComponent, anvilMergeComponent)

    val createsComponent = allRefs.containsAny(componentOrMergeComponent)

    if (createsComponent) return emptyList()

    val moduleOrInjectConstructor = listOf(daggerInject, daggerModule)

    val usesDaggerInJava = project
      .jvmFilesForSourceSetName(SourceSetName.MAIN)
      .filterIsInstance<JavaFile>()
      .any { file -> file.references.containsAny(moduleOrInjectConstructor) }

    if (usesDaggerInJava) return emptyList()

    val usesDaggerInKotlin = project
      .jvmFilesForSourceSetName(SourceSetName.MAIN)
      .filterIsInstance<KotlinFile>()
      .any { file -> file.references.containsAny(moduleOrInjectConstructor) }

    if (!usesDaggerInKotlin) return emptyList()

    return listOf(
      CouldUseAnvilFinding(
        findingName = findingName,
        dependentProject = project,
        buildFile = project.buildFile
      )
    )
  }
}
