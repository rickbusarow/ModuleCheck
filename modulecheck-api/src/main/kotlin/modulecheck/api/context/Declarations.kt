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
import modulecheck.api.context.Declarations.DeclarationsKey.Parameterized
import modulecheck.model.dependency.ProjectDependency
import modulecheck.parsing.gradle.model.SourceSetName
import modulecheck.parsing.source.DeclaredName
import modulecheck.parsing.source.PackageName
import modulecheck.project.McProject
import modulecheck.project.ProjectCache
import modulecheck.project.ProjectContext
import modulecheck.project.isAndroid
import modulecheck.utils.cache.SafeCache
import modulecheck.utils.lazy.LazySet
import modulecheck.utils.lazy.LazySet.DataSource
import modulecheck.utils.lazy.LazySetComponent
import modulecheck.utils.lazy.asDataSource
import modulecheck.utils.lazy.dataSource
import modulecheck.utils.lazy.dataSourceOf
import modulecheck.utils.lazy.lazySet
import modulecheck.utils.letIf

class Declarations private constructor(
  private val delegate: SafeCache<DeclarationsKey, LazySet<DeclaredName>>,
  private val project: McProject
) : ProjectContext.Element {

  override val key: ProjectContext.Key<Declarations>
    get() = Key

  // Allow for caching full hierarchies of declarations in a single `LazySet`.
  // Without this, a full-hierarchy LazySet needs to be rebuilt from cached elements for every
  // `mustBeApi` or `all` query, and that turns out to be very expensive.
  private sealed interface DeclarationsKey {
    object ALL : DeclarationsKey
    data class Parameterized(
      val sourceSetName: SourceSetName,
      val includeUpstream: Boolean,
      val packageNameOrNull: PackageName?
    ) : DeclarationsKey
  }

  suspend fun all(): LazySet<DeclaredName> {
    return delegate.getOrPut(ALL) {
      project.sourceSets
        .keys
        .map { project.declarations().get(it, includeUpstream = false, packageNameOrNull = null) }
        .let { lazySet(it) }
    }
  }

  suspend fun get(
    sourceSetName: SourceSetName,
    includeUpstream: Boolean,
    packageNameOrNull: PackageName? = null
  ): LazySet<DeclaredName> {
    val key = Parameterized(sourceSetName, includeUpstream, packageNameOrNull)
    return delegate.getOrPut(key) {
      val components = mutableListOf<LazySetComponent<DeclaredName>>()

      val sourceSetSeed = if (includeUpstream) {
        sourceSetName.withUpstream(project)
          .filterNot { it == SourceSetName.TEST_FIXTURES }
      } else {
        listOf(sourceSetName)
      }

      sourceSetSeed.forEach { sourceSetOrUpstream ->

        val rNameOrNull = project.androidRDeclaredNameForSourceSetName(sourceSetOrUpstream)

        project.jvmFilesForSourceSetName(sourceSetOrUpstream)
          .toList()
          .letIf(packageNameOrNull != null) { files ->
            files.filter { it.packageName == packageNameOrNull }
          }
          .map { dataSource(DataSource.Priority.HIGH) { it.declarations } }
          .let { components.addAll(it) }

        if (rNameOrNull != null) {
          check(project.isAndroid())

          val resources = project.androidResourceDeclaredNamesForSourceSetName(sourceSetOrUpstream)
            .asDataSource()

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
      return Declarations(
        SafeCache(listOf(project.path, Declarations::class)), project
      )
    }
  }
}

suspend fun ProjectContext.declarations(): Declarations = get(Declarations)

suspend fun ProjectDependency.declarations(
  projectCache: ProjectCache,
  packageNameOrNull: PackageName? = null
): LazySet<DeclaredName> {
  val project = projectCache.getValue(path)
  if (isTestFixture) {
    return project.declarations()
      .get(
        sourceSetName = SourceSetName.TEST_FIXTURES,
        includeUpstream = false,
        packageNameOrNull = packageNameOrNull
      )
  }

  // If the dependency config is `testImplementation(...)` or `androidTestImplementation(...)`:
  //   If the dependency is an Android module, then it automatically targets the `debug` source.
  //   If the dependency is a Kotlin/Java module, then it automatically targets 'main'.
  //
  // If the dependency is something like `debugImplementation(...)`, the dependency is providing its
  // `debug` source, which in turn provides its upstream `main` source.
  val nonTestSourceSetName = configurationName.toSourceSetName()
    .nonTestSourceSetNameOrNull()
    ?: declaringSourceSetName(isAndroid = project.isAndroid())

  // If we got something like `debug` as a source set, that just means that the dependent project
  // has a `debug` source set.  If the dependency project has `debug`, that's what it'll provide.
  // If it doesn't have `debug`, it'll just provide `main`.
  val declarationsSourceSetName = nonTestSourceSetName
    .takeIf { project.sourceSets.containsKey(nonTestSourceSetName) }
    ?: SourceSetName.MAIN

  return project.declarations()
    .get(
      sourceSetName = declarationsSourceSetName,
      includeUpstream = true,
      packageNameOrNull = packageNameOrNull
    )
}
