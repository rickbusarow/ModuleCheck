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

package modulecheck.parsing.kotlin.compiler.impl

import com.squareup.anvil.annotations.ContributesBinding
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.takeWhile
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import modulecheck.dagger.AppScope
import modulecheck.dagger.SingleIn
import modulecheck.parsing.gradle.model.ProjectPath
import modulecheck.parsing.gradle.model.SourceSetName
import modulecheck.parsing.kotlin.compiler.HasPsiAnalysis
import modulecheck.project.ProjectCache
import modulecheck.project.isAndroid
import modulecheck.utils.coroutines.mapAsyncNotNull
import javax.inject.Inject

@SingleIn(AppScope::class)
@ContributesBinding(AppScope::class)
class SafeAnalysisResultAccessImpl @Inject constructor(
  private val projectCache: ProjectCache
) : SafeAnalysisResultAccess {
  private val cache = mutableMapOf<HasPsiAnalysis, Boolean>()

  private val queue = MutableStateFlow<List<Pair<HasPsiAnalysis, Int>>>(emptyList())

  private val actor = MutableSharedFlow<Unit>(
    replay = 1,
    extraBufferCapacity = Int.MAX_VALUE
  )

  private val cacheLock = Mutex(locked = false)

  override suspend fun <T> withLeases(
    caller: HasPsiAnalysis,
    projectPath: ProjectPath,
    sourceSetName: SourceSetName,
    action: suspend (Collection<HasPsiAnalysis>) -> T
  ): T {

    val requested = projectCache.getValue(projectPath)
      .projectDependencies[sourceSetName]
      .mapAsyncNotNull { dep ->

        val dependencyProject = projectCache.getValue(dep.path)
        val dependencySourceSetName = dep.declaringSourceSetName(dependencyProject.isAndroid())

        dependencySourceSetName.withUpstream(dependencyProject)
          .firstNotNullOfOrNull { dependencyProject.sourceSets[it] }
          ?.kotlinEnvironmentDeferred?.await()
      }
      .toList()
      .plus(caller)

    cacheLock.withLock {

      val existing = queue.value.toMutableList()
      existing.add(caller to requested.size)

      val sorted = existing.sortedBy { it.second }

      queue.emit(sorted)
    }
    return withLeases(caller, requested) { action() }
  }

  private suspend fun <T> withLeases(
    caller: HasPsiAnalysis,
    requested: List<HasPsiAnalysis>,
    action: suspend () -> T
  ): T {

    var completed = false
    var result: T? = null

    queue
      .takeWhile { !completed }
      .collect { allPairs ->

        val acquired = cacheLock.withLock {

          val (head, _) = allPairs.first()

          head == caller && maybeLockAll(requested)
        }

        if (acquired) {
          result = action()
          cacheLock.withLock {
            requested.forEach { cache[it] = false }
          }
          completed = true

          val removed =
            actor.emit(Unit)
        }
      }

    @Suppress("UNCHECKED_CAST")
    return result as T
  }

  private fun maybeLockAll(
    requested: Collection<HasPsiAnalysis>
  ): Boolean = requested
    .map { cache.getOrPut(it) { false } }
    .none { locked -> locked }
    .also { allUnlocked ->
      if (allUnlocked) {
        requested.forEach { key ->
          cache[key] = true
        }
      }
    }
}
