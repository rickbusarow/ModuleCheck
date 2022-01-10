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

package modulecheck.parsing.psi

import org.jetbrains.kotlin.cli.common.CLIConfigurationKeys
import org.jetbrains.kotlin.cli.common.environment.setIdeaIoUseFallback
import org.jetbrains.kotlin.cli.common.messages.AnalyzerWithCompilerReport
import org.jetbrains.kotlin.cli.common.messages.CompilerMessageSeverity
import org.jetbrains.kotlin.cli.common.messages.CompilerMessageSourceLocation
import org.jetbrains.kotlin.cli.common.messages.MessageRenderer
import org.jetbrains.kotlin.cli.common.messages.PlainTextMessageRenderer
import org.jetbrains.kotlin.cli.common.messages.PrintingMessageCollector
import org.jetbrains.kotlin.cli.jvm.compiler.EnvironmentConfigFiles
import org.jetbrains.kotlin.cli.jvm.compiler.KotlinCoreEnvironment
import org.jetbrains.kotlin.cli.jvm.compiler.NoScopeRecordCliBindingTrace
import org.jetbrains.kotlin.cli.jvm.compiler.TopDownAnalyzerFacadeForJVM
import org.jetbrains.kotlin.com.intellij.mock.MockProject
import org.jetbrains.kotlin.com.intellij.openapi.Disposable
import org.jetbrains.kotlin.com.intellij.openapi.util.Disposer
import org.jetbrains.kotlin.com.intellij.openapi.util.UserDataHolderBase
import org.jetbrains.kotlin.com.intellij.pom.PomModel
import org.jetbrains.kotlin.com.intellij.pom.PomModelAspect
import org.jetbrains.kotlin.com.intellij.pom.PomTransaction
import org.jetbrains.kotlin.com.intellij.pom.impl.PomTransactionBase
import org.jetbrains.kotlin.com.intellij.pom.tree.TreeAspect
import org.jetbrains.kotlin.config.CommonConfigurationKeys
import org.jetbrains.kotlin.config.CompilerConfiguration
import org.jetbrains.kotlin.config.languageVersionSettings
import org.jetbrains.kotlin.psi.KtFile
import org.jetbrains.kotlin.resolve.BindingContext
import org.jetbrains.kotlin.resolve.lazy.declarations.FileBasedDeclarationProviderFactory
import sun.reflect.ReflectionFactory

fun createBindingContext(
  classpath: List<String>,
  files: List<KtFile>
): BindingContext {
  if (classpath.isEmpty()) {
    return BindingContext.EMPTY
  }

  val environment: KotlinCoreEnvironment = createKotlinCoreEnvironment()

  val analyzer = AnalyzerWithCompilerReport(
    PrintingMessageCollector(System.err, ModuleCheckMessageRenderer, true),
    environment.configuration.languageVersionSettings
  )
  analyzer.analyzeAndReport(files) {
    TopDownAnalyzerFacadeForJVM.analyzeFilesWithJavaIntegration(
      project = environment.project,
      files = files,
      trace = NoScopeRecordCliBindingTrace(),
      configuration = environment.configuration,
      packagePartProvider = environment::createPackagePartProvider,
      declarationProviderFactory = ::FileBasedDeclarationProviderFactory
    )
  }
  return analyzer.analysisResult.bindingContext
}

/*
Borrowed from Detekt
https://github.com/detekt/detekt/blob/master/detekt-parser/src/main/kotlin/io/github/detekt/parser/KotlinEnvironmentUtils.kt
 */
fun createKotlinCoreEnvironment(
  configuration: CompilerConfiguration = CompilerConfiguration(),
  disposable: Disposable = Disposer.newDisposable()
): KotlinCoreEnvironment {
  // https://github.com/JetBrains/kotlin/commit/2568804eaa2c8f6b10b735777218c81af62919c1
  setIdeaIoUseFallback()
  configuration.put(
    CLIConfigurationKeys.MESSAGE_COLLECTOR_KEY,
    PrintingMessageCollector(System.err, MessageRenderer.PLAIN_FULL_PATHS, false)
  )
  configuration.put(CommonConfigurationKeys.MODULE_NAME, "detekt")

  val environment = KotlinCoreEnvironment.createForProduction(
    disposable,
    configuration,
    EnvironmentConfigFiles.JVM_CONFIG_FILES
  )

  val projectCandidate = environment.project

  val project = requireNotNull(projectCandidate as? MockProject) {
    "MockProject type expected, actual - ${projectCandidate.javaClass.simpleName}"
  }

  project.registerService(PomModel::class.java, ModuleCheckPomModel())

  return environment
}

/**
 * https://github.com/pinterest/ktlint/blob/69cc0f7f826e18d7ec20e7a0f05df12d53a3c1e1/ktlint-core/src/main/kotlin/com/pinterest/ktlint/core/internal/KotlinPsiFileFactory.kt#L70
 */
private class ModuleCheckPomModel : UserDataHolderBase(), PomModel {

  override fun runTransaction(transaction: PomTransaction) {
    val transactionCandidate = transaction as? PomTransactionBase

    val pomTransaction = requireNotNull(transactionCandidate) {
      "expected ${PomTransactionBase::class.simpleName} but actual was ${transaction.javaClass.simpleName}"
    }

    pomTransaction.run()
  }

  override fun <T : PomModelAspect?> getModelAspect(aspect: Class<T>): T? {
    if (aspect == TreeAspect::class.java) {
      // using approach described in https://git.io/vKQTo due to the magical bytecode of TreeAspect
      // (check constructor signature and compare it to the source)
      // (org.jetbrains.kotlin:kotlin-compiler-embeddable:1.0.3)
      val constructor = ReflectionFactory.getReflectionFactory()
        .newConstructorForSerialization(aspect, Any::class.java.getDeclaredConstructor())
      @Suppress("UNCHECKED_CAST")
      return constructor.newInstance() as T
    }
    return null
  }
}

private object ModuleCheckMessageRenderer : PlainTextMessageRenderer() {
  override fun getName() = "ModuleCheck message renderer"
  override fun getPath(location: CompilerMessageSourceLocation) = location.path
  override fun render(
    severity: CompilerMessageSeverity,
    message: String,
    location: CompilerMessageSourceLocation?
  ): String {
    if (!severity.isError) return ""
    return super.render(severity, message, location)
  }
}
