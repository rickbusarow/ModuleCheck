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

import kotlinx.coroutines.flow.asFlow
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.filterIsInstance
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.mapNotNull
import kotlinx.coroutines.runBlocking
import modulecheck.parsing.gradle.SourceSetName
import modulecheck.parsing.source.AnvilScopeName
import modulecheck.parsing.source.AnvilScopeNameEntry
import modulecheck.parsing.source.DeclarationName
import modulecheck.parsing.source.JvmFile
import modulecheck.parsing.source.KotlinFile
import modulecheck.parsing.source.RawAnvilAnnotatedType
import modulecheck.parsing.source.contains
import modulecheck.project.ConfiguredProjectDependency
import modulecheck.project.McProject
import modulecheck.project.ProjectContext
import modulecheck.utils.SafeCache
import org.jetbrains.kotlin.name.FqName
import kotlin.LazyThreadSafetyMode.NONE

data class AnvilScopedDeclarations(
  val scopeName: AnvilScopeName,
  val contributions: MutableSet<DeclarationName>,
  val merges: MutableSet<DeclarationName>
)

data class AnvilGraph(
  private val project: McProject,
  private val delegate: SafeCache<SourceSetName, Map<AnvilScopeName, AnvilScopedDeclarations>>
) : ProjectContext.Element {

  private val contributeAnnotations = setOf(
    "com.squareup.anvil.annotations.ContributesTo",
    "com.squareup.anvil.annotations.ContributesBinding",
    "com.squareup.anvil.annotations.ContributesMultibinding"
  )

  private val mergeAnnotations = setOf(
    "com.squareup.anvil.annotations.MergeComponent",
    "com.squareup.anvil.annotations.MergeSubcomponent"
  )

  val allAnnotations = mergeAnnotations + contributeAnnotations

  override val key: ProjectContext.Key<AnvilGraph>
    get() = Key

  suspend fun all(): List<Map<AnvilScopeName, AnvilScopedDeclarations>> {
    return project.sourceSets.keys.map { get(it) }
  }

  suspend fun mergedScopeNames(): List<AnvilScopeName> = all()
    .asSequence()
    .flatMap { it.values }
    .filter { it.merges.isNotEmpty() }
    .map { it.scopeName }
    .toList()

  suspend fun get(
    sourceSetName: SourceSetName
  ): Map<AnvilScopeName, AnvilScopedDeclarations> {
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
          kotlinFile.importsLazy.value.contains(annotationName) ||
            kotlinFile.interpretedReferencesLazy.value.contains(annotationName)
        }
      }
      .collect { kotlinFile ->

        val (merged, contributed) = kotlinFile
          .getScopeArguments(allAnnotations, mergeAnnotations)

        merged
          .forEach { rawAnvilAnnotatedType ->

            val declarations = rawAnvilAnnotatedType.declarations(sourceSetName, kotlinFile)

            declarations.merges.add(rawAnvilAnnotatedType.declarationName)
          }
        contributed
          .forEach { rawAnvilAnnotatedType ->

            val declarations = rawAnvilAnnotatedType.declarations(sourceSetName, kotlinFile)

            declarations.contributions.add(rawAnvilAnnotatedType.declarationName)
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

    val maybeExtraReferences by lazy(NONE) {
      runBlocking { kotlinFile.interpretedReferencesLazy }
    }

    // if scope is directly imported (most likely),
    // then use that fully qualified import
    val rawScopeName = kotlinFile.importsLazy.value
      .firstNotNullOfOrNull { it.endingWith(scopeNameEntry.name).firstOrNull() }
      ?.let { FqName(it) }
      // if the scope is wildcard-imported
      ?: dependenciesBySourceSetName[sourceSetName]
        .orEmpty()
        .asFlow()
        .mapNotNull { cpd ->
          cpd.project
            .declarations()
            .get(SourceSetName.MAIN)
            .filter { maybeExtraReferences.value.contains(it) }
            .firstOrNull { it.fqName.endsWith(scopeNameEntry.name) }
        }
        .firstOrNull()
        ?.let { FqName(it.fqName) }
      // Scope must be defined in this same module
      ?: maybeExtraReferences.value
        .flatMap { it.startingWith(kotlinFile.packageFqName) }
        .firstOrNull { maybeExtra -> maybeExtra.endsWith(scopeNameEntry.name) }
        ?.let { FqName(it) }
      // Scope must be defined in this same package
      ?: FqName("${kotlinFile.packageFqName}.${scopeNameEntry.name}")

    return AnvilScopeName(rawScopeName)
  }

  private fun McProject.dependenciesBySourceSetName(): Map<SourceSetName, List<ConfiguredProjectDependency>> {
    return configurations
      .map { (configurationName, _) ->
        configurationName.toSourceSetName() to projectDependencies[configurationName].orEmpty()
      }
      .groupBy { it.first }
      .map { it.key to it.value.flatMap { it.second } }
      .toMap()
  }

  companion object Key : ProjectContext.Key<AnvilGraph> {

    override suspend operator fun invoke(project: McProject): AnvilGraph {

      return AnvilGraph(
        project = project,
        delegate = SafeCache()
      )
    }
  }
}

suspend fun ProjectContext.anvilGraph(): AnvilGraph = get(AnvilGraph)
