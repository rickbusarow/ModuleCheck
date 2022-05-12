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

package modulecheck.core.context

import kotlinx.coroutines.flow.toList
import modulecheck.api.context.kaptDependencies
import modulecheck.api.context.referencesForSourceSetName
import modulecheck.config.CodeGeneratorBinding
import modulecheck.config.ModuleCheckSettings
import modulecheck.config.asMap
import modulecheck.core.kapt.UnusedKaptProcessorFinding
import modulecheck.core.kapt.defaultCodeGeneratorBindings
import modulecheck.finding.FindingName
import modulecheck.parsing.gradle.model.ConfigurationName
import modulecheck.parsing.source.AgnosticDeclaredName
import modulecheck.parsing.source.Reference
import modulecheck.project.McProject
import modulecheck.project.ProjectContext
import modulecheck.utils.LazySet
import modulecheck.utils.SafeCache
import modulecheck.utils.mapAsync
import modulecheck.utils.mapToSet

data class UnusedKaptProcessors(
  private val delegate: SafeCache<ConfigurationName, Set<UnusedKaptProcessorFinding>>,
  private val project: McProject
) : ProjectContext.Element {

  override val key: ProjectContext.Key<UnusedKaptProcessors>
    get() = Key

  suspend fun all(settings: ModuleCheckSettings): List<UnusedKaptProcessorFinding> {
    return project
      .configurations
      .keys
      .filter { it.value.startsWith("kapt") }
      .mapAsync { configurationName -> get(configurationName, settings) }
      .toList()
      .flatten()
      .distinct()
  }

  suspend fun get(
    configurationName: ConfigurationName,
    settings: ModuleCheckSettings
  ): Set<UnusedKaptProcessorFinding> {

    return delegate.getOrPut(configurationName) {

      val generatorBindings = settings.additionalCodeGenerators + defaultCodeGeneratorBindings()

      val matchers = generatorBindings.asMap()

      val kaptDependencies = project.kaptDependencies()

      val processors = kaptDependencies.get(configurationName)

      val references = project.referencesForSourceSetName(configurationName.toSourceSetName())

      processors
        .filterNot {
          val matcher = matchers[it.name] ?: return@filterNot true

          matcher.matchedIn(references)
        }
        .mapToSet { processor ->
          UnusedKaptProcessorFinding(
            findingName = FindingName("unused-kapt-processor"),
            dependentProject = project,
            dependentPath = project.path,
            buildFile = project.buildFile,
            oldDependency = processor,
            configurationName = configurationName
          )
        }
    }
  }

  private suspend fun CodeGeneratorBinding.matchedIn(
    references: LazySet<Reference>
  ): Boolean = annotationNames
    .any { annotationName ->
      references.contains(AgnosticDeclaredName(annotationName))
    }

  companion object Key : ProjectContext.Key<UnusedKaptProcessors> {
    override suspend operator fun invoke(project: McProject): UnusedKaptProcessors {

      return UnusedKaptProcessors(SafeCache(), project)
    }
  }
}

suspend fun ProjectContext.unusedKaptProcessors(): UnusedKaptProcessors = get(UnusedKaptProcessors)
