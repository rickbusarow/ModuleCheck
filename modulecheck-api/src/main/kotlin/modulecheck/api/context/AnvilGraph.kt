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

package modulecheck.api.context

import modulecheck.api.util.flatMapBlocking
import modulecheck.parsing.AnvilScopeName
import modulecheck.parsing.AnvilScopeNameEntry
import modulecheck.parsing.ConfiguredProjectDependency
import modulecheck.parsing.DeclarationName
import modulecheck.parsing.McProject
import modulecheck.parsing.ProjectContext
import modulecheck.parsing.RawAnvilAnnotatedType
import modulecheck.parsing.SourceSetName
import modulecheck.parsing.asDeclarationName
import modulecheck.parsing.psi.KotlinFile
import modulecheck.parsing.psi.asDeclarationName
import modulecheck.parsing.psi.internal.getByNameOrIndex
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.psi.KtAnnotated
import org.jetbrains.kotlin.psi.KtAnnotationEntry
import org.jetbrains.kotlin.psi.classOrObjectRecursiveVisitor
import org.jetbrains.kotlin.utils.addToStdlib.safeAs

data class AnvilGraph(
  val project: McProject,
  val scopeContributions: Map<SourceSetName, Map<AnvilScopeName, Set<DeclarationName>>>,
  val scopeMerges: Map<SourceSetName, Map<AnvilScopeName, Set<DeclarationName>>>
) : ProjectContext.Element {
  override val key: ProjectContext.Key<AnvilGraph>
    get() = Key

  override fun toString(): String {
    return """ProjectAnvilGraph(
      | project=$project,
      | scopeContributions=$scopeContributions,
      | scopeMerges=$scopeMerges)""".trimMargin()
  }

  companion object Key : ProjectContext.Key<AnvilGraph> {

    override suspend operator fun invoke(project: McProject): AnvilGraph {
      if (project.anvilGradlePlugin == null) return AnvilGraph(
        project = project,
        scopeContributions = emptyMap(),
        scopeMerges = emptyMap()
      )

      val mergeAnnotations = mergeAnnotations()
      val allAnnotations = mergeAnnotations + contributeAnnotations()

      val mergedMap = mutableMapOf<SourceSetName, Map<AnvilScopeName, Set<DeclarationName>>>()
      val contributedMap = mutableMapOf<SourceSetName, Map<AnvilScopeName, Set<DeclarationName>>>()

      project.sourceSets
        .keys
        .forEach { sourceSetName ->

          val (merged, contributed) = project.declarationsForScopeName(
            allAnnotations = allAnnotations,
            mergeAnnotations = mergeAnnotations
          )

          mergedMap[sourceSetName] = merged
          contributedMap[sourceSetName] = contributed
        }

      return AnvilGraph(
        project = project,
        scopeContributions = contributedMap,
        scopeMerges = mergedMap
      )
    }

    private fun contributeAnnotations(): Set<String> = setOf(
      "com.squareup.anvil.annotations.ContributesTo",
      "com.squareup.anvil.annotations.ContributesBinding",
      "com.squareup.anvil.annotations.ContributesMultibinding"
    )

    private fun mergeAnnotations(): Set<String> = setOf(
      "com.squareup.anvil.annotations.MergeComponent",
      "com.squareup.anvil.annotations.MergeSubcomponent"
    )

    private suspend fun McProject.declarationsForScopeName(
      allAnnotations: Set<String>,
      mergeAnnotations: Set<String>
    ): Pair<Map<AnvilScopeName, Set<DeclarationName>>, Map<AnvilScopeName, Set<DeclarationName>>> {
      val mergedMap = mutableMapOf<AnvilScopeName, MutableSet<DeclarationName>>()
      val contributedMap = mutableMapOf<AnvilScopeName, MutableSet<DeclarationName>>()

      sourceSets
        .keys
        .forEach { sourceSetName ->

          jvmFilesForSourceSetName(sourceSetName)
            .asSequence()
            // Anvil only works with Kotlin, so no point in trying to parse Java files
            .filterIsInstance<KotlinFile>()
            // only re-visit files which have Anvil annotations
            .filter { kotlinFile ->
              kotlinFile.imports.any { it in allAnnotations } ||
                kotlinFile.maybeExtraReferences.any { it in allAnnotations }
            }
            .forEach { kotlinFile ->

              val (merged, contributed) = kotlinFile
                .getScopeArguments(allAnnotations, mergeAnnotations)

              merged
                .forEach { rawAnvilAnnotatedType ->

                  val scopeName = getAnvilScopeName(
                    scopeNameEntry = rawAnvilAnnotatedType.anvilScopeNameEntry,
                    sourceSetName = sourceSetName,
                    kotlinFile = kotlinFile
                  )

                  val declarationNames = mergedMap.getOrPut(scopeName) { mutableSetOf() }

                  declarationNames.add(rawAnvilAnnotatedType.declarationName)

                  mergedMap[scopeName] = declarationNames
                }
              contributed
                .forEach { rawAnvilAnnotatedType ->

                  val scopeName = getAnvilScopeName(
                    scopeNameEntry = rawAnvilAnnotatedType.anvilScopeNameEntry,
                    sourceSetName = sourceSetName,
                    kotlinFile = kotlinFile
                  )

                  val declarationNames = contributedMap.getOrPut(scopeName) { mutableSetOf() }

                  declarationNames.add(rawAnvilAnnotatedType.declarationName)

                  contributedMap[scopeName] = declarationNames
                }
            }
        }

      return mergedMap to contributedMap
    }

    private fun KotlinFile.getScopeArguments(
      allAnnotations: Set<String>,
      mergeAnnotations: Set<String>
    ): ScopeArgumentParseResult {
      val mergeArguments = mutableSetOf<RawAnvilAnnotatedType>()
      val contributeArguments = mutableSetOf<RawAnvilAnnotatedType>()

      val visitor = classOrObjectRecursiveVisitor { classOrObject ->

        val typeFqName = classOrObject.fqName ?: return@classOrObjectRecursiveVisitor
        val annotated = classOrObject.safeAs<KtAnnotated>() ?: return@classOrObjectRecursiveVisitor

        annotated
          .annotationEntries
          .filter { annotationEntry ->
            val typeRef = annotationEntry.typeReference?.text ?: return@filter false

            allAnnotations.any { it.endsWith(typeRef) }
          }
          .forEach { annotationEntry ->
            val typeRef = annotationEntry.typeReference!!.text

            val raw = annotationEntry.toRawAnvilAnnotatedType(typeFqName) ?: return@forEach

            if (mergeAnnotations.any { it.endsWith(typeRef) }) {
              mergeArguments.add(raw)
            } else {
              contributeArguments.add(raw)
            }
          }
      }

      ktFile.accept(visitor)

      return ScopeArgumentParseResult(
        mergeArguments = mergeArguments,
        contributeArguments = contributeArguments
      )
    }

    internal data class ScopeArgumentParseResult(
      val mergeArguments: Set<RawAnvilAnnotatedType>,
      val contributeArguments: Set<RawAnvilAnnotatedType>
    )

    fun KtAnnotationEntry.toRawAnvilAnnotatedType(typeFqName: FqName): RawAnvilAnnotatedType? {
      val valueArgument = valueArgumentList
        ?.getByNameOrIndex(0, "scope")
        ?: return null

      val entryText = valueArgument
        .text
        .replace(".+[=]+".toRegex(), "") // remove named arguments
        .replace("::class", "")
        .trim()

      return RawAnvilAnnotatedType(
        declarationName = typeFqName.asDeclarationName(),
        anvilScopeNameEntry = AnvilScopeNameEntry(entryText)
      )
    }

    private suspend fun McProject.getAnvilScopeName(
      scopeNameEntry: AnvilScopeNameEntry,
      sourceSetName: SourceSetName,
      kotlinFile: KotlinFile
    ): AnvilScopeName {
      val dependenciesBySourceSetName = dependenciesBySourceSetName()

      // if scope is directly imported (most likely),
      // then use that fully qualified import
      val rawScopeName = kotlinFile.imports.firstOrNull { import ->
        import.endsWith(scopeNameEntry.name)
      }
        ?.asDeclarationName()
        // if the scope is wildcard-imported
        ?: dependenciesBySourceSetName[sourceSetName]
          .orEmpty()
          .asSequence()
          .flatMapBlocking { cpd ->
            cpd.project
              .declarations()[SourceSetName.MAIN]
              .orEmpty()
          }
          .filter { dn ->
            dn.fqName in kotlinFile.maybeExtraReferences
          }
          .firstOrNull { dn ->
            dn.fqName.endsWith(scopeNameEntry.name)
          } // Scope must be defined in this same module
        ?: kotlinFile
          .maybeExtraReferences
          .firstOrNull { maybeExtra ->
            maybeExtra.startsWith(kotlinFile.packageFqName) &&
              maybeExtra.endsWith(scopeNameEntry.name)
          }
          ?.asDeclarationName()
        // Scope must be defined in this same package
        ?: "${kotlinFile.packageFqName}.${scopeNameEntry.name}".asDeclarationName()

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
  }
}

suspend fun ProjectContext.anvilGraph(): AnvilGraph = get(AnvilGraph)
