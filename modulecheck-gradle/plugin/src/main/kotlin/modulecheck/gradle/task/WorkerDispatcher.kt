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

package modulecheck.gradle.task

import com.squareup.anvil.annotations.ContributesBinding
import kotlinx.coroutines.InternalCoroutinesApi
import kotlinx.coroutines.Runnable
import modulecheck.dagger.AppScope
import modulecheck.utils.coroutines.RealDispatchAction
import modulecheck.utils.coroutines.WorkerDispatcher
import modulecheck.utils.coroutines.WorkerFacade
import org.gradle.api.provider.Property
import org.gradle.workers.WorkAction
import org.gradle.workers.WorkParameters
import org.gradle.workers.WorkerExecutor
import javax.inject.Inject
import kotlin.coroutines.CoroutineContext

@ContributesBinding(AppScope::class)
class RealWorkerFacade @Inject constructor(
  private val workerExecutor: WorkerExecutor
) : WorkerFacade {
  override fun <T> invoke(action: RealDispatchAction<T>): T {

    var result: T? = null

    workerExecutor.noIsolation()
      .also {
        it.submit(DispatchAction::class.java) { params ->
          params.action.set {
            result = action()
          }
        }
      }.await()

    @Suppress("UNCHECKED_CAST")
    return result as T
  }
}

@ContributesBinding(AppScope::class)
class RealWorkerDispatcher @Inject constructor(
  workerExecutor: WorkerExecutor
) : WorkerDispatcher() {

  private val queue = workerExecutor.processIsolation()

  @InternalCoroutinesApi
  override fun dispatchYield(context: CoroutineContext, block: Runnable) {
    super.dispatchYield(context, block)
  }

  override fun dispatch(context: CoroutineContext, block: Runnable) {

    queue.submit(DispatchAction::class.java) {

      println("                         dispatching")

      it.action.set { block.run() }
      println("                                                  dispatched")
    }
  }
}

abstract class DispatchAction : WorkAction<DispatchParams> {
  override fun execute() {
    parameters.action.get().invoke()
  }
}

interface DispatchParams : WorkParameters {
  val action: Property<() -> Unit>
}
