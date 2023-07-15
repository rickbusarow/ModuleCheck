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

@file:JvmMultifileClass

package modulecheck.gradle.platforms.android.internal

import com.android.build.api.variant.AndroidComponentsExtension
import com.android.build.gradle.BasePlugin
import modulecheck.gradle.platforms.android.AgpApiAccess
import modulecheck.gradle.platforms.android.AndroidTestedExtension
import modulecheck.gradle.platforms.android.UnsafeDirectAgpApiReference
import modulecheck.model.dependency.SourceSets
import modulecheck.model.dependency.isTestingOnly
import modulecheck.model.sourceset.SourceSetName
import modulecheck.model.sourceset.asSourceSetName
import modulecheck.parsing.gradle.model.GradleConfiguration
import modulecheck.parsing.gradle.model.GradleProject
import modulecheck.parsing.source.PackageName
import modulecheck.parsing.source.PackageName.Companion.asPackageName
import modulecheck.utils.mapToSet
import java.io.File

fun FileTreeWalk.files(): Sequence<File> = asSequence().filter { it.isFile }

/**
 * @param agpApiAccess the [AgpApiAccess] to use for safe access
 * @return A map of the [SourceSetName] to manifest [File] if
 *   the AGP plugin is applied, or null if AGP isn't applied
 * @since 0.12.0
 */
@UnsafeDirectAgpApiReference
fun GradleProject.androidManifests(agpApiAccess: AgpApiAccess): Map<SourceSetName, File>? =
  agpApiAccess.ifSafeOrNull(this) {
    requireBaseExtension()
      .sourceSets
      .associate { it.name.asSourceSetName() to it.manifest.srcFile }
  }

/**
 * @param agpApiAccess the [AgpApiAccess] to use for safe access
 * @param mcSourceSets the [SourceSets] from this project, used to look up hierarchies
 * @return A map of the [SourceSetName] to base package names if the
 *   [namespace][com.android.build.api.dsl.CommonExtension.namespace] is defined
 * @since 0.12.0
 */
@UnsafeDirectAgpApiReference
fun GradleProject.androidNamespaces(
  agpApiAccess: AgpApiAccess,
  mcSourceSets: SourceSets
): Map<SourceSetName, PackageName>? = agpApiAccess.ifSafeOrNull(this) {

  val baseExtension = requireBaseExtension()

  val namespace = baseExtension.namespace?.asPackageName()
  val testNameSpaceOrNull =
    (baseExtension as? AndroidTestedExtension)?.testNamespace?.asPackageName()

  baseExtension.sourceSets
    .mapNotNull { androidSourceSet ->

      val name = androidSourceSet.name.asSourceSetName()

      val isTestingSourceSet = name.isTestingOnly(mcSourceSets)

      val thisNamespace = testNameSpaceOrNull.takeIf { isTestingSourceSet }
        ?: namespace
        ?: return@mapNotNull null

      name to thisNamespace
    }
    .toMap()
}

/**
 * @param agpApiAccess the [AgpApiAccess] to use for safe access
 * @return the main src `AndroidManifest.xml` file if it exists. This will
 *   typically be `$projectDir/src/main/AndroidManifest.xml`, but if the position
 *   has been changed in the Android extension, the new path will be used.
 * @since 0.12.0
 */
fun GradleProject.mainAndroidManifest(agpApiAccess: AgpApiAccess): File? {

  return agpApiAccess.ifSafeOrNull(this) {
    @Suppress("UnstableApiUsage")
    requireCommonExtension().sourceSets
      .findByName("main")
      ?.manifest
      ?.let { it as? com.android.build.gradle.internal.api.DefaultAndroidSourceFile }
      ?.srcFile
  }
}

/**
 * @param agpApiAccess the [AgpApiAccess] to use for safe access
 * @return the main src `AndroidManifest.xml` file if it exists. This will
 *   typically be `$projectDir/src/main/AndroidManifest.xml`, but if the position
 *   has been changed in the Android extension, the new path will be used.
 * @since 0.12.0
 */
fun GradleProject.onAndroidPlugin(agpApiAccess: AgpApiAccess, action: BasePlugin.() -> Unit) {

  agpApiAccess.whenSafe(this) {

    plugins.withType(BasePlugin::class.java) { plugin ->
      action(plugin)
    }
  }
}

/**
 * Invokes [configAction] for the compiled configurations (`api`, `implementation`,
 * `compileOnly`, `runtimeOnly`) of *each build variant* once AGP has finalized the list.
 *
 * Immediately returns `null` if the receiver project does not have AGP applied.
 *
 * @param agpApiAccess ensures that AGP is in the classpath and applied before using its apis
 * @param configAction invoked *for each build variant* with the set of compiled configurations
 * @return [Unit] if the project had AGP and registered the
 *   callback, or `null` if the project does not have AGP
 */
@Suppress("UnstableApiUsage")
inline fun GradleProject.onAndroidCompileConfigurationsOrNull(
  agpApiAccess: AgpApiAccess,
  crossinline configAction: (SourceSetName, Set<GradleConfiguration>) -> Unit
): Unit? {
  return agpApiAccess.ifSafeOrNull(this@onAndroidCompileConfigurationsOrNull) {

    val commonExtension = requireCommonExtension()

    val componentsExtension = extensions.getByType(AndroidComponentsExtension::class.java)

    componentsExtension.onVariants { variant ->

      val sourceSet = commonExtension.sourceSets
        .getByName(variant.name)

      val configs = sequenceOf(
        sourceSet.apiConfigurationName,
        sourceSet.implementationConfigurationName,
        sourceSet.compileOnlyConfigurationName,
        sourceSet.runtimeOnlyConfigurationName
      )
        .mapToSet { configName -> configurations.getByName(configName) }

      configAction(sourceSet.name.asSourceSetName(), configs)
    }
  }
}

/**
 * @param agpApiAccess the [AgpApiAccess] to use for safe access
 * @return true if the project is an Android project and no manifest
 *   file exists at the location defined in the Android extension
 * @since 0.12.0
 */
fun GradleProject.isMissingManifestFile(agpApiAccess: AgpApiAccess): Boolean {

  return mainAndroidManifest(agpApiAccess)
    // the file must be declared, but not exist in order for this to be triggered
    ?.let { !it.exists() }
    ?: false
}

/**
 * @param agpApiAccess the [AgpApiAccess] to use for safe access
 * @return true if the project is an Android library, dynamic feature, or test
 *   extensions module and BuildConfig generation has NOT been explicitly disabled.
 * @since 0.12.0
 */
fun GradleProject.generatesBuildConfig(agpApiAccess: AgpApiAccess): Boolean {

  return agpApiAccess.ifSafeOrNull(this) {

    @Suppress("UnstableApiUsage")
    requireCommonExtension().buildFeatures.buildConfig != false
  }
    ?.orPropertyDefault(
      gradleProject = this,
      key = "android.defaults.buildfeatures.buildconfig",
      defaultValue = true
    )
    ?: false
}

fun Boolean?.orPropertyDefault(
  gradleProject: GradleProject,
  key: String,
  defaultValue: Boolean
): Boolean {
  if (this != null) return this
  return gradleProject.findProperty(key) as? Boolean ?: defaultValue
}
