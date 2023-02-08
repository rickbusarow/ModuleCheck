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

package modulecheck.gradle.platforms.sourcesets

import modulecheck.gradle.platforms.android.AndroidBaseExtension
import modulecheck.gradle.platforms.android.UnsafeDirectAgpApiReference
import modulecheck.model.dependency.Configurations
import modulecheck.model.dependency.SourceSets
import modulecheck.parsing.gradle.model.GradleProject

fun interface AndroidSourceSetsParser {
  fun parse(): SourceSets

  fun interface Factory {
    /**
     * @param parsedConfigurations the configurations for this target project
     * @param extension the instance of AGP extension applied to this project
     * @param hasTestFixturesPlugin has either the `java-test-fixtures` plugin or
     *   `buildFeatures.testFixtures` is enabled in the extension
     * @param gradleProject the project being parsed
     * @return the [AndroidSourceSetsParser] for this project
     * @since 0.12.0
     */
    @UnsafeDirectAgpApiReference
    fun create(
      parsedConfigurations: Configurations,
      extension: AndroidBaseExtension,
      hasTestFixturesPlugin: Boolean,
      gradleProject: GradleProject
    ): AndroidSourceSetsParser
  }
}
