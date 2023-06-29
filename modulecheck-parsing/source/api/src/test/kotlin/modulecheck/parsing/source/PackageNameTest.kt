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

package modulecheck.parsing.source

import io.kotest.assertions.throwables.shouldNotThrow
import io.kotest.assertions.throwables.shouldThrowWithMessage
import io.kotest.matchers.shouldBe
import io.kotest.property.Arb
import io.kotest.property.arbitrary.stringPattern
import modulecheck.parsing.source.PackageName.DEFAULT
import modulecheck.testing.forAllBlocking
import org.junit.jupiter.api.Test

internal class PackageNameTest {

  @Test
  fun `any non-empty name string just becomes wrapped by the class`() {

    Arb.stringPattern("""(.|\s)*\S(.|\s)*""")
      .forAllBlocking { name ->

        shouldNotThrow<Throwable> {
          PackageName(name)
        }
      }
  }

  @Test
  fun `an empty package name parameter to PackageNameImpl throws exception with message`() {

    shouldThrowWithMessage<IllegalArgumentException>(
      "A ${PackageNameImpl::class.qualifiedName} must be a non-empty, " +
        "non-blank String.  Represent an empty/blank " +
        "or missing package name as ${DEFAULT::class.qualifiedName}.  " +
        "This name argument, wrapped in single quotes: ''"
    ) {
      PackageNameImpl("")
    }
  }

  @Test
  fun `a blank package name parameter to PackageNameImpl throws exception with message`() {

    Arb.stringPattern("\\s*")
      .forAllBlocking { name ->

        shouldThrowWithMessage<IllegalArgumentException>(
          "A ${PackageNameImpl::class.qualifiedName} must be a non-empty, " +
            "non-blank String.  Represent an empty/blank " +
            "or missing package name as ${DEFAULT::class.qualifiedName}.  " +
            "This name argument, wrapped in single quotes: '$name'"
        ) {
          PackageNameImpl(name)
        }
      }
  }

  @Test
  fun `an empty package name becomes DEFAULT`() {

    PackageName("") shouldBe DEFAULT
  }

  @Test
  fun `a blank package name becomes DEFAULT`() {

    Arb.stringPattern("\\s*")
      .forAllBlocking { name ->

        PackageName(name) shouldBe DEFAULT
      }
  }
}
