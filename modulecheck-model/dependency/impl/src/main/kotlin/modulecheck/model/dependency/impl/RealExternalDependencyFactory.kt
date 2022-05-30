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
import modulecheck.dagger.AppScope
import modulecheck.dagger.DaggerList
import modulecheck.model.dependency.ExternalDependency
import modulecheck.parsing.gradle.model.ConfigurationName
import modulecheck.utils.lazy.unsafeLazy
import javax.inject.Inject

/**
 * Creates an [ExternalDependency] for given arguments, a `List<CodeGeneratorBinding>` to look up
 * a [CodeGeneratorBinding] in the event that the project dependency in question
 * is an annotation processor.
 *
 * @property generatorBindings the list of possible bindings to search
 */
@ContributesBinding(AppScope::class)
class RealExternalDependencyFactory @Inject constructor(
  private val generatorBindings: DaggerList<CodeGeneratorBinding>
) : ExternalDependency.Factory {
  override fun create(
    configurationName: ConfigurationName,
    group: String?,
    moduleName: String,
    version: String?
  ): ExternalDependency {
    val name by unsafeLazy { "${group ?: ""}:$moduleName" }

    return when {
      configurationName.isKapt() -> ExternalDependency.ExternalCodeGeneratorDependency(
        configurationName = configurationName,
        group = group,
        moduleName = moduleName,
        version = version,
        codeGeneratorBindingOrNull = generatorBindings.filterIsInstance<CodeGeneratorBinding.AnnotationProcessor>()
          .firstOrNull { it.generatorMavenCoordinates == name }
      )

      else -> ExternalDependency.ExternalRuntimeDependency(
        configurationName = configurationName,
        group = group,
        moduleName = moduleName,
        version = version,
      )
    }
  }
}
