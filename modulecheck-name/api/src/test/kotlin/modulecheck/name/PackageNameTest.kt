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

package modulecheck.name

import io.kotest.assertions.throwables.shouldNotThrow
import io.kotest.matchers.shouldBe
import io.kotest.property.Arb
import io.kotest.property.arbitrary.stringPattern
import modulecheck.testing.asTests
import modulecheck.utils.interpuncts
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestFactory

internal class PackageNameTest {

  @TestFactory
  fun `any non-empty name string just becomes wrapped by the class`() =
    Arb.stringPattern("""(.|\s)*\S(.|\s)*""")
      .asTests { name ->

        shouldNotThrow<Throwable> {
          PackageName(name).asString shouldBe name
        }
      }

  @Test
  fun `an empty package name becomes DEFAULT`() {

    PackageName("") shouldBe PackageName.DEFAULT
  }

  @Test
  fun `a null package name becomes DEFAULT`() {

    PackageName(null) shouldBe PackageName.DEFAULT
  }

  @TestFactory
  fun `a blank package name becomes DEFAULT`() = Arb.stringPattern("""[^\S\r\n]+""")
    .asTests(testName = { it.interpuncts }) { name ->

      PackageName(name) shouldBe PackageName.DEFAULT
    }
}
