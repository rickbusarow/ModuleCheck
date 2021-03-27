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

data class AnvilScopeName(val fqName: ImportName)

data class AnvilScopeContributions(
  internal val delegate: ConcurrentMap<ConfigurationName, Set<AnvilScopeName>>
) : ConcurrentMap<ConfigurationName, Set<AnvilScopeName>> by delegate,
    ProjectContext.Element {

  override val key: ProjectContext.Key<AnvilScopeContributions>
    get() = Key

  companion object Key : ProjectContext.Key<AnvilScopeContributions> {

    private val annotations = listOf(
      "com.squareup.anvil.annotations.ContributesTo",
      "com.squareup.anvil.annotations.ContributesBinding",
      "com.squareup.anvil.annotations.ContributesMultibinding"
    )

    override operator fun invoke(project: Project2): AnvilScopeContributions {
      if (project.anvilGradlePlugin == null) return AnvilScopeContributions(ConcurrentHashMap())

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

      return AnvilScopeContributions(ConcurrentHashMap(map))
    }

    private fun KotlinFile.getScopeArguments(): Set<String> {
      val scopeArguments = mutableSetOf<String>()

      val visitor = annotationEntryRecursiveVisitor { entry ->

        val typeRef = entry.typeReference?.text ?: return@annotationEntryRecursiveVisitor

        if (annotations.any { it.endsWith(typeRef) }) {
          val t = entry.valueArgumentList?.getByNameOrIndex(0, "scope")

          if (t != null) {
            scopeArguments.add(t.text.replace("::class", ""))
          }
        }
      }

      ktFile.accept(visitor)

      return scopeArguments
    }
  }
}

val ProjectContext.anvilScopeContributions: AnvilScopeContributions
  get() = get(
    AnvilScopeContributions
  )

fun ProjectContext.anvilScopeContributionsForConfigurationName(
  configurationName: ConfigurationName
): Set<AnvilScopeName> = anvilScopeContributions[configurationName].orEmpty()
