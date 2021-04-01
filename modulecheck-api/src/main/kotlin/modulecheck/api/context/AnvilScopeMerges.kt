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

import modulecheck.api.ConfiguredProjectDependency
import modulecheck.api.Project2
import modulecheck.api.SourceSetName
import modulecheck.api.anvil.AnvilScopeName
import modulecheck.api.anvil.AnvilScopeNameEntry
import modulecheck.api.anvil.RawAnvilAnnotatedType
import modulecheck.api.files.KotlinFile
import modulecheck.psi.internal.getByNameOrIndex
import org.jetbrains.kotlin.psi.KtAnnotated
import org.jetbrains.kotlin.psi.classOrObjectRecursiveVisitor
import org.jetbrains.kotlin.utils.addToStdlib.safeAs
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.ConcurrentMap

data class AnvilScopeMerges(
  internal val delegate: ConcurrentHashMap<SourceSetName, Map<AnvilScopeName, Set<DeclarationName>>>
) : ConcurrentMap<SourceSetName, Map<AnvilScopeName, Set<DeclarationName>>> by delegate,
    ProjectContext.Element {

  override val key: ProjectContext.Key<AnvilScopeMerges>
    get() = Key

  companion object Key : ProjectContext.Key<AnvilScopeMerges> {

    private val annotations = listOf(
      "com.squareup.anvil.annotations.MergeComponent",
      "com.squareup.anvil.annotations.MergeSubcomponent"
    )

    override operator fun invoke(project: Project2): AnvilScopeMerges {
      if (project.anvilGradlePlugin == null) return AnvilScopeMerges(ConcurrentHashMap())

      val map = project.parseAnvilScopes(annotations)

      return AnvilScopeMerges(ConcurrentHashMap(map))
    }
  }
}

val ProjectContext.anvilScopeMerges: AnvilScopeMerges
  get() = get(AnvilScopeMerges)

fun ProjectContext.anvilScopeMergesForSourceSetName(
  sourceSetName: SourceSetName
): Map<AnvilScopeName, Set<DeclarationName>> = anvilScopeMerges[sourceSetName].orEmpty()

internal fun KotlinFile.getScopeArguments(annotations: List<String>): Set<RawAnvilAnnotatedType> {
  val scopeArguments = mutableSetOf<RawAnvilAnnotatedType>()

  val visitor = classOrObjectRecursiveVisitor { classOrObject ->

    val typeFqName = classOrObject.fqName ?: return@classOrObjectRecursiveVisitor
    val annotated = classOrObject.safeAs<KtAnnotated>() ?: return@classOrObjectRecursiveVisitor

    annotated
      .annotationEntries
      .filter { annotationEntry ->
        val typeRef = annotationEntry.typeReference?.text ?: return@filter false

        annotations.any { it.endsWith(typeRef) }
      }
      .onEach { annotationEntry ->

        val entryText = annotationEntry
          .valueArgumentList
          ?.getByNameOrIndex(0, "scope")
          ?.text
          ?.replace(".+[=]+".toRegex(), "") // remove named arguments
          ?.replace("::class", "")
          ?.trim()

        if (entryText != null) {
          scopeArguments.add(
            RawAnvilAnnotatedType(
              declarationName = typeFqName.asString(),
              anvilScopeNameEntry = AnvilScopeNameEntry(entryText)
            )
          )
        }
      }
  }

  ktFile.accept(visitor)

  return scopeArguments
}

internal fun Project2.parseAnvilScopes(
  annotations: List<String>
): Map<SourceSetName, Map<AnvilScopeName, Set<DeclarationName>>> {
  return configurations
    .map { (configurationName, _) ->
      configurationName.toSourceSetName() to declarationsForScopeName(annotations)
    }
    .toMap()
}

internal fun Project2.dependenciesBySourceSetName(): Map<SourceSetName, List<ConfiguredProjectDependency>> {
  return configurations
    .map { (configurationName, _) ->
      configurationName.toSourceSetName() to projectDependencies.value[configurationName].orEmpty()
    }
    .groupBy { it.first }
    .map { it.key to it.value.flatMap { it.second } }
    .toMap()
}

internal fun Project2.declarationsForScopeName(
  annotations: List<String>
): Map<AnvilScopeName, Set<DeclarationName>> {
  val map = mutableMapOf<AnvilScopeName, MutableSet<DeclarationName>>()

  sourceSets
    .keys
    .forEach { sourceSetName ->

      jvmFilesForSourceSetName(sourceSetName)
        // Anvil only works with Kotlin, so no point in trying to parse Java files
        .filterIsInstance<KotlinFile>()
        // only re-visit files which have Anvil annotations
        .filter { kotlinFile ->
          kotlinFile.imports.any { it in annotations } ||
            kotlinFile.maybeExtraReferences.any { it in annotations }
        }
        .forEach { kotlinFile ->

          kotlinFile
            .getScopeArguments(annotations)
            .forEach { rawAnvilAnnotatedType ->
              rawAnvilAnnotatedType.anvilScopeNameEntry

              val scopeName = getAnvilScopeName(
                scopeNameEntry = rawAnvilAnnotatedType.anvilScopeNameEntry,
                sourceSetName = sourceSetName,
                kotlinFile = kotlinFile
              )

              val declarationNames = map.getOrPut(scopeName) { mutableSetOf() }

              declarationNames.add(rawAnvilAnnotatedType.declarationName)

              map[scopeName] = declarationNames
            }
        }
    }

  return map
}

internal fun Project2.getAnvilScopeName(
  scopeNameEntry: AnvilScopeNameEntry,
  sourceSetName: SourceSetName,
  kotlinFile: KotlinFile
): AnvilScopeName {
  val dependenciesBySourceSetName = dependenciesBySourceSetName()

  // if scope is directly imported (most likely),
  // then use that fully qualified import
  val rawScopeName = kotlinFile.imports.firstOrNull { import ->
    import.endsWith(scopeNameEntry.name)
  } // if the scope is wildcard-imported
    ?: dependenciesBySourceSetName[sourceSetName]
      .orEmpty()
      .asSequence()
      .flatMap { cpd ->
        cpd.project
          .declarations[SourceSetName.MAIN]
          .orEmpty()
      }
      .filter { dn ->
        dn in kotlinFile.maybeExtraReferences
      }
      .firstOrNull { dn ->
        dn.endsWith(scopeNameEntry.name)
      } // Scope must be defined in this same module
    ?: kotlinFile
      .maybeExtraReferences
      .firstOrNull { maybeExtra ->
        maybeExtra.startsWith(kotlinFile.packageFqName) &&
          maybeExtra.endsWith(scopeNameEntry.name)
      } // Scope must be defined in this same package
    ?: kotlinFile.packageFqName + "." + scopeNameEntry.name

  return AnvilScopeName(rawScopeName)
}
