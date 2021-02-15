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

package modulecheck.api

import hermit.test.Hermit
import hermit.test.LazyResets
import java.io.File
import java.nio.file.Files

fun Hermit.tempDir(path: String = ""): LazyResets<File> {
  return object : LazyResets<File> {

    val delegate = LazyResets(this@tempDir) { Files.createTempDirectory(path).toFile() }

    override fun reset() {
      delegate.value.delete()
      delegate.reset()
    }

    override val value: File
      get() = delegate.value

    override fun isInitialized(): Boolean = delegate.isInitialized()
  }
}
