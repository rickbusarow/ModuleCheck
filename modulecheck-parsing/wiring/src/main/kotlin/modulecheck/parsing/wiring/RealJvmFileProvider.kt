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

package modulecheck.parsing.wiring

import com.squareup.anvil.annotations.ContributesBinding
import modulecheck.dagger.AppScope
import modulecheck.dagger.SingleIn
import modulecheck.parsing.gradle.SourceSetName
import modulecheck.parsing.java.RealJavaFile
import modulecheck.parsing.psi.RealKotlinFile
import modulecheck.parsing.psi.internal.PsiElementResolver
import modulecheck.parsing.psi.internal.asKtFile
import modulecheck.parsing.psi.internal.isKotlinFile
import modulecheck.parsing.psi.internal.isKtFile
import modulecheck.parsing.source.JvmFile
import modulecheck.project.JvmFileProvider
import modulecheck.project.McProject
import modulecheck.utils.SafeCache
import org.jetbrains.kotlin.incremental.isJavaFile
import java.io.File
import javax.inject.Inject
import javax.inject.Provider

@SingleIn(AppScope::class)
class FileCache @Inject constructor() : SafeCache<File, JvmFile> by SafeCache()

class RealJvmFileProvider(
  private val fileCache: FileCache,
  private val project: McProject,
  private val sourceSetName: SourceSetName
) : JvmFileProvider {

  override suspend fun getOrNull(
    file: File
  ): JvmFile? {

    // ignore anything which isn't Kotlin or Java
    if (!file.isKotlinFile() && !file.isJavaFile()) return null

    return fileCache.getOrPut(file) {
      when {
        file.isKtFile() -> RealKotlinFile(
          ktFile = file.asKtFile(),
          psiResolver = PsiElementResolver(
            project = project,
            sourceSetName = sourceSetName
          )
        )
        else -> RealJavaFile(file)
      }
    }
  }

  @ContributesBinding(AppScope::class)
  class Factory @Inject constructor(
    private val fileCacheProvider: Provider<FileCache>
  ) : JvmFileProvider.Factory {

    override fun create(
      project: McProject,
      sourceSetName: SourceSetName
    ): RealJvmFileProvider = RealJvmFileProvider(
      fileCache = fileCacheProvider.get(),
      project = project,
      sourceSetName = sourceSetName
    )
  }
}
