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

package modulecheck.parsing.wiring

import com.squareup.anvil.annotations.ContributesBinding
import modulecheck.dagger.SingleIn
import modulecheck.dagger.TaskScope
import modulecheck.model.sourceset.SourceSetName
import modulecheck.parsing.java.RealJavaFile
import modulecheck.parsing.kotlin.compiler.KotlinEnvironment
import modulecheck.parsing.kotlin.compiler.internal.isKotlinFile
import modulecheck.parsing.kotlin.compiler.internal.isKtFile
import modulecheck.parsing.psi.RealKotlinFile
import modulecheck.parsing.psi.internal.PsiElementResolver
import modulecheck.parsing.source.JvmFile
import modulecheck.parsing.source.internal.AndroidDataBindingNameProvider
import modulecheck.parsing.source.internal.AndroidRNameProvider
import modulecheck.parsing.source.internal.InterpretingInterceptor
import modulecheck.parsing.source.internal.ParsingChain
import modulecheck.project.JvmFileProvider
import modulecheck.project.McProject
import modulecheck.utils.cache.SafeCache
import org.jetbrains.kotlin.incremental.isJavaFile
import java.io.File
import javax.inject.Inject
import javax.inject.Provider

/**
 * Note that there is also a Psi file cache inside the [KotlinEnvironment].
 * This cache of [JvmFile] just provides the next layer, in order
 * to have caching for parsed declarations, references, and whatnot.
 *
 * The Psi file cache re-uses Psi files because they have
 * internal caching used internally by the compilation object.
 *
 * @since 0.12.0
 */
@SingleIn(TaskScope::class)
class JvmFileCache @Inject constructor() {
  private val delegate = SafeCache<File, JvmFile>(listOf(JvmFileCache::class))

  /**
   * @return a cached [JvmFile], or creates and caches a new one using [default]
   * @since 0.12.0
   */
  suspend fun getOrPut(
    file: File,
    default: suspend () -> JvmFile
  ): JvmFile {
    return delegate.getOrPut(key = file, default)
  }
}

class RealJvmFileProvider(
  private val jvmFileCache: JvmFileCache,
  private val project: McProject,
  private val sourceSetName: SourceSetName,
  private val androidRNameProvider: AndroidRNameProvider,
  private val androidDataBindingNameProvider: AndroidDataBindingNameProvider
) : JvmFileProvider {

  override suspend fun getOrNull(
    file: File
  ): JvmFile? {

    // ignore anything which isn't Kotlin or Java
    if (!file.isKotlinFile() && !file.isJavaFile()) return null

    return jvmFileCache.getOrPut(file) {

      val sourceSet = project.sourceSets.getValue(sourceSetName)
      val kotlinEnvironment = sourceSet.kotlinEnvironmentDeferred.await()

      val nameParser = ParsingChain.Factory(
        listOf(
          ConcatenatingParsingInterceptor(),
          AndroidResourceReferenceParsingInterceptor(
            androidRNameProvider = androidRNameProvider
          ),
          AndroidDataBindingReferenceParsingInterceptor(
            androidDataBindingNameProvider = androidDataBindingNameProvider
          ),
          InterpretingInterceptor()
        )
      )

      when {
        file.isKtFile() -> RealKotlinFile(
          file = file,
          psi = kotlinEnvironment.ktFile(file),
          psiResolver = PsiElementResolver(
            project = project,
            sourceSetName = sourceSetName
          ),
          nameParser = nameParser
        )

        else -> RealJavaFile(
          file = file,
          psi = kotlinEnvironment.javaPsiFile(file),
          jvmTarget = sourceSet.jvmTarget,
          nameParser = nameParser
        )
      }
    }
  }

  @ContributesBinding(TaskScope::class)
  class Factory @Inject constructor(
    private val jvmFileCacheProvider: Provider<JvmFileCache>
  ) : JvmFileProvider.Factory {

    override fun create(
      project: McProject,
      sourceSetName: SourceSetName
    ): RealJvmFileProvider = RealJvmFileProvider(
      jvmFileCache = jvmFileCacheProvider.get(),
      project = project,
      sourceSetName = sourceSetName,
      androidRNameProvider = RealAndroidRNameProvider(project, sourceSetName),
      androidDataBindingNameProvider = RealAndroidDataBindingNameProvider(project, sourceSetName)
    )
  }
}
