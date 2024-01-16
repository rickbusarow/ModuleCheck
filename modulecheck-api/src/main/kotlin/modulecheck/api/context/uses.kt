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

package modulecheck.api.context

import modulecheck.model.dependency.ConfiguredDependency
import modulecheck.model.dependency.ExternalDependency.ExternalCodeGeneratorDependency
import modulecheck.model.dependency.ExternalDependency.ExternalRuntimeDependency
import modulecheck.model.dependency.MightHaveCodeGeneratorBinding
import modulecheck.model.dependency.ProjectDependency.CodeGeneratorProjectDependency
import modulecheck.model.dependency.ProjectDependency.RuntimeProjectDependency
import modulecheck.model.sourceset.SourceSetName
import modulecheck.parsing.source.DeclaredName
import modulecheck.parsing.source.Generated
import modulecheck.parsing.source.PackageName
import modulecheck.parsing.source.SimpleName.Companion.asSimpleName
import modulecheck.project.McProject
import modulecheck.utils.lazy.LazySet

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
      val split = annotationName.split('.')

      refs.contains(
        DeclaredName.agnostic(
          packageName = PackageName(split.dropLast(1).joinToString(".")),
          simpleNames = listOf(split.last().asSimpleName())
        )
      )
    }
}

private suspend fun McProject.usesRuntimeDependency(dependency: RuntimeProjectDependency): Boolean {

  val dependencyDeclarations = dependency.declarations(projectCache)

  val referencesSourceSetName = dependency.configurationName.toSourceSetName()

  val refs = referencesForSourceSetName(referencesSourceSetName)

  // Check whether human-written code references the dependency first.
  val usedInStaticSource = refs.containsAny(dependencyDeclarations)

  if (usedInStaticSource) return true

  // Any references in the receiver project's generated code
  // which reference a declaration from this dependency
  val generatedRefs = generatedDeclarations(
    sourceSetName = referencesSourceSetName,
    sourceDeclarations = dependencyDeclarations
  )

  val usedForGeneration = when {
    generatedRefs.isEmpty() -> false
    refs.containsAny(generatedRefs) -> true
    else -> generationIsUsedDownstream(
      generatedRefs = generatedRefs,
      referencesSourceSetName = referencesSourceSetName
    )
  }

  if (usedForGeneration) return true

  // If there are no references is manually/human written static code, then parse the Anvil graph.
  val anvilContributions = dependency.project()
    .anvilScopeContributionsForSourceSetName(SourceSetName.MAIN)

  return anvilGraph()
    .mergedScopeNames()
    .any { anvilContributions.containsKey(it) }
}

private suspend fun McProject.generationIsUsedDownstream(
  generatedRefs: LazySet<Generated>,
  referencesSourceSetName: SourceSetName
): Boolean {

  return dependents()
    .asSequence()
    .map { downstreamDependency ->

      val downstreamProject = projectCache.getValue(downstreamDependency.dependentProjectPath)
      val downstreamSourceSet = downstreamDependency.projectDependency
        .declaringSourceSetName(downstreamProject.sourceSets)

      downstreamProject to downstreamSourceSet
    }
    .filter { (_, downstreamSourceSet: SourceSetName) ->
      downstreamSourceSet == referencesSourceSetName
    }
    .distinct()
    .any { (downstreamProject, downstreamSourceSet) ->

      downstreamProject
        .referencesForSourceSetName(downstreamSourceSet)
        .containsAny(generatedRefs)
    }
}
