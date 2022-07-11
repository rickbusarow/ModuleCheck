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
import modulecheck.utils.letIf
import javax.inject.Inject

interface SafeAnalysisResultAccess {

  suspend fun <T> withLeases(
    requester: HasPsiAnalysis,
    projectPath: ProjectPath,
    sourceSetName: SourceSetName,
    action: suspend (Collection<HasPsiAnalysis>) -> T
  ): T
}

@SingleIn(AppScope::class)
@ContributesBinding(AppScope::class)
class SafeAnalysisResultAccessImpl @Inject constructor(
  private val projectCache: ProjectCache
) : SafeAnalysisResultAccess {
  private val cache = mutableMapOf<HasPsiAnalysis, Boolean>()

  private val queue = MutableStateFlow<List<Pair<HasPsiAnalysis, Int>>>(emptyList())

  private val cacheLock = Mutex(locked = false)

  override suspend fun <T> withLeases(
    requester: HasPsiAnalysis,
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
      .plus(requester)

    addToQueue(
      requester = requester,
      requested = requested,
      sort = true
    )

    return withLeases(requester, requested) { action(requested) }
  }

  private suspend fun addToQueue(
    requester: HasPsiAnalysis,
    requested: List<HasPsiAnalysis>,
    sort: Boolean
  ) {
    cacheLock.withLock {
      queue.value = queue.value
        .plus(requester to requested.size)
        .letIf(sort) { list ->
          list.sortedBy { pair -> pair.second }
        }
    }
  }

  private suspend fun <T> withLeases(
    requester: HasPsiAnalysis,
    requested: List<HasPsiAnalysis>,
    action: suspend () -> T
  ): T {
    var completed = false
    var result: T? = null

    queue
      .takeWhile { !completed }
      .collect { allPairs ->

        val (head, _) = allPairs.first()
        // only do work when this requester is first in line.
        if (head != requester) return@collect

        val acquired = cacheLock.withLock {
          // If this requester is first, remove it from the queue and try to lock all of its
          // environments. If it can't lock everything, it'll be placed in the back of the queue.
          queue.value = queue.value.filterNot { it.first == requester }

          maybeLockAll(requested)
        }

        if (acquired) {
          // do the work we need the locks for
          result = action()
          // release the locks and update the queue with a new sort
          cacheLock.withLock {
            requested.forEach { cache[it] = false }
            queue.value = queue.value.sortedBy { it.second }
          }
          // stop collecting so that we return out of the function
          completed = true
        } else {
          // if this requester is blocked by some other consumer, go to the end of the line
          // once there's a change to
          addToQueue(
            requester = requester,
            requested = requested,
            sort = false
          )
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
