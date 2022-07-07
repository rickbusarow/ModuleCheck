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

package modulecheck.parsing.kotlin.compiler

import modulecheck.parsing.gradle.model.ProjectPath
import modulecheck.parsing.gradle.model.SourceSetName
import org.jetbrains.kotlin.cli.jvm.compiler.KotlinCoreEnvironment
import org.jetbrains.kotlin.com.intellij.psi.PsiJavaFile
import org.jetbrains.kotlin.config.CompilerConfiguration
import org.jetbrains.kotlin.psi.KtFile
import org.jetbrains.kotlin.resolve.BindingContext
import java.io.File

/**
 * Models everything needed in order to creat authentic Psi files for [BindingContext]-backed type
 * resolution.
 */
interface KotlinEnvironment {
  /** Used to create Psi files for Kotlin and Java */
  val psiFileFactory: McPsiFileFactory

  /**
   * wrapper around "core" settings like Kotlin version, source files, and classpath files (external
   * dependencies)
   */
  val coreEnvironment: KotlinCoreEnvironment

  /**
   * The result of file analysis, used for last-resort type resolution. This object is very
   * expensive to create, but it's created lazily.
   */
  val bindingContext: BindingContext

  /**
   * "core" settings like Kotlin version, source files, and classpath files (external dependencies)
   */
  val compilerConfiguration: CompilerConfiguration

  /**
   * The cache of Kotlin Psi files created by [psiFileFactory]. Note that these are re-used in
   * dependency modules, and much of their implementation is Lazy, so re-use is important.
   */
  val ktFiles: Map<File, KtFile>

  /**
   * The cache of Java Psi files created by [psiFileFactory]. Note that these are re-used in
   * dependency modules, and much of their implementation is Lazy, so re-use is important.
   */
  val javaFiles: Map<File, PsiJavaFile>

  @Suppress("MaxLineLength")
  /**
   * Model for additional sources coming from upstream "dependencies". These files can come from:
   * - an upstream source set within the same project
   * - an internal project dependency, like `implementation(project(":lib1"))`
   * - an external dependency, like `implementation("com.google.dagger:dagger")`
   *
   * ```
   * ┌──────────────────────┐
   * │:lib2                 │
   * │                      │
   * │  ┌────────────────┐  │
   * │  │    src/test    │  │
   * │  └────────────────┘  │
   * │           │          │
   * │           └────────────────────────────────┐
   * │                      │      ┌──────────────│───────┐
   * │  ┌────────────────┐  │      │:lib1         ▼       │
   * │  │    src/main    │  │      │  ┌────────────────┐  │
   * │  └────────────────┘  │    ─ ─ ▶│    src/main    │  │
   * │           ▲          │   │  │  └────────────────┘  │
   * │                      │      │                      │
   * └───────────│──────────┘   │  └──────────────────────┘
   *
   *             │              │
   *    ┌ ─ ─ ─ ─ ─ ─ ─ ─ ┐
   *                            │
   *    │InheritedSources │─ ─ ─
   *
   *    └ ─ ─ ─ ─ ─ ─ ─ ─ ┘
   * ```
   *
   * @property ktFiles all Psi "Kt" files. These should be the same instances as those used in the
   *   upstream source sets. Note that this can only be Kotlin Psi files, per the signature of
   *   [TopDownAnalyzerFacadeForJvm.analyzeFilesWithJavaIntegration][org.jetbrains.kotlin.cli.jvm.compiler.TopDownAnalyzerFacadeForJVM.analyzeFilesWithJavaIntegration]
   * @property jvmFiles all source code files, whether they're `.java` or `.kt`.
   * @property classpathFiles `.jar` files from external dependencies
   */
  data class InheritedSources(
    val ktFiles: Set<KtFile>,
    val jvmFiles: Set<File>,
    val classpathFiles: Set<File>
  )

  /** Creates an instance of [KotlinEnvironment] */
  fun interface Factory {
    /**
     * @param projectPath the path of the project for which this environment is being modeled
     * @param sourceSetName the name of the source set for which this environment is being modeled
     */
    suspend fun create(
      projectPath: ProjectPath,
      sourceSetName: SourceSetName
    ): KotlinEnvironment
  }
}
