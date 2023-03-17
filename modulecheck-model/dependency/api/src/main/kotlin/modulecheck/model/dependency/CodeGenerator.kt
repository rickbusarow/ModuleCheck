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

package modulecheck.model.dependency

/**
 * Models a special kind of dependency, where the symbols which determine whether it's "used" are
 * actually provided by a different artifact.
 *
 * For instance, an annotation processor `com.example.foo:foo-generator` does not have any
 * declarations which show up in a library's source code. Instead, it looks for annotations from
 * `com.example.foo:foo-annotations` in order to trigger code generation. So, in order for us to
 * determine whether `foo-generator` is used, we must look for those annotations. If there are no
 * annotations, then `foo-generator` isn't triggered and could probably be removed.
 *
 * N.B. Code generators often evolve over time, adding new annotations. So if a defined generator is
 * throwing a false positive saying it's unused, it's best to first check the list of
 * [annotationNames] to make sure it's exhaustive.
 *
 * @since 0.13.0
 */
interface CodeGenerator {
  /**
   * The human-readable name for this type of extension, like 'annotation processor' or 'KSP
   * extension'
   *
   * @since 0.12.0
   */
  val extensionTypeName: String

  /**
   * The configuration name(s) used when adding this extension to the "main" source set's
   * compilation, such as `kapt`, `annotationProcessor`, `ksp`, or `anvil`. All other configuration
   * names for downstream source sets are derived from these base names, like `kapt` -> `kaptTest`.
   *
   * This should almost always just be a single name. It's a List because annotations processors may
   * be `kapt` or `annotationProcessor` depending upon whether they're applied to a Kotlin module or
   * a pure Java one.
   *
   * @since 0.12.0
   */
  val baseConfigNames: List<ConfigurationName>

  /**
   * A human-readable, descriptive name for this specific compiler extension, such as 'Hilt Android'
   * or 'Moshi Kotlin codegen (KSP)'.
   *
   * This name doesn't strictly need to be unique, but that's probably a good idea. For instance,
   * instead of two extensions named 'Room', we have 'Room (annotation processor)' and 'Room (KSP)'.
   *
   * @since 0.12.0
   */
  val name: String

  /**
   * This is the .jar for the compiler library, not the Gradle plugin. For instance,
   * `androidx.room:room-compiler` or `com.squareup.anvil:compiler`.
   *
   * For KSP, annotation processors, and Anvil, the extensions are typically added in the build file
   * like `kapt("com.example.foo:compiler:1.2.3")`. For a Kotlin compiler plugin, this will probably
   * be an "invisible" dependency added via the library's Gradle plugin.
   *
   * @since 0.12.0
   */
  val generatorMavenCoordinates: String

  /**
   * The *fully qualified* names of all annotations which make this compiler extension do something
   * -- typically code generation or perhaps static analysis.
   *
   * For instance, `androidx.room.Database` triggers compilation for the Room compiler, so it would
   * be included in this list.
   *
   * The annotations don't necessarily need to come from the same library, so long as the compiler
   * extension looks for them. For instance, `javax.Inject` is included in the list of annotations
   * for both Anvil and Dagger.
   *
   * On the other hand, an annotation from the library does not need to be listed just because it's
   * an annotation. An example of this would be `tangle.inject.InternalTangleApi`, which is not used
   * by the compiler.
   *
   * @since 0.12.0
   */
  val annotationNames: List<String>
}

fun List<CodeGenerator>.asMap(): Map<String, CodeGenerator> =
  associateBy { it.generatorMavenCoordinates }

/**
 * Indicates that some type (probably a
 * [ConfiguredDependency][modulecheck.parsing.gradle.model.ConfiguredDependency]) is associated with
 * an established [CodeGenerator].
 *
 * @since 0.12.0
 */
interface MightHaveCodeGeneratorBinding {
  /**
   * The [CodeGenerator] if it is defined, or null.
   *
   * @since 0.12.0
   */
  val codeGeneratorBindingOrNull: CodeGenerator?
}
