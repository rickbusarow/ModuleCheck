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

package modulecheck.config.internal

import com.squareup.anvil.annotations.ContributesTo
import dagger.Provides
import modulecheck.config.CodeGeneratorBinding
import modulecheck.config.ModuleCheckSettings
import modulecheck.dagger.AppScope
import modulecheck.dagger.DaggerList
import modulecheck.model.dependency.CodeGenerator
import modulecheck.utils.mapToSet

/**
 * Dagger bindings for [CodeGenerator]
 *
 * @since 0.12.0
 */
@dagger.Module
@ContributesTo(AppScope::class)
object CodeGeneratorBindingsModule {

  /**
   * @return the default [CodeGeneratorBindings][defaultCodeGeneratorBindings] with custom ones
   *   defined in [ModuleCheckSettings]
   * @since 0.12.0
   */
  @Provides
  fun provideCodeGeneratorBindings(
    settings: ModuleCheckSettings
  ): DaggerList<CodeGeneratorBinding> {
    return settings.additionalCodeGenerators
      .plus(defaultCodeGeneratorBindings())
      .plus(
        @Suppress("DEPRECATION")
        settings.additionalKaptMatchers.mapToSet { it.toCodeGeneratorBinding() }
      )
  }
}
