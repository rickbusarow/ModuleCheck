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

import modulecheck.api.ConfigurationName
import modulecheck.api.Project2
import modulecheck.api.SourceSetName
import modulecheck.api.files.KotlinFile
import modulecheck.psi.internal.getByNameOrIndex
import org.jetbrains.kotlin.psi.annotationEntryRecursiveVisitor
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.ConcurrentMap

data class AnvilScopeMerges(
  internal val delegate: ConcurrentMap<ConfigurationName, Set<AnvilScopeName>>
) : ConcurrentMap<ConfigurationName, Set<AnvilScopeName>> by delegate,
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

      val map = project
        .configurations
        .mapValues { (configurationName, _) ->

          val projectDependencies = project
            .projectDependencies
            .value[configurationName]
            .orEmpty()

          project
            .jvmFilesForSourceSetName(configurationName.toSourceSetName())
            // Anvil only works with Kotlin, so no point in trying to parse Java files
            .filterIsInstance<KotlinFile>()
            // only re-visit files which have Anvil annotations
            .filter { kotlinFile ->
              kotlinFile.imports.any { it in annotations } ||
                kotlinFile.maybeExtraReferences.any { it in annotations }
            }
            .flatMap { kotlinFile ->

              kotlinFile
                .getScopeArguments()
                .map { scopeName ->

                  // if scope is directly imported (most likely),
                  // then use that fully qualified import
                  kotlinFile.imports.firstOrNull { import ->
                    import.endsWith(scopeName)
                  } // if the scope is wildcard-imported
                    ?: projectDependencies
                      .asSequence()
                      .flatMap { cpd ->
                        cpd.project
                          .declarations[SourceSetName.MAIN]
                          .orEmpty()
                      }
                      .filter { declarationName ->
                        declarationName in kotlinFile.maybeExtraReferences
                      }
                      .firstOrNull { declarationName ->
                        declarationName.endsWith(scopeName)
                      } // Scope must be defined in this same module
                    ?: kotlinFile
                      .maybeExtraReferences
                      .firstOrNull { maybeExtra ->
                        maybeExtra.startsWith(kotlinFile.packageFqName) && maybeExtra.endsWith(
                          scopeName
                        )
                      } // Scope must be defined in this same package
                    ?: kotlinFile.packageFqName + "." + scopeName
                }
                .map { AnvilScopeName(it) }
            }
            .toSet()
        }

      return AnvilScopeMerges(ConcurrentHashMap(map))
    }

    private fun KotlinFile.getScopeArguments(): Set<String> {
      val scopeArguments = mutableSetOf<String>()

      val visitor = annotationEntryRecursiveVisitor { entry ->

        val typeRef = entry.typeReference?.text ?: return@annotationEntryRecursiveVisitor

        if (annotations.any { it.endsWith(typeRef) }) {
          val entryText = entry
            .valueArgumentList
            ?.getByNameOrIndex(0, "scope")
            ?.text
            ?.replace(".+[=]+".toRegex(), "") // remove named arguments
            ?.replace("::class", "")
            ?.trim()

          if (entryText != null) {
            scopeArguments.add(entryText)
          }
        }
      }

      ktFile.accept(visitor)

      return scopeArguments
    }
  }
}

val ProjectContext.anvilScopeMerges: AnvilScopeMerges
  get() = get(
    AnvilScopeMerges
  )

fun ProjectContext.anvilScopeMergesForConfigurationName(
  configurationName: ConfigurationName
): Set<AnvilScopeName> = anvilScopeMerges[configurationName].orEmpty()
