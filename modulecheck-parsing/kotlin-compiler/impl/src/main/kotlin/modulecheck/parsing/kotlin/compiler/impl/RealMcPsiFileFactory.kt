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

import com.squareup.anvil.annotations.ContributesBinding
import modulecheck.dagger.TaskScope
import modulecheck.parsing.kotlin.compiler.KotlinEnvironment
import modulecheck.parsing.kotlin.compiler.McPsiFileFactory
import modulecheck.parsing.kotlin.compiler.internal.AbstractMcPsiFileFactory
import org.jetbrains.kotlin.com.intellij.openapi.project.Project
import org.jetbrains.kotlin.com.intellij.openapi.vfs.StandardFileSystems
import org.jetbrains.kotlin.com.intellij.openapi.vfs.VirtualFileManager
import org.jetbrains.kotlin.com.intellij.openapi.vfs.VirtualFileSystem
import org.jetbrains.kotlin.com.intellij.psi.PsiFile
import org.jetbrains.kotlin.com.intellij.psi.PsiManager
import java.io.File
import java.io.FileNotFoundException
import javax.inject.Inject

/**
 * A real implementation of [McPsiFileFactory] using a curated [KotlinEnvironment].
 *
 * The files created from this factory are backed by a meaningful
 * [BindingContext][org.jetbrains.kotlin.resolve.BindingContext] which is aware of the full
 * classpath and may be used for type resolution.
 *
 * @since 0.13.0
 */
class RealMcPsiFileFactory(
  private val kotlinEnvironment: KotlinEnvironment
) : AbstractMcPsiFileFactory(),
  McPsiFileFactory {

  override val coreEnvironment by lazy { kotlinEnvironment.coreEnvironment }

  private val psiProject: Project by lazy { coreEnvironment.project }
  private val psiManager: PsiManager by lazy { PsiManager.getInstance(psiProject) }
  private val virtualFileSystem: VirtualFileSystem by lazy {
    // make sure that the PsiManager has initialized, or we'll get NPE's when trying to initialize
    // the VirtualFileManager instance
    psiManager
    VirtualFileManager.getInstance()
      .getFileSystem(StandardFileSystems.FILE_PROTOCOL)
  }

  override fun create(file: File): PsiFile {
    if (!file.exists()) throw FileNotFoundException("could not find file $file")

    val vFile = virtualFileSystem.findFileByPath(file.absolutePath)
      ?: throw FileNotFoundException("could not find file $file")

    val psi = psiManager.findFile(vFile)

    return when (file.extension) {
      "java" -> psi as PsiFile
      "kt", "kts" -> psi as PsiFile
      else -> throw IllegalArgumentException(
        "file extension must be one of [java, kt, kts], but it was `${file.extension}`."
      )
    }
  }

  /**
   * Creates an instance of [McPsiFileFactory]
   *
   * @since 0.13.0
   */
  @ContributesBinding(TaskScope::class)
  class Factory @Inject constructor() : McPsiFileFactory.Factory {
    override fun create(
      kotlinEnvironment: KotlinEnvironment
    ): RealMcPsiFileFactory = RealMcPsiFileFactory(kotlinEnvironment)
  }
}
