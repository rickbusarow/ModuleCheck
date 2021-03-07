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

package modulecheck.testing

import hermit.test.Hermit
import hermit.test.LazyResets
import java.io.File
import java.nio.file.Files
import java.util.*

fun Hermit.tempDir(path: String = UUID.randomUUID().toString()): LazyResets<File> {
  return object : LazyResets<File> {

    private var lazyHolder: Lazy<File> = createLazy()

    override val value: File
      get() = lazyHolder.value

    private fun createLazy() = lazy {
      register(this)
      Files.createTempDirectory(path).toFile()
    }

    override fun reset() {
      value.deleteRecursively()

      lazyHolder = createLazy()
    }

    override fun isInitialized(): Boolean = lazyHolder.isInitialized()
  }
}

fun Hermit.tempFile(path: String = "temp.kt"): LazyResets<File> {
  return object : LazyResets<File> {

    private var lazyHolder: Lazy<File> = createLazy()

    override val value: File
      get() = lazyHolder.value

    private fun createLazy() = lazy {
      register(this)

      @Suppress("DEPRECATION")
      createTempFile(path)
    }

    override fun reset() {
      value.deleteRecursively()

      lazyHolder = createLazy()
    }

    override fun isInitialized(): Boolean = lazyHolder.isInitialized()
  }
}

