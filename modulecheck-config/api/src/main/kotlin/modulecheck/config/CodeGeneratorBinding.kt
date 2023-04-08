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

package modulecheck.config

import modulecheck.model.dependency.CodeGenerator
import modulecheck.model.dependency.ConfigurationName
import modulecheck.model.dependency.PluginDefinition

/**
 * Models a special kind of dependency, where the symbols which determine
 * whether it's "used" are actually provided by a different artifact.
 *
 * For instance, an annotation processor `com.example.foo:foo-generator` does not have any
 * declarations which show up in a library's source code. Instead, it looks for annotations
 * from `com.example.foo:foo-annotations` in order to trigger code generation. So, in order
 * for us to determine whether `foo-generator` is used, we must look for those annotations. If
 * there are no annotations, then `foo-generator` isn't triggered and could probably be removed.
 *
 * N.B. Code generators often evolve over time, adding new annotations. So if a
 * defined generator is throwing a false positive saying it's unused, it's best
 * to first check the list of [annotationNames] to make sure it's exhaustive.
 *
 * @since 0.12.0
 */
// TODO (maybe) - 99.9% of code generators will be triggered by annotations, but technically they
//   don't have to be.  Code generators can be triggered by any symbol, such as keywords like `data`
//   or `suspend`.  Maybe expand this class to support other predicates, like `(File) -> Boolean`
//   and/or a keyword matcher.
sealed class CodeGeneratorBinding(
  /**
   * The human-readable name for this type of extension,
   * like 'annotation processor' or 'KSP extension'
   *
   * @since 0.12.0
   */
  override val extensionTypeName: String,
  /**
   * The configuration name(s) used when adding this extension to the "main" source set's
   * compilation, such as `kapt`, `annotationProcessor`, `ksp`, or `anvil`. All other configuration
   * names for downstream source sets are derived from these base names, like `kapt` -> `kaptTest`.
   *
   * This should almost always just be a single name. It's a List because
   * annotations processors may be `kapt` or `annotationProcessor` depending
   * upon whether they're applied to a Kotlin module or a pure Java one.
   *
   * @since 0.12.0
   */
  override val baseConfigNames: List<ConfigurationName>
) : CodeGenerator {

  /**
   * Represents an annotation processor like Dagger or Room, which could be used
   * via `kapt` in a Kotlin library or `annotationProcessor` in a pure Java library.
   *
   * For any processor which also has a KSP implementation,
   * that extension should just be listed twice.
   *
   * @since 0.12.0
   */
  data class AnnotationProcessor(
    override val name: String,
    override val generatorMavenCoordinates: String,
    override val annotationNames: List<String>
  ) : CodeGeneratorBinding(
    "annotation processor",
    listOf(ConfigurationName.kapt, ConfigurationName.annotationProcessor)
  )

  /**
   * Represents KSP extensions, like Moshi or Room.
   *
   * For any extension which also has annotation processor
   * implementation, that extension should just be listed twice.
   *
   * @since 0.12.0
   */
  data class KspExtension(
    override val name: String,
    override val generatorMavenCoordinates: String,
    override val annotationNames: List<String>
  ) : CodeGeneratorBinding(
    "KSP extension",
    listOf(ConfigurationName.ksp)
  )

  /**
   * Represents code generators which **extend** Anvil's
   * codegen functionality, but **not Anvil itself**.
   *
   * @see KotlinCompilerPlugin
   * @since 0.12.0
   */
  data class AnvilExtension(
    override val name: String,
    override val generatorMavenCoordinates: String,
    override val annotationNames: List<String>
  ) : CodeGeneratorBinding(
    "Anvil extension",
    listOf(ConfigurationName.anvil)
  )

  /**
   * Pure Kotlin compiler plugins, like Anvil.
   *
   * @since 0.12.0
   */
  data class KotlinCompilerPlugin(
    override val name: String,
    override val generatorMavenCoordinates: String,
    override val annotationNames: List<String>,
    val gradlePlugin: PluginDefinition
  ) : CodeGeneratorBinding(
    "Kotlin compiler plugin",
    listOf(ConfigurationName.kotlinCompileClasspath)
  )
}
