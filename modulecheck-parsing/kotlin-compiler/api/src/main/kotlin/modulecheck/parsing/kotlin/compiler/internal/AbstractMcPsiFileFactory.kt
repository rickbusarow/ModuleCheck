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

package modulecheck.parsing.kotlin.compiler.internal

import modulecheck.parsing.kotlin.compiler.McPsiFileFactory
import org.intellij.lang.annotations.Language
import org.jetbrains.kotlin.cli.jvm.compiler.KotlinCoreEnvironment
import org.jetbrains.kotlin.com.intellij.lang.java.JavaLanguage
import org.jetbrains.kotlin.com.intellij.openapi.project.Project
import org.jetbrains.kotlin.com.intellij.openapi.util.text.StringUtilRt
import org.jetbrains.kotlin.com.intellij.openapi.vfs.StandardFileSystems
import org.jetbrains.kotlin.com.intellij.openapi.vfs.VirtualFileManager
import org.jetbrains.kotlin.com.intellij.openapi.vfs.VirtualFileSystem
import org.jetbrains.kotlin.com.intellij.psi.PsiFile
import org.jetbrains.kotlin.com.intellij.psi.PsiFileFactory
import org.jetbrains.kotlin.com.intellij.psi.PsiJavaFile
import org.jetbrains.kotlin.com.intellij.psi.PsiManager
import org.jetbrains.kotlin.incremental.isJavaFile
import org.jetbrains.kotlin.psi.KtFile
import org.jetbrains.kotlin.psi.KtPsiFactory
import java.io.File
import java.io.FileNotFoundException

/** Base class for an [McPsiFileFactory] implementation */
abstract class AbstractMcPsiFileFactory : McPsiFileFactory {

  /**
   * wrapper around "core" settings like Kotlin version, source files, and classpath files (external
   * dependencies)
   */
  abstract val coreEnvironment: KotlinCoreEnvironment

  private val psiProject: Project by lazy { coreEnvironment.project }
  private val psiManager: PsiManager by lazy { PsiManager.getInstance(psiProject) }
  private val virtualFileSystem: VirtualFileSystem by lazy {
    // make sure that the PsiManager has initialized, or we'll get NPE's when trying to initialize
    // the VirtualFileManager instance
    psiManager
    VirtualFileManager.getInstance()
      .getFileSystem(StandardFileSystems.FILE_PROTOCOL)
  }

  private val ktFileFactory: KtPsiFactory by lazy {
    KtPsiFactory(psiProject, markGenerated = false)
  }
  private val javaFileFactory: PsiFileFactory by lazy { PsiFileFactory.getInstance(psiProject) }

  override fun create(file: File): PsiFile {
    return when (file.extension) {
      ".java" -> createJava(file)
      ".kt", ".kts" -> createKotlin(file)
      else -> throw IllegalArgumentException(
        "file extension must be one of [.java, .kt, .kts], but it was `${file.extension}`."
      )
    }
  }

  override fun createKotlin(file: File): KtFile {
    if (!file.exists()) throw FileNotFoundException("could not find file $file")
    if (!file.isKotlinFile()) throw IllegalArgumentException(
      "the file's extension must be either `.kt` or `.kts`, but it was `${file.extension}`."
    )

    val vFile = virtualFileSystem.findFileByPath(file.path)
      ?: throw FileNotFoundException("could not find file $file")

    return psiManager.findFile(vFile) as KtFile
  }

  override fun createKotlin(
    name: String,
    @Language("kotlin")
    content: String
  ): KtFile {
    return ktFileFactory.createPhysicalFile(
      name,
      StringUtilRt.convertLineSeparators(content.trimIndent())
    )
  }

  override fun createJava(file: File): PsiJavaFile {
    if (!file.isJavaFile()) {
      throw IllegalArgumentException(
        "the file's extension must be `.java`, but it was `${file.extension}`."
      )
    }

    val vFile = virtualFileSystem.findFileByPath(file.absolutePath)!!

    val psi = psiManager.findFile(vFile)

    return psi as PsiJavaFile
  }

  override fun createJava(
    name: String,
    @Language("java")
    content: String
  ): PsiJavaFile {
    return javaFileFactory.createFileFromText(
      name,
      JavaLanguage.INSTANCE,
      content.trimIndent()
    ) as PsiJavaFile
  }
}
