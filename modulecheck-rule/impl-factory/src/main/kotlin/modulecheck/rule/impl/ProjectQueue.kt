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

package modulecheck.rule.impl

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.channelFlow
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.sync.withPermit
import modulecheck.api.context.depths
import modulecheck.model.dependency.ProjectPath.StringProjectPath
import modulecheck.project.McProject
import modulecheck.utils.coroutines.mapAsync
import modulecheck.utils.flatMapToSet
import modulecheck.utils.mapToSet
import modulecheck.utils.trace.HasTraceTags
import modulecheck.utils.trace.traced

/**
 * Processes projects in a set order, and automatically clears the cache of each
 *
 * @since 0.12.0
 */
class ProjectQueue(
  private val projects: List<McProject>
) : HasTraceTags {

  override val tags: Iterable<Any>
    get() = listOf(ProjectQueue::class)

  /**
   * Processes projects in a set order, and automatically clears the cache of each
   *
   * @since 0.12.0
   */
  fun <T> process(transform: suspend (project: McProject) -> T): Flow<T> {

    return channelFlow {

      // A full set of all dependencies per project, regardless of their source set or classpath
      val projectsToDependencyPaths = projects.associateWith { project ->
        project.allDependencies().mapToSet { it.path }
      }
        .toMutableMap()

      // Remaining projects with all their dependencies. The entry for a project is removed after
      // it's processed.
      val upstreamPending = projectsToDependencyPaths.toMutableMap()

      /**
       * Are there any projects still being processed which depend upon the receiver project?
       *
       * @since 0.12.0
       */
      fun McProject.hasRemainingDownstream() = upstreamPending.entries
        .none { it.value.contains(path) }

      // safe access to the shared state of the queue
      val lock = Mutex(false)

      // Limit concurrency to half the available processors when emitting the projects to the
      // consumer. This is meant to ensure that high-priority/heavyweight projects get processed
      // quickly, so that their contexts can be cleared quickly. This has to happen because the
      // collection on the consumer's side isn't "fair", and without a low concurrency,
      // some of the early projects effectively get trampled.
      //
      // This doesn't mean we'll only use half the available threads.  Once the consumer starts
      // applying rules, those checks will start creating more concurrency.
      val concurrency = Integer.max(Runtime.getRuntime().availableProcessors(), 2) / 2
      val semaphore = Semaphore(concurrency)

      val toClear = mutableSetOf<McProject>()

      projectsToDependencyPaths.sortByHierarchy()
        .forEach { project ->

          launch {
            semaphore.withPermit {
              send(transform(project))
            }

            lock.withLock {

              toClear.add(project)
              upstreamPending.remove(project)

              toClear.removeIf { maybeClear ->
                maybeClear.hasRemainingDownstream()
                  .also { unused ->
                    if (unused) {
                      // after all checks are done for a given project, clear its cache
                      maybeClear.clearContext()
                    }
                  }
              }
            }
          }
        }

      invokeOnClose { _ ->
        // If any projects somehow haven't cleared their context, do so now.
        toClear.forEach { it.clearContext() }
      }
    }
  }

  /**
   * Prioritize the projects with the most dependencies
   *
   * @since 0.12.0
   */
  private suspend fun MutableMap<McProject, Set<StringProjectPath>>.sortByHierarchy(): List<McProject> {

    val pending = keys.mapAsync { project ->

      traced(project) {
        (project.depths().all().maxOfOrNull { it.depth } ?: 0) to project
      }
    }
      .toList()
      .sortedByDescending { it.first }
      .map { it.second }
      .toMutableList()

    val out = mutableListOf<McProject>()

    while (pending.isNotEmpty()) {
      val next = pending.firstOrNull { maybeNext ->
        values.none { paths -> maybeNext.path in paths }
      } ?: pending.first()

      out.add(next)
      pending.remove(next)
    }

    return out
  }

  private suspend fun McProject.allDependencies(): Set<McProject> {
    return generateSequence(
      depths().all().asSequence()
    ) { depths ->
      depths.flatMap { it.children }
        .takeIf { it.iterator().hasNext() }
    }
      .flatMapToSet { depths ->
        depths.mapTo(mutableListOf()) { it.dependentProject }
          .filterNot { it == this@allDependencies }
      }
  }
}
