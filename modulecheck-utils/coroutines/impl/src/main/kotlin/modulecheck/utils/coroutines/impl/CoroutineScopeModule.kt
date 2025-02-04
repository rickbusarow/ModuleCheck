/*
 * Copyright (C) 2021-2025 Rick Busarow
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

package modulecheck.utils.coroutines.impl

import com.squareup.anvil.annotations.ContributesTo
import dagger.Module
import dagger.Provides
import dispatch.core.DefaultCoroutineScope
import dispatch.core.DispatcherProvider
import dispatch.core.IOCoroutineScope
import dispatch.core.MainCoroutineScope
import dispatch.core.MainImmediateCoroutineScope
import dispatch.core.UnconfinedCoroutineScope
import kotlinx.coroutines.CoroutineScope
import modulecheck.dagger.TaskScope

@Suppress("UndocumentedPublicFunction", "UndocumentedPublicClass", "InjectDispatcher")
@Module
@ContributesTo(TaskScope::class)
object CoroutineScopeModule {

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

@ContributesTo(TaskScope::class)
interface DispatcherProviderComponent {
  val dispatcherProvider: DispatcherProvider
}
