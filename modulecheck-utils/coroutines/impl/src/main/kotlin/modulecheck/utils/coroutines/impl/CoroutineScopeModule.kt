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

<<<<<<<< HEAD:modulecheck-utils/coroutines-wiring/src/main/kotlin/modulecheck/utils/coroutines/CoroutineScopeModule.kt
package modulecheck.utils.coroutines
|||||||| 826f2215:modulecheck-dagger/src/main/kotlin/modulecheck/dagger/CoroutineScopeModule.kt
package modulecheck.dagger
========
package modulecheck.utils.coroutines.impl
>>>>>>>> main:modulecheck-utils/coroutines/impl /src/main/kotlin/modulecheck/utils/coroutines/impl /CoroutineScopeModule.kt

import com.squareup.anvil.annotations.ContributesBinding
import com.squareup.anvil.annotations.ContributesTo
import dagger.Module
import dagger.Provides
import dispatch.core.DefaultCoroutineScope
import dispatch.core.DispatcherProvider
import dispatch.core.IOCoroutineScope
import dispatch.core.MainCoroutineScope
import dispatch.core.MainImmediateCoroutineScope
import dispatch.core.UnconfinedCoroutineScope
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope

<<<<<<<< HEAD:modulecheck-utils/coroutines-wiring/src/main/kotlin/modulecheck/utils/coroutines/CoroutineScopeModule.kt
import kotlinx.coroutines.Dispatchers
import modulecheck.dagger.AppScope
import modulecheck.dagger.SingleIn
import modulecheck.utils.coroutines.fork.LimitedDispatcher
import javax.inject.Inject

|||||||| 826f2215:modulecheck-dagger/src/main/kotlin/modulecheck/dagger/CoroutineScopeModule.kt
========
import kotlinx.coroutines.Dispatchers
import modulecheck.dagger.AppScope
import modulecheck.dagger.SingleIn
import modulecheck.utils.coroutines.LimitedDispatcher

>>>>>>>> main:modulecheck-utils/coroutines/impl /src/main/kotlin/modulecheck/utils/coroutines/impl /CoroutineScopeModule.kt

@Suppress("UndocumentedPublicFunction", "UndocumentedPublicClass")
@Module
@ContributesTo(AppScope::class)
object CoroutineScopeModule {

  private val DEFAULT_CONCURRENCY: Int
    get() = Integer.max(Runtime.getRuntime().availableProcessors(), 2)

  @Provides
  @SingleIn(AppScope::class)
  fun provideDefaultDispatcher(): LimitedDispatcher = LimitedDispatcherImpl(
    dispatcher = Dispatchers.Default,
    parallelism = DEFAULT_CONCURRENCY
  )

  @Provides
  fun provideCoroutineScope(): CoroutineScope = DefaultCoroutineScope()

  @Provides
  fun provideDefaultCoroutineScope(): DefaultCoroutineScope = DefaultCoroutineScope()

  @Provides
  fun provideIOCoroutineScope(): IOCoroutineScope = IOCoroutineScope()

  @Provides
  fun provideMainCoroutineScope(): MainCoroutineScope = MainCoroutineScope()

  @Provides
  fun provideMainImmediateCoroutineScope(): MainImmediateCoroutineScope =
    MainImmediateCoroutineScope()

  @Provides
  fun provideUnconfinedCoroutineScope(): UnconfinedCoroutineScope = UnconfinedCoroutineScope()
}

@ContributesTo(AppScope::class)
interface DispatcherProviderComponent {
  val dispatcherProvider: DispatcherProvider
}

abstract class WorkerDispatcher : CoroutineDispatcher()

@SingleIn(AppScope::class)
@ContributesBinding(AppScope::class)
class ModuleCheckDispatcherProvider @Inject constructor(
  workerDispatcher: WorkerDispatcher
) : DispatcherProvider {

  override val default: CoroutineDispatcher = LimitedDispatcher(
    dispatcher = Dispatchers.Default,
    parallelism = Integer.max(Runtime.getRuntime().availableProcessors(), 2)
  )
  override val io: CoroutineDispatcher = default
  override val main: CoroutineDispatcher = Dispatchers.Main
  override val mainImmediate: CoroutineDispatcher = Dispatchers.Main.immediate
  override val unconfined: CoroutineDispatcher = Dispatchers.Unconfined
}
