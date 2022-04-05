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

import kotlinx.coroutines.flow.toList
import modulecheck.api.context.Declarations.DeclarationsKey.ALL
import modulecheck.api.context.Declarations.DeclarationsKey.WithUpstream
import modulecheck.api.context.Declarations.DeclarationsKey.WithoutUpstream
import modulecheck.parsing.gradle.SourceSetName
import modulecheck.parsing.source.DeclaredName
import modulecheck.project.ConfiguredProjectDependency
import modulecheck.project.McProject
import modulecheck.project.ProjectContext
import modulecheck.project.isAndroid
import modulecheck.utils.LazySet
import modulecheck.utils.LazySet.DataSource.Priority.HIGH
import modulecheck.utils.LazySetComponent
import modulecheck.utils.SafeCache
import modulecheck.utils.dataSource
import modulecheck.utils.dataSourceOf
import modulecheck.utils.lazySet

data class Declarations private constructor(
  private val delegate: SafeCache<DeclarationsKey, LazySet<DeclaredName>>,
  private val project: McProject
) : ProjectContext.Element {

  override val key: ProjectContext.Key<Declarations>
    get() = Key

  // Allow for caching full hierarchies up declarations in a single `LazySet`.
  // Without this, a full-hierarchy LazySet needs to be rebuilt from cached elements for every
  // `mustBeApi` or `all` query, and that turns out ti be very expensive.
  private sealed interface DeclarationsKey {
    object ALL : DeclarationsKey
    data class WithUpstream(val sourceSetName: SourceSetName) : DeclarationsKey
    data class WithoutUpstream(val sourceSetName: SourceSetName) : DeclarationsKey
  }

  suspend fun all(): LazySet<DeclaredName> {
    return delegate.getOrPut(ALL) {
      project.sourceSets
        .keys
        .map { project.declarations().get(it, false) }
        .let { lazySet(it) }
    }
  }

  suspend fun get(
    sourceSetName: SourceSetName,
    includeUpstream: Boolean
  ): LazySet<DeclaredName> {
    val key = if (includeUpstream) {
      WithUpstream(sourceSetName)
    } else WithoutUpstream(sourceSetName)
    return delegate.getOrPut(key) {
      val components = mutableListOf<LazySetComponent<DeclaredName>>()

      val seed = if (includeUpstream) {
        sourceSetName.withUpstream(project)
          .filterNot { it == SourceSetName.TEST_FIXTURES }
      } else {
        listOf(sourceSetName)
      }

      seed.forEach { sourceSetOrUpstream ->

        val rNameOrNull = project.androidRDeclaredNamesForSourceSetName(sourceSetOrUpstream)

        project.jvmFilesForSourceSetName(sourceSetOrUpstream)
          .toList()
          .map { dataSource(HIGH) { it.declarations } }
          .let { components.addAll(it) }

        if (rNameOrNull != null) {
          check(project.isAndroid())

          val resources = project.androidResourceDeclaredNamesForSourceSetName(sourceSetOrUpstream)

          components.add(resources)

          components.add(dataSourceOf(rNameOrNull))

          components.add(
            project.androidDataBindingDeclarationsForSourceSetName(sourceSetOrUpstream)
          )
        }
      }

      lazySet(components)
    }
  }

  companion object Key : ProjectContext.Key<Declarations> {
    override suspend operator fun invoke(project: McProject): Declarations {
      return Declarations(SafeCache(), project)
    }
  }
}

suspend fun ProjectContext.declarations(): Declarations = get(Declarations)

suspend fun ConfiguredProjectDependency.declarations(): LazySet<DeclaredName> {
  if (isTestFixture) {
    return project.declarations().get(SourceSetName.TEST_FIXTURES, false)
  }

  // If the dependency is something like `testImplementation(...)`, then the dependent project is
  // getting the `main` source from the dependency.  If it's something like
  // `debugImplementation(...)`, though, then the dependency is providing its `debug` source, which
  // in turn provides its upstream `main` source.
  val nonTestSourceSetName = configurationName.toSourceSetName()
    .nonTestSourceSetNameOrNull()
    ?: declaringSourceSetName()

  // If we got something like `debug` as a source set, that just means that the dependent project
  // has a `debug` source set.  If the dependency project has `debug`, that's what it'll provide.
  // If it doesn't have `debug`, it'll just provide `main`.
  val declarationsSourceSetName = nonTestSourceSetName
    .takeIf { project.sourceSets.containsKey(nonTestSourceSetName) }
    ?: SourceSetName.MAIN

  return project.declarations().get(declarationsSourceSetName, true)
}
