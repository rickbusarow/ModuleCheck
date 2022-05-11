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

package modulecheck.gradle.platforms

import com.android.build.gradle.TestedExtension
import modulecheck.gradle.platforms.sourcesets.AndroidSourceSetsParser
import modulecheck.gradle.platforms.sourcesets.JvmSourceSetsParser
import modulecheck.parsing.gradle.model.Configurations
import modulecheck.parsing.gradle.model.SourceSets
import javax.inject.Inject
import org.gradle.api.Project as GradleProject

class SourceSetsFactory @Inject constructor(
  private val jvmSourceSetsParser: JvmSourceSetsParser,
  private val androidSourceSetsParserFactory: AndroidSourceSetsParser.Factory
) {

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

        androidSourceSetsParserFactory.create(
          mcConfigurations, extension, hasTestFixturesPlugin
        ).parse()
      }
  }

  private fun GradleProject.jvmSourceSets(
    mcConfigurations: Configurations
  ): SourceSets {

    return jvmSourceSetsParser.parse(
      parsedConfigurations = mcConfigurations,
      gradleProject = this
    )
  }
}

fun GradleProject.isAndroid(): Boolean = extensions.findByType(TestedExtension::class.java) != null
