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

@file:Suppress("DEPRECATION")

package modulecheck.gradle.platforms.sourcesets

import modulecheck.gradle.platforms.android.AndroidBaseExtension
import modulecheck.gradle.platforms.android.UnsafeDirectAgpApiReference
import modulecheck.parsing.gradle.model.Configurations
import modulecheck.parsing.gradle.model.SourceSets

fun interface AndroidSourceSetsParser {
  fun parse(): SourceSets

  fun interface Factory {
    /**
     * @param parsedConfigurations the configurations for this target project
     * @param extension the instance of AGP extension applied to this project
     * @param hasTestFixturesPlugin has either the `java-test-fixtures` plugin or
     *   `buildFeatures.testFixtures` is enabled in the extension
     * @return the [AndroidSourceSetsParser] for this project
     */
    @UnsafeDirectAgpApiReference
    fun create(
      parsedConfigurations: Configurations,
      extension: AndroidBaseExtension,
      hasTestFixturesPlugin: Boolean
    ): AndroidSourceSetsParser
  }
}
