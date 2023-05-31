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

package modulecheck.api.context

import kotlinx.coroutines.flow.asFlow
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.filterIsInstance
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.mapNotNull
import modulecheck.model.dependency.ProjectDependency
import modulecheck.model.sourceset.SourceSetName
import modulecheck.parsing.source.AnvilScopeName
import modulecheck.parsing.source.AnvilScopeNameEntry
import modulecheck.parsing.source.JvmFile
import modulecheck.parsing.source.KotlinFile
import modulecheck.parsing.source.McName.CompatibleLanguage.KOTLIN
import modulecheck.parsing.source.QualifiedDeclaredName
import modulecheck.parsing.source.RawAnvilAnnotatedType
import modulecheck.parsing.source.ReferenceName
import modulecheck.project.McProject
import modulecheck.project.ProjectContext
import modulecheck.project.project
import modulecheck.utils.cache.SafeCache
import org.jetbrains.kotlin.name.FqName

/**
 * Represents a set of Anvil scope declarations including both contributions and merges.
 * @property scopeName the [AnvilScopeName] for these declarations
 * @property contributions mutable set of [QualifiedDeclaredName]s that contribute to this scope
 * @property merges mutable set of [QualifiedDeclaredName]s that are merged into this scope
 */
data class AnvilScopedDeclarations(
  val scopeName: AnvilScopeName,
  val contributions: MutableSet<QualifiedDeclaredName>,
  val merges: MutableSet<QualifiedDeclaredName>
)

/**
 * Represents a graph of Anvil scopes and their associated declarations.
 * @property project the [McProject] for which this graph is defined
 * @property delegate a cache that maps from [SourceSetName] to a map of [AnvilScopeName] to [AnvilScopedDeclarations]
 */
data class AnvilGraph(
  private val project: McProject,
  private val delegate: SafeCache<SourceSetName, Map<AnvilScopeName, AnvilScopedDeclarations>>
) : ProjectContext.Element {

  private val contributeAnnotations = setOf(
    "com.squareup.anvil.annotations.ContributesTo",
    "com.squareup.anvil.annotations.ContributesBinding",
    "com.squareup.anvil.annotations.ContributesMultibinding",
    "com.squareup.anvil.annotations.ContributesSubcomponent"
  ).map { ReferenceName(it, KOTLIN) }

  private val mergeAnnotations = setOf(
    "com.squareup.anvil.annotations.compat.MergeInterfaces",
    "com.squareup.anvil.annotations.compat.MergeModules",
    "com.squareup.anvil.annotations.MergeComponent",
    "com.squareup.anvil.annotations.MergeSubcomponent"
  ).map { ReferenceName(it, KOTLIN) }

  private val allAnnotations = mergeAnnotations + contributeAnnotations

  /**
   * @return the unique key of the Anvil graph within the project context
   */
  override val key: ProjectContext.Key<AnvilGraph>
    get() = Key

  /**
   * Retrieves a list of all Anvil scoped declarations for each source set in the project.
   * @return a list of maps from [AnvilScopeName] to [AnvilScopedDeclarations] for each source set
   */
  suspend fun all(): List<Map<AnvilScopeName, AnvilScopedDeclarations>> {
    return project.sourceSets.keys.map { get(it) }
  }

  /**
   * Retrieves a list of all Anvil scope names that have merges.
   * @return a list of [AnvilScopeName]s with merges
   */
  suspend fun mergedScopeNames(): List<AnvilScopeName> = all()
    .asSequence()
    .flatMap { it.values }
    .filter { it.merges.isNotEmpty() }
    .map { it.scopeName }
    .toList()

  /**
   * Retrieves the Anvil scoped declarations for a specific source set.
   * @param sourceSetName the [SourceSetName] for which to get the Anvil scoped declarations
   * @return a map from [AnvilScopeName] to [AnvilScopedDeclarations] for the given source set
   */
  suspend fun get(sourceSetName: SourceSetName): Map<AnvilScopeName, AnvilScopedDeclarations> {
    if (project.anvilGradlePlugin == null) return emptyMap()

    return delegate.getOrPut(sourceSetName) {
      project.declarationsForScopeName(sourceSetName)
    }
  }

  private suspend fun McProject.declarationsForScopeName(
    sourceSetName: SourceSetName
  ): MutableMap<AnvilScopeName, AnvilScopedDeclarations> {
    val map = mutableMapOf<AnvilScopeName, AnvilScopedDeclarations>()
    suspend fun RawAnvilAnnotatedType.declarations(
      sourceSetName: SourceSetName,
      kotlinFile: JvmFile
    ): AnvilScopedDeclarations {
      val scopeName = getAnvilScopeName(
        scopeNameEntry = anvilScopeNameEntry,
        sourceSetName = sourceSetName,
        kotlinFile = kotlinFile
      )

      return map.getOrPut(scopeName) {
        AnvilScopedDeclarations(
          scopeName = scopeName,
          contributions = mutableSetOf(),
          merges = mutableSetOf()
        )
      }
    }

    jvmFilesForSourceSetName(sourceSetName)
      // Anvil only works with Kotlin, so no point in trying to parse Java files
      .filterIsInstance<KotlinFile>()
      // only re-visit files which have Anvil annotations
      .filter { kotlinFile ->

        allAnnotations.any { annotationName ->
          kotlinFile.references.contains(annotationName)
        }
      }
      .collect { kotlinFile ->

        val (merged, contributed) = kotlinFile
          .getAnvilScopeArguments(allAnnotations, mergeAnnotations)

        merged
          .forEach { rawAnvilAnnotatedType ->

            val declarations = rawAnvilAnnotatedType.declarations(sourceSetName, kotlinFile)

            declarations.merges.add(rawAnvilAnnotatedType.declaredName)
          }
        contributed
          .forEach { rawAnvilAnnotatedType ->

            val declarations = rawAnvilAnnotatedType.declarations(sourceSetName, kotlinFile)

            declarations.contributions.add(rawAnvilAnnotatedType.declaredName)
          }
      }

    return map
  }

  private suspend fun McProject.getAnvilScopeName(
    scopeNameEntry: AnvilScopeNameEntry,
    sourceSetName: SourceSetName,
    kotlinFile: JvmFile
  ): AnvilScopeName {
    val dependenciesBySourceSetName = dependenciesBySourceSetName()

    val maybeExtraReferences = kotlinFile.references

    // if scope is directly resolved (most likely),
    // then use that fully qualified name
    val rawScopeName = kotlinFile.references
      .firstOrNull { reference -> reference.name.endsWith(scopeNameEntry.name.name) }
      ?.let { FqName(it.name) }
      // if the scope is wildcard-imported
      ?: dependenciesBySourceSetName[sourceSetName]
        .orEmpty()
        .asFlow()
        .mapNotNull { cpd ->
          cpd.project(project)
            .declarations()
            .get(SourceSetName.MAIN, includeUpstream = true)
            .filter { maybeExtraReferences.contains(it) }
            .firstOrNull { it.name.endsWith(scopeNameEntry.name.name) }
        }
        .firstOrNull()
        ?.let { FqName(it.name) }
      // Scope must be defined in this same module
      ?: maybeExtraReferences
        .mapNotNull { reference ->
          reference.takeIf { it.startsWith(kotlinFile.packageName) }
        }
        .firstOrNull { maybeExtra -> maybeExtra.endsWith(scopeNameEntry.name.name) }
        ?.let { FqName(it.name) }
      // Scope must be defined in this same package
      ?: FqName("${kotlinFile.packageName}.${scopeNameEntry.name}")

    return AnvilScopeName(rawScopeName)
  }

  private fun McProject.dependenciesBySourceSetName(): Map<SourceSetName, List<ProjectDependency>> {
    return configurations
      .map { (configurationName, _) ->
        configurationName.toSourceSetName() to projectDependencies[configurationName].orEmpty()
      }
      .groupBy { it.first }
      .map { (sourceSetName, pairs) ->
        sourceSetName to pairs.flatMap { it.second }
      }
      .toMap()
  }

  /**
   * Companion object that acts as the key for [AnvilGraph] within a [ProjectContext].
   */
  companion object Key : ProjectContext.Key<AnvilGraph> {
    /**
     * Generates an [AnvilGraph] instance for the given project.
     * @param project the project for which to generate the [AnvilGraph] instance
     * @return the generated [AnvilGraph] instance
     */
    override suspend operator fun invoke(project: McProject): AnvilGraph {
      return AnvilGraph(
        project = project,
        delegate = SafeCache(listOf(project.projectPath, AnvilGraph::class))
      )
    }
  }
}

/**
 * Extension function to retrieve [AnvilGraph] from the [ProjectContext].
 * @return the [AnvilGraph] instance from the context
 */
suspend fun ProjectContext.anvilGraph(): AnvilGraph = get(AnvilGraph)
