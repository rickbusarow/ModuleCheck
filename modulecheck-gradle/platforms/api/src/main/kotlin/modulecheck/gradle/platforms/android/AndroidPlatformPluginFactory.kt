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

package modulecheck.gradle.platforms.android

import modulecheck.model.dependency.AndroidPlatformPlugin

interface AndroidPlatformPluginFactory {
  /**
   * @param gradleProject the target project
   * @param androidCommonExtension the instance of AGP extension applied to this project
   * @param hasTestFixturesPlugin has either the `java-test-fixtures`
   *   plugin or `buildFeatures.testFixtures` is enabled in the extension
   * @return the [AndroidPlatformPlugin] capturing all of this project's settings
   * @since 0.12.0
   */
  @UnsafeDirectAgpApiReference
  fun create(
    gradleProject: org.gradle.api.Project,
    androidCommonExtension: AndroidCommonExtension,
    hasTestFixturesPlugin: Boolean
  ): AndroidPlatformPlugin
}
