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

package modulecheck.builds

import modulecheck.builds.artifacts.ArtifactsPlugin
import modulecheck.builds.ktlint.KtLintConventionPlugin
import modulecheck.builds.matrix.VersionsMatrixYamlPlugin
import org.gradle.api.Plugin
import org.gradle.api.Project

abstract class McBuildPlugin : Plugin<Project> {
  override fun apply(target: Project) {

    target.plugins.apply(CleanPlugin::class.java)
    target.plugins.apply(DependencyGuardConventionPlugin::class.java)
    target.plugins.apply(DetektConventionPlugin::class.java)
    target.plugins.apply(DokkaConventionPlugin::class.java)
    target.plugins.apply(JavaLibraryConventionPlugin::class.java)
    target.plugins.apply(KotlinJvmConventionPlugin::class.java)
    target.plugins.apply(KtLintConventionPlugin::class.java)
    target.plugins.apply(TestConventionPlugin::class.java)

    target.extensions.create("mcbuild", ModuleCheckBuildExtension::class.java)
  }
}

abstract class McBuildRootPlugin : Plugin<Project> {
  override fun apply(target: Project) {

    target.checkProjectIsRoot()

    target.plugins.apply(McBuildPlugin::class.java)

    target.plugins.apply(ArtifactsPlugin::class.java)
    target.plugins.apply(BenManesVersionsPlugin::class.java)
    target.plugins.apply(DependencyGuardAggregatePlugin::class.java)
    target.plugins.apply(KnitConventionPlugin::class.java)
    target.plugins.apply(VersionsMatrixYamlPlugin::class.java)
    target.plugins.apply(WebsitePlugin::class.java)
  }
}
