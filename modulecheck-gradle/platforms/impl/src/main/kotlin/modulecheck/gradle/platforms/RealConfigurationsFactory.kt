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
import modulecheck.dagger.TaskScope
import modulecheck.model.dependency.ConfigFactory
import modulecheck.model.dependency.Configurations
import modulecheck.model.dependency.ExternalDependency
import modulecheck.model.dependency.ProjectDependency
import modulecheck.model.dependency.ProjectPath.StringProjectPath
import modulecheck.model.dependency.asConfigurationName
import modulecheck.parsing.gradle.model.GradleConfiguration
import modulecheck.parsing.gradle.model.GradleProject
import modulecheck.parsing.gradle.model.GradleProjectDependency
import modulecheck.utils.mapToSet
import org.gradle.api.artifacts.ExternalModuleDependency
import org.gradle.api.artifacts.ModuleDependency
import org.gradle.api.initialization.dsl.ScriptHandler
import org.gradle.internal.component.external.model.ProjectDerivedCapability
import javax.inject.Inject

@ContributesBinding(TaskScope::class)
class RealConfigurationsFactory @Inject constructor(
  private val projectDependencyFactory: ProjectDependency.Factory,
  private val externalDependencyFactory: ExternalDependency.Factory
) : ConfigurationsFactory {

  override fun create(gradleProject: GradleProject): Configurations {

    val configFactory = ConfigFactory<GradleConfiguration>(
      identifier = { name },
      projectDependencies = { projectDependencies(this) },
      externalDependencies = { externalDependencies(this) },
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

  private fun projectDependencies(
    configuration: GradleConfiguration
  ): Set<ProjectDependency> = configuration.dependencies
    .withType(GradleProjectDependency::class.java)
    .mapToSet { gradleProjectDependency ->

      projectDependencyFactory.create(
        configurationName = configuration.name.asConfigurationName(),
        path = StringProjectPath(gradleProjectDependency.dependencyProject.path),
        isTestFixture = gradleProjectDependency.isTestFixtures()
      )
    }

  private fun externalDependencies(
    configuration: GradleConfiguration
  ): Set<ExternalDependency> = configuration.dependencies
    .filterIsInstance<ExternalModuleDependency>()
    .mapToSet { dep ->

      externalDependencyFactory.create(
        configurationName = configuration.name.asConfigurationName(),
        group = dep.group,
        moduleName = dep.name,
        version = dep.version,
        isTestFixture = dep.isTestFixtures()
      )
    }

  private fun ModuleDependency.isTestFixtures() = requestedCapabilities
    .filterIsInstance<ProjectDerivedCapability>()
    .any { capability -> capability.capabilityId.endsWith(TEST_FIXTURES_SUFFIX) }

  companion object {
    private const val TEST_FIXTURES_SUFFIX = "-test-fixtures"
    private const val TEST_FIXTURES_PLUGIN_ID = "java-test-fixtures"
  }
}
