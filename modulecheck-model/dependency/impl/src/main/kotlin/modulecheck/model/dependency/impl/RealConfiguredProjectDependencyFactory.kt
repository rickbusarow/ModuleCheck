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

package modulecheck.model.dependency.impl

import com.squareup.anvil.annotations.ContributesBinding
import modulecheck.config.CodeGeneratorBinding
import modulecheck.config.CodeGeneratorBinding.AnnotationProcessor
import modulecheck.dagger.AppScope
import modulecheck.dagger.DaggerList
import modulecheck.model.dependency.ConfiguredProjectDependency
import modulecheck.model.dependency.ConfiguredProjectDependency.ConfiguredCodeGeneratorProjectDependency
import modulecheck.model.dependency.ConfiguredProjectDependency.ConfiguredRuntimeProjectDependency
import modulecheck.parsing.gradle.model.ConfigurationName
import modulecheck.parsing.gradle.model.ProjectPath
import modulecheck.parsing.gradle.model.ProjectPath.StringProjectPath
import modulecheck.parsing.gradle.model.ProjectPath.TypeSafeProjectPath
import modulecheck.parsing.gradle.model.TypeSafeProjectPathResolver
import javax.inject.Inject

/**
 * Creates a [ConfiguredProjectDependency] for given arguments,
 * using [pathResolver] and [generatorBindings] to look up a [CodeGeneratorBinding] in the event
 * that the project dependency in question is an annotation processor.
 *
 * @property pathResolver used to look up the [StringProjectPath] of any internal project code
 * generators.  This is necessary in order to look up the [CodeGeneratorBinding].
 * @property generatorBindings the list of possible bindings to search
 */
@ContributesBinding(AppScope::class)
class RealConfiguredProjectDependencyFactory @Inject constructor(
  private val pathResolver: TypeSafeProjectPathResolver,
  private val generatorBindings: DaggerList<CodeGeneratorBinding>
) : ConfiguredProjectDependency.Factory {
  override fun create(
    configurationName: ConfigurationName,
    path: ProjectPath,
    isTestFixture: Boolean
  ): ConfiguredProjectDependency {

    val stringPath = when (path) {
      is StringProjectPath -> path
      is TypeSafeProjectPath -> pathResolver.resolveStringProjectPath(path)
    }

    return when {
      configurationName.isKapt() -> ConfiguredCodeGeneratorProjectDependency(
        configurationName = configurationName, path = path, isTestFixture = isTestFixture,
        codeGeneratorBindingOrNull = generatorBindings.filterIsInstance<AnnotationProcessor>()
          .firstOrNull { it.generatorMavenCoordinates == stringPath.value }
      )

      else -> ConfiguredRuntimeProjectDependency(
        configurationName = configurationName,
        path = path,
        isTestFixture = isTestFixture
      )
    }
  }
}
