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
@file:Suppress("MaxLineLength")

package modulecheck.utils.coroutines.impl

import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.InternalCoroutinesApi
import kotlinx.coroutines.handleCoroutineException
import kotlinx.coroutines.internal.SynchronizedObject
import modulecheck.utils.coroutines.LimitedDispatcher
import kotlin.coroutines.CoroutineContext
import kotlin.coroutines.EmptyCoroutineContext

/**
 * Forked from kotlinx.coroutines
 *
 * The result of .limitedParallelism(x) call, a dispatcher that wraps the given dispatcher, but
 * limits the parallelism level, while trying to emulate fairness.
 *
 * @since 0.12.0
 */
@OptIn(InternalCoroutinesApi::class)
class LimitedDispatcherImpl(
  private val dispatcher: CoroutineDispatcher,
  private val parallelism: Int
) : LimitedDispatcher(), Runnable {

  @Volatile
  private var runningWorkers = 0

  private val queue = LockFreeTaskQueue<Runnable>(singleConsumer = false)

  // A separate object that we can synchronize on for K/N
  private val workerAllocationLock = SynchronizedObject()

  @ExperimentalCoroutinesApi
  override fun limitedParallelism(parallelism: Int): CoroutineDispatcher {
    parallelism.checkParallelism()
    if (parallelism >= this.parallelism) return this
    return super.limitedParallelism(parallelism)
  }

  override fun run() {
    var fairnessCounter = 0
    while (true) {
      val task = queue.removeFirstOrNull()
      if (task != null) {
        @Suppress("TooGenericExceptionCaught")
        try {
          task.run()
        } catch (e: Throwable) {
          handleCoroutineException(EmptyCoroutineContext, e)
        }
        // 16 is our out-of-thin-air constant to emulate fairness. Used in JS dispatchers as well
        @Suppress("MagicNumber")
        if (++fairnessCounter >= 16 && dispatcher.isDispatchNeeded(this)) {
          // Do "yield" to let other views to execute their runnable as well
          // Note that we do not decrement 'runningWorkers' as we still committed to do our part of work
          dispatcher.dispatch(this, this)
          return
        }
        continue
      }

      synchronized(workerAllocationLock) {
        --runningWorkers
        if (queue.size == 0) return
        ++runningWorkers
        fairnessCounter = 0
      }
    }
  }

  override fun dispatch(context: CoroutineContext, block: Runnable) {
    dispatchInternal(block) {
      dispatcher.dispatch(this, this)
    }
  }

  @InternalCoroutinesApi
  override fun dispatchYield(context: CoroutineContext, block: Runnable) {
    dispatchInternal(block) {
      dispatcher.dispatchYield(this, this)
    }
  }

  private inline fun dispatchInternal(block: Runnable, dispatch: () -> Unit) {
    // Add task to queue so running workers will be able to see that
    if (addAndTryDispatching(block)) return
    /*
     * Protect against the race when the number of workers is enough,
     * but one (because of synchronized serialization) attempts to complete,
     * and we just observed the number of running workers smaller than the actual
     * number (hit right between `--runningWorkers` and `++runningWorkers` in `run()`)
     */
    if (!tryAllocateWorker()) return
    dispatch()
  }

  private fun tryAllocateWorker(): Boolean {
    synchronized(workerAllocationLock) {
      if (runningWorkers >= parallelism) return false
      ++runningWorkers
      return true
    }
  }

  private fun addAndTryDispatching(block: Runnable): Boolean {
    queue.addLast(block)
    return runningWorkers >= parallelism
  }
}

// Save a few bytecode ops
internal fun Int.checkParallelism() =
  require(this >= 1) { "Expected positive parallelism level, but got $this" }
