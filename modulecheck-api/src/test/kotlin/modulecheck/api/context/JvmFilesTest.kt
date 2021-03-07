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

package modulecheck.api.context

import hermit.test.junit.HermitJUnit5
import io.kotest.matchers.shouldBe
import modulecheck.api.Project2Impl
import org.junit.jupiter.api.Test
import java.io.File
import java.util.concurrent.ConcurrentHashMap

internal class JvmFilesTest : HermitJUnit5() {

  @Test
  fun `foo`() {

    val p = Project2Impl(
      path = ":",
      projectDir = File(""),
      buildFile = File(""),
      configurations = emptyMap(),
      projectDependencies = lazy { emptyMap() },
      hasKapt = false,
      sourceSets = emptyMap(),
      projectCache = ConcurrentHashMap(),
      anvilGradlePlugin = null
    )

    p[JvmFiles] shouldBe emptyMap()
  }
}
