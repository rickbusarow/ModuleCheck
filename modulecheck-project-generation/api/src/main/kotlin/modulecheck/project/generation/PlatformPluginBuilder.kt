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

package modulecheck.project.generation

import modulecheck.model.dependency.AndroidPlatformPlugin
import modulecheck.model.dependency.AndroidPlatformPlugin.AndroidApplicationPlugin
import modulecheck.model.dependency.AndroidPlatformPlugin.AndroidDynamicFeaturePlugin
import modulecheck.model.dependency.AndroidPlatformPlugin.AndroidLibraryPlugin
import modulecheck.model.dependency.AndroidPlatformPlugin.AndroidTestPlugin
import modulecheck.model.dependency.ConfigurationName
import modulecheck.model.dependency.Configurations
import modulecheck.model.dependency.ExternalDependencies
import modulecheck.model.dependency.JvmPlatformPlugin.JavaLibraryPlugin
import modulecheck.model.dependency.JvmPlatformPlugin.KotlinJvmPlugin
import modulecheck.model.dependency.PlatformPlugin
import modulecheck.model.dependency.ProjectDependencies
import modulecheck.model.dependency.ProjectPath.StringProjectPath
import modulecheck.model.dependency.SourceSets
import modulecheck.model.sourceset.SourceSetName
import modulecheck.parsing.kotlin.compiler.impl.DependencyModuleDescriptorAccess
import modulecheck.parsing.source.PackageName
import modulecheck.parsing.source.UnqualifiedAndroidResource
import java.io.File

interface PlatformPluginBuilder<T : PlatformPlugin> {
  val sourceSets: MutableMap<SourceSetName, SourceSetBuilder>
  val configurations: MutableMap<ConfigurationName, ConfigBuilder>

  fun toPlugin(
    dependencyModuleDescriptorAccess: DependencyModuleDescriptorAccess,
    projectPath: StringProjectPath,
    projectDependencies: ProjectDependencies,
    externalDependencies: ExternalDependencies
  ): T
}

data class JavaLibraryPluginBuilder(
  override val sourceSets: MutableMap<SourceSetName, SourceSetBuilder> = mutableMapOf(),
  override val configurations: MutableMap<ConfigurationName, ConfigBuilder> = mutableMapOf()
) : PlatformPluginBuilder<JavaLibraryPlugin> {

  override fun toPlugin(
    dependencyModuleDescriptorAccess: DependencyModuleDescriptorAccess,
    projectPath: StringProjectPath,
    projectDependencies: ProjectDependencies,
    externalDependencies: ExternalDependencies
  ): JavaLibraryPlugin = JavaLibraryPlugin(
    sourceSets = SourceSets(
      sourceSets.mapValues {
        it.value.toSourceSet(
          dependencyModuleDescriptorAccess = dependencyModuleDescriptorAccess,
          projectPath = projectPath
        )
      }
    ),
    configurations = Configurations(
      configurations.mapValues {
        it.value.toConfig(
          configFactory(
            projectDependencies,
            externalDependencies
          )
        )
      }
    )
  )
}

data class KotlinJvmPluginBuilder(
  override val sourceSets: MutableMap<SourceSetName, SourceSetBuilder> = mutableMapOf(),
  override val configurations: MutableMap<ConfigurationName, ConfigBuilder> = mutableMapOf()
) : PlatformPluginBuilder<KotlinJvmPlugin> {
  override fun toPlugin(
    dependencyModuleDescriptorAccess: DependencyModuleDescriptorAccess,
    projectPath: StringProjectPath,
    projectDependencies: ProjectDependencies,
    externalDependencies: ExternalDependencies
  ): KotlinJvmPlugin = KotlinJvmPlugin(
    sourceSets = SourceSets(
      sourceSets.mapValues {
        it.value.toSourceSet(
          dependencyModuleDescriptorAccess = dependencyModuleDescriptorAccess,
          projectPath = projectPath
        )
      }
    ),
    configurations = Configurations(
      configurations.mapValues {
        it.value.toConfig(
          configFactory(
            projectDependencies,
            externalDependencies
          )
        )
      }
    )
  )
}

interface AndroidPlatformPluginBuilder<T : AndroidPlatformPlugin> : PlatformPluginBuilder<T> {
  var viewBindingEnabled: Boolean
  var nonTransientRClass: Boolean
  var kotlinAndroidExtensionEnabled: Boolean
  val manifests: MutableMap<SourceSetName, File>

  /**
   * @see AndroidPlatformPlugin.namespaces
   * @since 0.13.0
   */
  val namespaces: MutableMap<SourceSetName, PackageName>
  val resValues: MutableMap<SourceSetName, Set<UnqualifiedAndroidResource>>
}

data class AndroidApplicationPluginBuilder(
  override var viewBindingEnabled: Boolean = true,
  override var nonTransientRClass: Boolean = false,
  override var kotlinAndroidExtensionEnabled: Boolean = true,
  override val manifests: MutableMap<SourceSetName, File> = mutableMapOf(),
  override val namespaces: MutableMap<SourceSetName, PackageName> = mutableMapOf(),
  override val sourceSets: MutableMap<SourceSetName, SourceSetBuilder> = mutableMapOf(),
  override val configurations: MutableMap<ConfigurationName, ConfigBuilder> = mutableMapOf(),
  override val resValues: MutableMap<SourceSetName, Set<UnqualifiedAndroidResource>> = mutableMapOf()
) : AndroidPlatformPluginBuilder<AndroidApplicationPlugin> {
  override fun toPlugin(
    dependencyModuleDescriptorAccess: DependencyModuleDescriptorAccess,
    projectPath: StringProjectPath,
    projectDependencies: ProjectDependencies,
    externalDependencies: ExternalDependencies
  ): AndroidApplicationPlugin = AndroidApplicationPlugin(
    sourceSets = SourceSets(
      sourceSets.mapValues {
        it.value.toSourceSet(
          dependencyModuleDescriptorAccess = dependencyModuleDescriptorAccess,
          projectPath = projectPath
        )
      }
    ),
    configurations = Configurations(
      configurations.mapValues {
        it.value.toConfig(
          configFactory(
            projectDependencies,
            externalDependencies
          )
        )
      }
    ),
    nonTransientRClass = nonTransientRClass,
    viewBindingEnabled = viewBindingEnabled,
    kotlinAndroidExtensionEnabled = kotlinAndroidExtensionEnabled,
    manifests = manifests,
    namespaces = namespaces,
    resValuesLazy = lazy { resValues }
  )
}

data class AndroidLibraryPluginBuilder(
  override var viewBindingEnabled: Boolean = true,
  override var nonTransientRClass: Boolean = false,
  override var kotlinAndroidExtensionEnabled: Boolean = true,
  var buildConfigEnabled: Boolean = true,
  var androidResourcesEnabled: Boolean = true,
  override val manifests: MutableMap<SourceSetName, File> = mutableMapOf(),
  override val namespaces: MutableMap<SourceSetName, PackageName> = mutableMapOf(),
  override val sourceSets: MutableMap<SourceSetName, SourceSetBuilder> = mutableMapOf(),
  override val configurations: MutableMap<ConfigurationName, ConfigBuilder> = mutableMapOf(),
  override val resValues: MutableMap<SourceSetName, Set<UnqualifiedAndroidResource>> = mutableMapOf()
) : AndroidPlatformPluginBuilder<AndroidLibraryPlugin> {
  override fun toPlugin(
    dependencyModuleDescriptorAccess: DependencyModuleDescriptorAccess,
    projectPath: StringProjectPath,
    projectDependencies: ProjectDependencies,
    externalDependencies: ExternalDependencies
  ): AndroidLibraryPlugin = AndroidLibraryPlugin(
    sourceSets = SourceSets(
      sourceSets.mapValues {
        it.value.toSourceSet(
          dependencyModuleDescriptorAccess = dependencyModuleDescriptorAccess,
          projectPath = projectPath
        )
      }
    ),
    configurations = Configurations(
      configurations.mapValues {
        it.value.toConfig(
          configFactory(
            projectDependencies,
            externalDependencies
          )
        )
      }
    ),
    nonTransientRClass = nonTransientRClass,
    viewBindingEnabled = viewBindingEnabled,
    kotlinAndroidExtensionEnabled = kotlinAndroidExtensionEnabled,
    manifests = manifests,
    namespaces = namespaces,
    androidResourcesEnabled = androidResourcesEnabled,
    buildConfigEnabled = buildConfigEnabled,
    resValuesLazy = lazy { resValues }
  )
}

data class AndroidDynamicFeaturePluginBuilder(
  override var viewBindingEnabled: Boolean = true,
  override var nonTransientRClass: Boolean = false,
  override var kotlinAndroidExtensionEnabled: Boolean = true,
  var buildConfigEnabled: Boolean = true,
  override val manifests: MutableMap<SourceSetName, File> = mutableMapOf(),
  override val namespaces: MutableMap<SourceSetName, PackageName> = mutableMapOf(),
  override val sourceSets: MutableMap<SourceSetName, SourceSetBuilder> = mutableMapOf(),
  override val configurations: MutableMap<ConfigurationName, ConfigBuilder> = mutableMapOf(),
  override val resValues: MutableMap<SourceSetName, Set<UnqualifiedAndroidResource>> = mutableMapOf()
) : AndroidPlatformPluginBuilder<AndroidDynamicFeaturePlugin> {
  override fun toPlugin(
    dependencyModuleDescriptorAccess: DependencyModuleDescriptorAccess,
    projectPath: StringProjectPath,
    projectDependencies: ProjectDependencies,
    externalDependencies: ExternalDependencies
  ): AndroidDynamicFeaturePlugin = AndroidDynamicFeaturePlugin(
    sourceSets = SourceSets(
      sourceSets.mapValues {
        it.value.toSourceSet(
          dependencyModuleDescriptorAccess = dependencyModuleDescriptorAccess,
          projectPath = projectPath
        )
      }
    ),
    configurations = Configurations(
      configurations.mapValues {
        it.value.toConfig(
          configFactory(
            projectDependencies,
            externalDependencies
          )
        )
      }
    ),
    nonTransientRClass = nonTransientRClass,
    viewBindingEnabled = viewBindingEnabled,
    kotlinAndroidExtensionEnabled = kotlinAndroidExtensionEnabled,
    manifests = manifests,
    namespaces = namespaces,
    buildConfigEnabled = buildConfigEnabled,
    resValuesLazy = lazy { resValues }
  )
}

data class AndroidTestPluginBuilder(
  override var viewBindingEnabled: Boolean = true,
  override var nonTransientRClass: Boolean = false,
  override var kotlinAndroidExtensionEnabled: Boolean = true,
  var buildConfigEnabled: Boolean = true,
  override val manifests: MutableMap<SourceSetName, File> = mutableMapOf(),
  override val namespaces: MutableMap<SourceSetName, PackageName> = mutableMapOf(),
  override val sourceSets: MutableMap<SourceSetName, SourceSetBuilder> = mutableMapOf(),
  override val configurations: MutableMap<ConfigurationName, ConfigBuilder> = mutableMapOf(),
  override val resValues: MutableMap<SourceSetName, Set<UnqualifiedAndroidResource>> = mutableMapOf()
) : AndroidPlatformPluginBuilder<AndroidTestPlugin> {
  override fun toPlugin(
    dependencyModuleDescriptorAccess: DependencyModuleDescriptorAccess,
    projectPath: StringProjectPath,
    projectDependencies: ProjectDependencies,
    externalDependencies: ExternalDependencies
  ): AndroidTestPlugin = AndroidTestPlugin(
    sourceSets = SourceSets(
      sourceSets.mapValues {
        it.value.toSourceSet(
          dependencyModuleDescriptorAccess = dependencyModuleDescriptorAccess,
          projectPath = projectPath
        )
      }
    ),
    configurations = Configurations(
      configurations.mapValues {
        it.value.toConfig(
          configFactory(
            projectDependencies,
            externalDependencies
          )
        )
      }
    ),
    nonTransientRClass = nonTransientRClass,
    viewBindingEnabled = viewBindingEnabled,
    kotlinAndroidExtensionEnabled = kotlinAndroidExtensionEnabled,
    manifests = manifests,
    namespaces = namespaces,
    buildConfigEnabled = buildConfigEnabled,
    resValuesLazy = lazy { resValues }
  )
}
