/*
 * Copyright (C) 2021-2023 Rick Busarow
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
import kotlinx.coroutines.flow.toSet
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import modulecheck.dagger.SingleIn
import modulecheck.dagger.TaskScope
import modulecheck.model.dependency.ProjectPath
import modulecheck.model.dependency.withUpstream
import modulecheck.model.sourceset.SourceSetName
import modulecheck.parsing.kotlin.compiler.HasAnalysisResult
import modulecheck.project.McProject
import modulecheck.project.ProjectCache
import modulecheck.utils.coroutines.flatMapListMerge
import modulecheck.utils.coroutines.onEachAsync
import modulecheck.utils.letIf
import javax.inject.Inject
import kotlin.random.Random

/**
 * Thread-safe, "leased" access to [AnalysisResult][org.jetbrains.kotlin.analyzer.AnalysisResult]
 * creation and subsequent
 * [ModuleDescriptorImpl][org.jetbrains.kotlin.descriptors.impl.ModuleDescriptorImpl] access.
 *
 * @since 0.13.0
 */
interface SafeAnalysisResultAccess {

  /**
   * Suspends until all dependency module descriptors are available for use, then acquires locks for
   * all of them and performs [action]. No other project/source set will be able to read from those
   * analysis results, binding contexts, or module descriptors until [action] has completed.
   *
   * @since 0.13.0
   */
  suspend fun <T> withLeases(
    requester: HasAnalysisResult,
    projectPath: ProjectPath,
    sourceSetName: SourceSetName,
    action: suspend (Collection<HasAnalysisResult>) -> T
  ): T
}

/**
 * The only implementation of [SafeAnalysisResultAccess]
 *
 * @since 0.13.0
 */
@SingleIn(TaskScope::class)
@ContributesBinding(TaskScope::class)
class SafeAnalysisResultAccessImpl @Inject constructor(
  private val projectCache: ProjectCache
) : SafeAnalysisResultAccess {

  private val locksCacheLock = Mutex(locked = false)
  private val locks = mutableMapOf<HasAnalysisResult, Boolean>()

  private val queue = MutableStateFlow(DifferentList(emptyList()))

  override suspend fun <T> withLeases(
    requester: HasAnalysisResult,
    projectPath: ProjectPath,
    sourceSetName: SourceSetName,
    action: suspend (Collection<HasAnalysisResult>) -> T
  ): T {

    val thisProject = projectCache.getValue(projectPath)

    val requested = projectCache.getValue(projectPath)
      .projectDependencies[sourceSetName]
      .flatMapListMerge { dep ->

        val dependencyProject = projectCache.getValue(dep.projectPath)
        val dependencySourceSetName = dep.declaringSourceSetName(dependencyProject.sourceSets)

        dependencySourceSetName.upstreamEnvironments(dependencyProject)
      }
      .plus(sourceSetName.upstreamEnvironments(thisProject).filterNot { it == requester })
      // Before performing this analysis, each dependency needs to do its own analysis.
      // If those are already done, then there's nothing to do.  Otherwise, wait for them to
      // complete before entering this requester into the general queue.
      // We need to wait before queueing **this** requester because if this is queued first, it will
      // acquire the locks for all its dependencies, and then any dependency which hasn't actually
      // done analysis yet won't be able to acquire its lock, and we'll have a deadlock.
      .onEachAsync { dependencyEnvironment ->
        dependencyEnvironment.analysisResultDeferred
          .let { analysisResult ->
            if (!analysisResult.isCompleted) {
              analysisResult.await()
            }
          }
      }
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

        val acquired = locksCacheLock.withLock {
          // If this requester is first, remove it from the queue and try to lock all of its
          // environments. If we can't lock everything, this pending request be placed in the back
          // of the queue.
          queue.value = queue.value.filterNot { it == pendingRequest }.asDifferentList()

          maybeLockAll(pendingRequest)
        }

        if (acquired) {
          // do the work we need the locks for
          result = action()
          // release the locks and update the queue with a new sort
          locksCacheLock.withLock {

            pendingRequest.dependencies
              .plus(pendingRequest.requester)
              .forEach { locks[it] = false }

            queue.value = queue.value.distinct().sorted().asDifferentList()
          }

          // stop collecting so that we return out of the function
          completed = true
        } else {
          // If this requester is blocked by some other consumer, go to the end of the line.  Once
          // there's a change to the state of the locks, this requester will be able to check again.
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
        .distinct()
        .letIf(sort) { list ->
          list.sorted()
        }
        .asDifferentList()
    }
  }

  private fun maybeLockAll(
    pendingRequest: PendingRequest
  ): Boolean {

    val allEnvironments = pendingRequest.dependencies
      .plus(pendingRequest.requester)

    return allEnvironments
      .none { locks.getOrPut(it) { false } }
      .also { allUnlocked ->
        if (allUnlocked) {
          allEnvironments.forEach { key ->
            locks[key] = true
          }
        }
      }
  }

  internal data class PendingRequest(
    val requester: HasAnalysisResult,
    val dependencies: Set<HasAnalysisResult>
  ) : Comparable<PendingRequest> {

    override fun compareTo(other: PendingRequest): Int {

      return dependencies.contains(other.requester).compareTo(true)
        .compareTo(other.dependencies.contains(requester).compareTo(false))
    }
  }

  /**
   * This is a hack to get around the de-duping behavior of a
   * [StateFlow][kotlinx.coroutines.flow.StateFlow]. Two lists with identical contents will never be
   * equal, so
   *
   * As a bonus, this is also cheaper since we don't need to compare all the elements of the two
   * lists.
   *
   * @since 0.13.0
   */
  private class DifferentList(delegate: List<PendingRequest>) : List<PendingRequest> by delegate {
    @Suppress("EqualsAlwaysReturnsTrueOrFalse")
    override fun equals(other: Any?): Boolean = false
    override fun hashCode(): Int = Random.nextInt()
  }

  private fun List<PendingRequest>.asDifferentList() = DifferentList(this.distinct())

  private suspend fun SourceSetName.upstreamEnvironments(project: McProject) = withUpstream(project)
    .mapNotNull { project.sourceSets[it]?.kotlinEnvironmentDeferred?.await() }
}
