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

import com.squareup.anvil.annotations.ContributesBinding
import modulecheck.dagger.AppScope
import modulecheck.parsing.gradle.model.Configurations
import modulecheck.parsing.gradle.model.asConfigurationName
import org.gradle.api.Project
import org.gradle.api.artifacts.Configuration
import org.gradle.api.initialization.dsl.ScriptHandler
import javax.inject.Inject

@ContributesBinding(AppScope::class)
class RealConfigurationsFactory @Inject constructor() : ConfigurationsFactory {

  override fun create(gradleProject: Project): Configurations {

    val configFactory = ConfigFactory<Configuration>(
      identifier = { name },
      allFactory = { gradleProject.configurations.asSequence() },
      extendsFrom = {
        gradleProject.configurations.findByName(this)
          ?.extendsFrom
          ?.toList()
          .orEmpty()
      }
    )

    val map = gradleProject.configurations
      .filterNot { it.name == ScriptHandler.CLASSPATH_CONFIGURATION }
      .associate { configuration ->

        configuration.name.asConfigurationName() to configFactory.create(configuration)
      }
    return Configurations(map)
  }
}
