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

import com.github.gmazzo.gradle.plugins.BuildConfigExtension
import com.github.gmazzo.gradle.plugins.BuildConfigPlugin
import org.gradle.api.Project
import org.gradle.api.file.DirectoryProperty
import org.gradle.api.provider.Property
import org.gradle.api.provider.Provider
import java.io.File
import kotlin.LazyThreadSafetyMode.NONE
import kotlin.reflect.KClass

interface BuildPropertiesExtension {

  fun Project.buildConfig(
    sourceSetName: String = "main",
    internalVisibility: Boolean = true,
    config: BuildConfigBuilderScope.() -> Unit
  ) {

    plugins.apply(BuildConfigPlugin::class.java)

    BuildConfigBuilderScope(
      project = this,
      sourceSetName = sourceSetName,
      internalVisibility = internalVisibility
    ).config()
  }
}

@Suppress("MemberVisibilityCanBePrivate")
class BuildConfigBuilderScope(
  private val project: Project,
  private val sourceSetName: String,
  private val internalVisibility: Boolean
) {

  // Shadow the common project properties eagerly,
  // so that the project instance isn't accidentally referenced during task runtime.
  val rootDir: File = project.rootDir
  val projectDir: File = project.projectDir

  val normalSourceRoot: DirectoryProperty = project.objects.directoryProperty()
    .convention(
      project.layout.dir(
        project.provider {
          projectDir.resolve("src/$sourceSetName/kotlin")
        }
      )
    )

  val packageName: Property<String> = project.objects.property(String::class.java)
    .convention(
      normalSourceRoot.map { dir ->

        val root = dir.asFile
        val minDir = root
          .walkTopDown()
          .filter { it.isDirectoryWithFiles { file -> file.extension == "kt" } }
          .minByOrNull { it.path.length }
          ?.relativeTo(root)

        checkNotNull(minDir) {
          "Could not find a kotlin file in the source directory of: file://$normalSourceRoot"
        }

        minDir.path.split(File.separator).joinToString(".")
      }
    )

  private val sourceSet by lazy(NONE) {
    project.extensions
      .getByType(BuildConfigExtension::class.java)
      .sourceSets
      .maybeRegister(sourceSetName) { buildConfigSourceSet ->

        buildConfigSourceSet.className.convention("BuildProperties")
        buildConfigSourceSet.packageName.convention(packageName)

        buildConfigSourceSet.useKotlinOutput { generator ->
          if (internalVisibility) {
            generator.internalVisibility = true
          }
        }

        project.registerSimpleGenerationTaskAsDependency(
          sourceSetName,
          buildConfigSourceSet.generateTask
        )
      }
  }

  fun <T : Any> field(name: String, value: T) {

    sourceSet.configure { buildConfigSourceSet ->
      val valueString = getValueString(value)

      buildConfigSourceSet.buildConfigField(value::class.java.simpleName, name, valueString)
    }
  }

  fun field(name: String, valueProvider: () -> String) {
    field(name, String::class, project.provider { valueProvider() })
  }

  fun field(name: String, valueProvider: Provider<String>) {
    field(name, String::class, valueProvider)
  }

  fun <T : Any> field(name: String, type: KClass<out Any>, valueProvider: () -> T) {
    field(name, type, project.provider { valueProvider() })
  }

  fun <T : Any> field(name: String, type: KClass<out Any>, valueProvider: Provider<T>) {

    sourceSet.configure { buildConfigSourceSet ->
      buildConfigSourceSet.buildConfigField(
        type = type.qualifiedName!!,
        name = name,
        value = valueProvider.map { getValueString(it) }
      )
    }
  }

  @PublishedApi
  internal fun <T : Any> getValueString(value: T) = when (value) {
    is String -> {
      check(!value.startsWith("\\\"") && !value.endsWith("\\\"")) {
        "Don't add escaped quotes manually.  The Kable extension will do that automagically.\n" +
          "The `value` argument was (underscores added): _${value}_"
      }

      "\"$value\""
    }

    // For file paths, turn '/Users/<username>/Development/kable/extras/some-folder'
    //                 into '../../extras/some-folder'
    is File -> "\"${value.relativeTo(projectDir)}\""
    is Number -> "$value"
    else -> "\"$value\""
  }
}
