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

package modulecheck.rule.impl

import kotlinx.coroutines.flow.filterIsInstance
import modulecheck.api.context.jvmFilesForSourceSetName
import modulecheck.api.context.references
import modulecheck.config.ModuleCheckSettings
import modulecheck.finding.CouldUseAnvilFinding
import modulecheck.finding.FindingName
import modulecheck.model.sourceset.SourceSetName
import modulecheck.parsing.source.JavaFile
import modulecheck.parsing.source.KotlinFile
import modulecheck.parsing.source.McName.CompatibleLanguage.KOTLIN
import modulecheck.parsing.source.ReferenceName
import modulecheck.project.McProject
import modulecheck.utils.coroutines.any
import modulecheck.utils.lazy.containsAny
import net.swiftzer.semver.SemVer
import javax.inject.Inject

class AnvilFactoryRule @Inject constructor() : DocumentedRule<CouldUseAnvilFinding>() {

  override val name: FindingName = FindingName("use-anvil-factory-generation")
  override val description: String = "Finds modules which could use Anvil's factory generation " +
    "instead of Dagger's"

  private val anvilMergeComponent =
    ReferenceName("com.squareup.anvil.annotations.MergeComponent", KOTLIN)
  private val daggerComponent = ReferenceName("dagger.Component", KOTLIN)

  private val daggerInject = ReferenceName("dagger.Inject", KOTLIN)
  private val daggerModule = ReferenceName("dagger.Module", KOTLIN)

  @Suppress("MagicNumber")
  private val minimumAnvilVersion = SemVer(2, 0, 11)

  override suspend fun check(project: McProject): List<CouldUseAnvilFinding> {
    return parse(name, project)
  }

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

  override fun shouldApply(settings: ModuleCheckSettings): Boolean {
    return settings.checks.anvilFactoryGeneration
  }
}
