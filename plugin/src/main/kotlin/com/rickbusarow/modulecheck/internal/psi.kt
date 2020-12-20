package com.rickbusarow.modulecheck.internal

import org.jetbrains.kotlin.cli.common.CLIConfigurationKeys
import org.jetbrains.kotlin.cli.common.messages.MessageRenderer
import org.jetbrains.kotlin.cli.common.messages.PrintingMessageCollector
import org.jetbrains.kotlin.cli.jvm.compiler.EnvironmentConfigFiles
import org.jetbrains.kotlin.cli.jvm.compiler.KotlinCoreEnvironment
import org.jetbrains.kotlin.com.intellij.openapi.util.Disposer
import org.jetbrains.kotlin.com.intellij.psi.PsiFileFactory
import org.jetbrains.kotlin.config.CompilerConfiguration
import org.jetbrains.kotlin.psi.KtPsiFactory

val configuration = CompilerConfiguration().apply {
  put(
    CLIConfigurationKeys.MESSAGE_COLLECTOR_KEY,
    PrintingMessageCollector(
      System.err,
      MessageRenderer.PLAIN_FULL_PATHS,
      false
    )
  )
}

private val psiProject by lazy {
  KotlinCoreEnvironment.createForProduction(
    Disposer.newDisposable(),
    configuration,
    EnvironmentConfigFiles.JVM_CONFIG_FILES
  ).project
}

val psiFileFactory: PsiFileFactory = PsiFileFactory.getInstance(psiProject)
val psiElementFactory = KtPsiFactory(psiProject, false)
