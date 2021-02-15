/*
 * Copyright (C) 2021 Rick Busarow
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

package modulecheck.gradle.internal

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
