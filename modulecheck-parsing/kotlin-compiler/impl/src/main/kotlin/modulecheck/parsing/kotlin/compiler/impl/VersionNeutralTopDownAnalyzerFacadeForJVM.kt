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

package modulecheck.parsing.kotlin.compiler.impl

import org.jetbrains.kotlin.analyzer.AnalysisResult
import org.jetbrains.kotlin.cli.jvm.compiler.TopDownAnalyzerFacadeForJVM
import org.jetbrains.kotlin.com.intellij.openapi.project.Project
import org.jetbrains.kotlin.com.intellij.psi.search.GlobalSearchScope
import org.jetbrains.kotlin.config.CompilerConfiguration
import org.jetbrains.kotlin.container.get
import org.jetbrains.kotlin.context.ModuleContext
import org.jetbrains.kotlin.descriptors.ModuleDescriptor
import org.jetbrains.kotlin.descriptors.impl.ModuleDescriptorImpl
import org.jetbrains.kotlin.library.KotlinLibrary
import org.jetbrains.kotlin.load.java.JavaClassesTracker
import org.jetbrains.kotlin.load.kotlin.PackagePartProvider
import org.jetbrains.kotlin.psi.KtFile
import org.jetbrains.kotlin.resolve.BindingTrace
import org.jetbrains.kotlin.resolve.CompilerEnvironment
import org.jetbrains.kotlin.resolve.LazyTopDownAnalyzer
import org.jetbrains.kotlin.resolve.TargetEnvironment
import org.jetbrains.kotlin.resolve.TopDownAnalysisMode
import org.jetbrains.kotlin.resolve.jvm.extensions.AnalysisHandlerExtension
import org.jetbrains.kotlin.resolve.lazy.declarations.DeclarationProviderFactory
import org.jetbrains.kotlin.resolve.lazy.declarations.FileBasedDeclarationProviderFactory
import org.jetbrains.kotlin.storage.StorageManager

/**
 * This is a compatibility shim for invoking Kotlin 1.6.2x compiler analysis using the 1.6.10
 * classes. The `explicitCompilerEnvironment: TargetEnvironment = CompilerEnvironment` argument with
 * default was added in 1.6.20, and causes a `NotSuchMethodError` exception if parsing a 1.6.2x
 * project with 1.6.10 source.
 */
object VersionNeutralTopDownAnalyzerFacadeForJVM {
  /**
   * Performs the full analysis of this source set/configuration, returning the [AnalysisResult]
   * so that we can use the [BindingContext][org.jetbrains.kotlin.resolve.BindingContext] for type
   * resolution.
   *
   * Note that this process is eager, and can be very time-consuming for large
   * projects or projects with lots of internal dependencies. It's only a bit
   * faster than doing a normal compilation. This function is called when the lazy
   * [KotlinEnvironment.bindingContext][modulecheck.parsing.kotlin.compiler.KotlinEnvironment.bindingContext]
   * is accessed.
   */
  @Suppress("LongParameterList")
  fun analyzeFilesWithJavaIntegration(
    project: Project,
    files: Collection<KtFile>,
    trace: BindingTrace,
    configuration: CompilerConfiguration,
    packagePartProvider: (GlobalSearchScope) -> PackagePartProvider,
    declarationProviderFactory:
      (StorageManager, Collection<KtFile>) -> DeclarationProviderFactory = ::FileBasedDeclarationProviderFactory,
    sourceModuleSearchScope: GlobalSearchScope = TopDownAnalyzerFacadeForJVM.newModuleSearchScope(
      project,
      files
    ),
    klibList: List<KotlinLibrary> = emptyList(),
    explicitModuleDependencyList: List<ModuleDescriptorImpl> = emptyList(),
    explicitModuleFriendsList: List<ModuleDescriptorImpl> = emptyList(),
    explicitCompilerEnvironment: TargetEnvironment = CompilerEnvironment
  ): AnalysisResult {
    val container = TopDownAnalyzerFacadeForJVM.createContainer(
      project,
      files,
      trace,
      configuration,
      packagePartProvider,
      declarationProviderFactory,
      explicitCompilerEnvironment,
      sourceModuleSearchScope,
      klibList,
      explicitModuleDependencyList = explicitModuleDependencyList,
      explicitModuleFriendsList = explicitModuleFriendsList
    )

    val module = container.get<ModuleDescriptor>()
    val moduleContext = container.get<ModuleContext>()

    val analysisHandlerExtensions = AnalysisHandlerExtension.getInstances(project)

    fun invokeExtensionsOnAnalysisComplete(): AnalysisResult? {
      container.get<JavaClassesTracker>().onCompletedAnalysis(module)
      for (extension in analysisHandlerExtensions) {
        val result = extension.analysisCompleted(project, module, trace, files)
        if (result != null) return result
      }

      return null
    }

    for (extension in analysisHandlerExtensions) {
      val result = extension.doAnalysis(project, module, moduleContext, files, trace, container)
      if (result != null) {
        invokeExtensionsOnAnalysisComplete()?.let { return it }
        return result
      }
    }

    container.get<LazyTopDownAnalyzer>()
      .analyzeDeclarations(TopDownAnalysisMode.TopLevelDeclarations, files)

    invokeExtensionsOnAnalysisComplete()?.let { return it }

    return AnalysisResult.success(trace.bindingContext, module)
  }
}
