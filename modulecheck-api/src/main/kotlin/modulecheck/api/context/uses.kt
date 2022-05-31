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

package modulecheck.api.context

import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.filterIsInstance
import kotlinx.coroutines.flow.toSet
import modulecheck.config.MightHaveCodeGeneratorBinding
import modulecheck.model.dependency.ConfiguredDependency
import modulecheck.model.dependency.ExternalDependency.ExternalCodeGeneratorDependency
import modulecheck.model.dependency.ExternalDependency.ExternalRuntimeDependency
import modulecheck.model.dependency.ProjectDependency.CodeGeneratorProjectDependency
import modulecheck.model.dependency.ProjectDependency.RuntimeProjectDependency
import modulecheck.parsing.gradle.model.SourceSetName
import modulecheck.parsing.source.AgnosticDeclaredName
import modulecheck.parsing.source.Generated
import modulecheck.project.McProject
import modulecheck.project.isAndroid
import modulecheck.utils.coroutines.any
import modulecheck.utils.lazy.containsAny
import modulecheck.utils.lazy.dataSource
import modulecheck.utils.lazy.lazySet

suspend fun McProject.uses(dependency: ConfiguredDependency): Boolean {

  return when (dependency) {
    is RuntimeProjectDependency -> usesRuntimeDependency(dependency)
    is ExternalRuntimeDependency -> {
      // External runtime dependencies aren't parsed yet, so treat them all as used.
      true
    }

    is ExternalCodeGeneratorDependency -> usesCodeGenDependency(dependency)
    is CodeGeneratorProjectDependency -> usesCodeGenDependency(dependency)
  }
}

private suspend fun <T> McProject.usesCodeGenDependency(
  dependency: T
): Boolean where T : ConfiguredDependency,
                 T : MightHaveCodeGeneratorBinding {

  val codeGeneratorBinding = dependency.codeGeneratorBindingOrNull
    // If the dependency doesn't have a binding, default to treating it as used
    ?: return true

  val referencesSourceSetName = dependency.configurationName.toSourceSetName()

  val refs = referencesForSourceSetName(referencesSourceSetName)

  return codeGeneratorBinding.annotationNames
    .any { annotationName ->
      refs.contains(AgnosticDeclaredName(annotationName))
    }
}

private suspend fun McProject.usesRuntimeDependency(dependency: RuntimeProjectDependency): Boolean {

  val dependencyDeclarations = dependency.declarations(projectCache)

  val referencesSourceSetName = dependency.configurationName.toSourceSetName()

  val refs = referencesForSourceSetName(referencesSourceSetName)

  // Check whether human-written code references the dependency first.
  val usedInStaticSource = refs
    .any { reference -> dependencyDeclarations.contains(reference) }

  if (usedInStaticSource) return true

  // Any generated code from the receiver project which requires a declaration from the dependency
  val generatedFromThisDependency = lazySet(
    dataSource {

      // TODO - probably make "all generated declarations" its own ProjectContext.Element,
      //  specifically targeting generated declarations.  It shouldn't be needed in this specific
      //  case, since `dependencyDeclarations` should already be fully cached by the time we get
      //  here, and we have to iterate over the flow anyway in order to filter again.
      declarations().get(
        dependency.declaringSourceSetName(dependency.project().isAndroid()),
        includeUpstream = true
      )
        .filterIsInstance<Generated>()
        .filter { dependencyDeclarations.containsAny(it.sources) }
        .toSet()
    }
  )

  val usedUpstream = generatedFromThisDependency.isNotEmpty() && dependents()
    .any { downstreamDependency ->

      val downstreamProject = downstreamDependency.projectDependency.project()

      val downstreamSourceSet = downstreamDependency.projectDependency
        .declaringSourceSetName(downstreamProject.isAndroid())

      projectCache.getValue(downstreamDependency.dependentProjectPath)
        .referencesForSourceSetName(downstreamSourceSet)
        .containsAny(generatedFromThisDependency)
    }

  if (usedUpstream) return true

  // If there are no references is manually/human written static code, then parse the Anvil graph.
  val anvilContributions = dependency.project()
    .anvilScopeContributionsForSourceSetName(SourceSetName.MAIN)

  return anvilGraph()
    .mergedScopeNames()
    .any { anvilContributions.containsKey(it) }
}
