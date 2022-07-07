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
import modulecheck.dagger.AppScope
import modulecheck.parsing.kotlin.compiler.KotlinEnvironment
import modulecheck.parsing.kotlin.compiler.McPsiFileFactory
import modulecheck.parsing.kotlin.compiler.internal.AbstractMcPsiFileFactory
import javax.inject.Inject

/**
 * A real implementation of [McPsiFileFactory] using a curated [KotlinEnvironment].
 *
 * The files created from this factory are backed by a meaningful
 * [BindingContext][org.jetbrains.kotlin.resolve.BindingContext] which is aware of the full
 * classpath and may be used for type resolution.
 */
class RealMcPsiFileFactory(
  private val kotlinEnvironment: KotlinEnvironment
) : AbstractMcPsiFileFactory(),
  McPsiFileFactory {

  override val coreEnvironment by lazy { kotlinEnvironment.coreEnvironment }

  /** Creates an instance of [McPsiFileFactory] */
  @ContributesBinding(AppScope::class)
  class Factory @Inject constructor() : McPsiFileFactory.Factory {
    override fun create(
      kotlinEnvironment: KotlinEnvironment
    ): RealMcPsiFileFactory = RealMcPsiFileFactory(kotlinEnvironment)
  }
}
