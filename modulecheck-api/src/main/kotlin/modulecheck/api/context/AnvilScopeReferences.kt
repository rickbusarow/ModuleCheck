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
import modulecheck.api.files.KotlinFile
import modulecheck.psi.internal.getByNameOrIndex
import org.jetbrains.kotlin.psi.annotationEntryRecursiveVisitor
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.ConcurrentMap

data class AnvilScopeReference(val value: String)

data class AnvilScopeReferences(
  internal val delegate: ConcurrentMap<ConfigurationName, Set<AnvilScopeReference>>
) : ConcurrentMap<ConfigurationName, Set<AnvilScopeReference>> by delegate,
    ProjectContext.Element {

  override val key: ProjectContext.Key<AnvilScopeReferences>
    get() = Key

  companion object Key : ProjectContext.Key<AnvilScopeReferences> {

    private val annotations = listOf(
      "com.squareup.anvil.annotations.ContributesTo",
      "com.squareup.anvil.annotations.ContributesBinding",
      "com.squareup.anvil.annotations.ContributesMultibinding"
    )

    override operator fun invoke(project: Project2): AnvilScopeReferences {

      val map = project
        .configurations
        .mapValues { (configurationName, _) ->

          val scopeArguments = mutableSetOf<AnvilScopeReference>()

          val visitor = annotationEntryRecursiveVisitor { entry ->

            val typeRef = entry.typeReference?.text ?: return@annotationEntryRecursiveVisitor

            if (annotations.any { it.endsWith(typeRef) }) {

              val t = entry.valueArgumentList?.getByNameOrIndex(0, "scope")

              if (t != null) {
                scopeArguments.add(AnvilScopeReference(t.text))
              }
            }
          }
          val projectDependencies = project
            .projectDependencies
            .value[configurationName]
            .orEmpty()

          project[JvmFiles][configurationName.toSourceSetName()]
            .orEmpty()
            .filterIsInstance<KotlinFile>()
            .filter { kotlinFile ->
              kotlinFile.imports.any { it in annotations } ||
                kotlinFile.maybeExtraReferences.any { it in annotations }
            }
            .onEach { kotlinFile ->

              kotlinFile.ktFile.accept(visitor)

              // kotlinFile
              //   .maybeExtraReferences
              //   .mapNotNull { possible ->
              //     projectDependencies
              //       .firstOrNull {
              //         it.project[Declarations][SourceSetName.MAIN].orEmpty()
              //           .any { it == possible }
              //       }
              //   }
            }
        }

      return AnvilScopeReferences(ConcurrentHashMap(map))
    }
  }
}
