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

import modulecheck.utils.lazy.LazyDeferred
import org.jetbrains.kotlin.cli.jvm.compiler.KotlinCoreEnvironment
import org.jetbrains.kotlin.com.intellij.psi.PsiJavaFile
import org.jetbrains.kotlin.config.CompilerConfiguration
import org.jetbrains.kotlin.descriptors.impl.ModuleDescriptorImpl
import org.jetbrains.kotlin.psi.KtFile
import org.jetbrains.kotlin.resolve.BindingContext
import java.io.File

/**
 * Models everything needed in order to creat authentic Psi files for [BindingContext]-backed type
 * resolution.
 */
interface KotlinEnvironment : HasPsiAnalysis {
  /** Used to create Psi files for Kotlin and Java */
  val psiFileFactory: McPsiFileFactory

  /**
   * wrapper around "core" settings like Kotlin version, source files, and classpath files (external
   * dependencies)
   */
  val coreEnvironment: KotlinCoreEnvironment

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
}

/**
 * Holds the [BindingContext] and [ModuleDescriptorImpl] for a [KotlinEnvironment]. These are
 * retrieved from an [AnalysisResult][org.jetbrains.kotlin.analyzer.AnalysisResult].
 */
interface HasPsiAnalysis {

  /**
   * The result of file analysis, used for last-resort type resolution. This object is very
   * expensive to create, but it's created lazily.
   */
  val bindingContext: LazyDeferred<BindingContext>

  /**
   * The result of file analysis, used for last-resort type resolution. This object is very
   * expensive to create, but it's created lazily.
   *
   * N.B. This has to be an -Impl instead of just the `ModuleDescriptor` interface because
   * `TopDownAnalyzerFacadeForJVM.createContainer(...)` requires the -Impl type.
   */
  val moduleDescriptorDeferred: LazyDeferred<ModuleDescriptorImpl>
}
