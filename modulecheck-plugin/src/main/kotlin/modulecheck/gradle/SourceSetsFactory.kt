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

package modulecheck.gradle

import com.android.build.gradle.TestedExtension
import modulecheck.gradle.internal.isAndroid
import modulecheck.gradle.internal.sourcesets.AndroidSourceSetsParser
import modulecheck.gradle.internal.sourcesets.JvmSourceSetParser
import modulecheck.parsing.gradle.Configurations
import modulecheck.parsing.gradle.SourceSets
import javax.inject.Inject

class SourceSetsFactory @Inject constructor() {

  fun create(
    gradleProject: GradleProject,
    configurations: Configurations,
    hasTestFixturesPlugin: Boolean
  ): SourceSets {

    return if (gradleProject.isAndroid()) {
      gradleProject.androidSourceSets(configurations, hasTestFixturesPlugin)
    } else {
      gradleProject.jvmSourceSets(configurations)
    }
  }

  @Suppress("UnstableApiUsage")
  private fun GradleProject.androidSourceSets(
    mcConfigurations: Configurations,
    hasTestFixturesPlugin: Boolean
  ): SourceSets {

    return extensions.getByType(TestedExtension::class.java)
      .let { extension ->

        AndroidSourceSetsParser.parse(
          mcConfigurations, extension, hasTestFixturesPlugin
        )
      }
  }

  private fun GradleProject.jvmSourceSets(
    mcConfigurations: Configurations
  ): SourceSets {

    return JvmSourceSetParser.parse(
      parsedConfigurations = mcConfigurations,
      gradleProject = this
    )
  }
}
