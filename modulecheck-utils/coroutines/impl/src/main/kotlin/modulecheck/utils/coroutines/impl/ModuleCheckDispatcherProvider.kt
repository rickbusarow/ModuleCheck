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

package modulecheck.utils.coroutines.impl

import com.squareup.anvil.annotations.ContributesBinding
import dispatch.core.DispatcherProvider
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import modulecheck.dagger.AppScope
import modulecheck.dagger.SingleIn
import modulecheck.utils.coroutines.LimitedDispatcher
import javax.inject.Inject

/**
 * Uses a [LimitedDispatcher] as the [default] in order to keep things "fair" and limit heap size.
 *
 * @since 0.12.0
 */
@SingleIn(AppScope::class)
@ContributesBinding(AppScope::class)
class ModuleCheckDispatcherProvider @Inject constructor(
  limitedDispatcher: LimitedDispatcher
) : DispatcherProvider {

  override val default: CoroutineDispatcher = limitedDispatcher
  override val io: CoroutineDispatcher = default
  override val main: CoroutineDispatcher = Dispatchers.Main
  override val mainImmediate: CoroutineDispatcher = Dispatchers.Main.immediate
  override val unconfined: CoroutineDispatcher = Dispatchers.Unconfined
}
