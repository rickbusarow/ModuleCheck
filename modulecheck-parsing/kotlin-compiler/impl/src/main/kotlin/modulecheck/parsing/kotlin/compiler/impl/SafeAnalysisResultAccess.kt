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
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.takeWhile
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import modulecheck.dagger.AppScope
import modulecheck.dagger.SingleIn
import modulecheck.parsing.gradle.model.ProjectPath
import modulecheck.parsing.gradle.model.SourceSetName
import modulecheck.parsing.kotlin.compiler.HasPsiAnalysis
import modulecheck.project.McProject
import modulecheck.project.ProjectCache
import modulecheck.project.isAndroid
import modulecheck.utils.coroutines.flatMapListMerge
import modulecheck.utils.letIf
import java.util.Random
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

  private val locksCacheLock = Mutex(locked = false)
  private val locksCache = mutableMapOf<HasPsiAnalysis, Boolean>()

  private val queue = MutableStateFlow<List<PendingRequest>>(emptyList())

  override suspend fun <T> withLeases(
    requester: HasPsiAnalysis,
    projectPath: ProjectPath,
    sourceSetName: SourceSetName,
    action: suspend (Collection<HasPsiAnalysis>) -> T
  ): T {

    val thisProject = projectCache.getValue(projectPath)

    val requested = projectCache.getValue(projectPath)
      .projectDependencies[sourceSetName]
      .flatMapListMerge { dep ->

        val dependencyProject = projectCache.getValue(dep.path)
        val dependencySourceSetName = dep.declaringSourceSetName(dependencyProject.isAndroid())

        dependencySourceSetName.upstreamEnvironments(dependencyProject)
      }
      .plus(sourceSetName.upstreamEnvironments(thisProject).filterNot { it == requester })
      .toSet()

    val pendingRequest = PendingRequest(
      requester = requester,
      dependencies = requested
    )

    addToQueue(
      pendingRequest = pendingRequest,
      sort = true
    )

    return withLeases(pendingRequest) { action(requested) }
  }

  private suspend fun <T> withLeases(
    pendingRequest: PendingRequest,
    action: suspend () -> T
  ): T {
    var completed = false
    var result: T? = null

    queue
      // only do work when this requester is first in line.
      .filter { it.firstOrNull() == pendingRequest }
      .onEach {

        // log("ping pending - ${allPending.map { it.name }}")

        val acquired = locksCacheLock.withLock {
          // If this requester is first, remove it from the queue and try to lock all of its
          // environments. If we can't lock everything, this pending request be placed in the back
          // of the queue.
          queue.value = queue.value.filterNot { it == pendingRequest }
            .asDifferentList()

          maybeLockAll(pendingRequest)
        }

        if (acquired) {
          // do the work we need the locks for
          result = action()
          // release the locks and update the queue with a new sort
          locksCacheLock.withLock {

            pendingRequest.dependencies
              .plus(pendingRequest.requester)
              .forEach { locksCache[it] = false }

            queue.value = queue.value.sorted().asDifferentList()
          }

          // stop collecting so that we return out of the function
          completed = true
        } else {
          // If this requester is blocked by some other consumer, go to the end of the line once
          // there's a change to.
          addToQueue(
            pendingRequest = pendingRequest,
            sort = false
          )
        }
      }
      .takeWhile { !completed }
      .collect()

    @Suppress("UNCHECKED_CAST")
    return result as T
  }

  private suspend fun addToQueue(
    pendingRequest: PendingRequest,
    sort: Boolean
  ) {
    locksCacheLock.withLock {
      queue.value = queue.value
        .plus(pendingRequest)
        .letIf(sort) { list ->
          list.sorted()
        }
        .asDifferentList()
    }
  }

  private fun maybeLockAll(
    pendingRequest: PendingRequest
  ): Boolean {

    val all = pendingRequest.dependencies
      .plus(pendingRequest.requester)

    return pendingRequest.dependencies
      .map { dependency ->
        locksCache.getOrPut(dependency) { true }
      }
      .all { locked -> !locked }
      .also { allUnlocked ->
        if (allUnlocked) {

          all.forEach { key ->
            locksCache[key] = true
          }
        }
      }
  }

  internal data class PendingRequest(
    val requester: HasPsiAnalysis,
    val dependencies: Set<HasPsiAnalysis>
  ) : Comparable<PendingRequest> {

    /**
     * Compares this object with the specified object for order. Returns zero if this object is
     * equal to the specified other object, a negative number if it's less than other, or a positive
     * number if it's greater than other.
     */
    override fun compareTo(other: PendingRequest): Int {
      // return dependencies.size.compareTo(other.dependencies.size)

      return other.dependencies.contains(requester).compareTo(true)
        .compareTo(dependencies.contains(other.requester).compareTo(true))

      // return when {
      //   dependencies.contains(other.requester) -> 1
      //   other.dependencies.contains(requester) -> -1
      //   else -> 0
      //   else -> dependencies.size.compareTo(other.dependencies.size)
      // }
    }
  }

  private class DifferentList<E>(delegate: List<E>) : List<E> by delegate {
    override fun equals(other: Any?): Boolean = false
    override fun hashCode(): Int = Random().nextInt()
  }

  private fun <E> List<E>.asDifferentList() = DifferentList(this)

  private suspend fun SourceSetName.upstreamEnvironments(project: McProject) = withUpstream(project)
    .mapNotNull { project.sourceSets[it]?.kotlinEnvironmentDeferred?.await() }
}
