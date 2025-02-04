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

import com.squareup.anvil.annotations.ContributesBinding
import dispatch.core.DispatcherProvider
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import modulecheck.dagger.SingleIn
import modulecheck.dagger.TaskScope
import javax.inject.Inject

/**
 * Uses [default] as the [io] dispatcher in order to keep things "fair" and limit heap size.
 *
 * @since 0.12.0
 */
@Suppress("InjectDispatcher")
@SingleIn(TaskScope::class)
@ContributesBinding(TaskScope::class)
class ModuleCheckDispatcherProvider @Inject constructor() : DispatcherProvider {

  override val default: CoroutineDispatcher = Dispatchers.Default
  override val io: CoroutineDispatcher = default
  override val main: CoroutineDispatcher = Dispatchers.Main
  override val mainImmediate: CoroutineDispatcher = Dispatchers.Main.immediate
  override val unconfined: CoroutineDispatcher = Dispatchers.Unconfined
}
