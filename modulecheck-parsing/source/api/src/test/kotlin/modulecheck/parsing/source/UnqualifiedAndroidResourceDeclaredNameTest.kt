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

package modulecheck.parsing.source

import io.kotest.matchers.collections.shouldContainExactly
import modulecheck.testing.sealedSubclassInstances
import org.junit.jupiter.api.Test

internal class UnqualifiedAndroidResourceDeclaredNameTest : BaseMcNameTest() {

  @Test
  fun `all possible prefixes should be returned by the prefixes function`() {
    val allPossibleInstances = UnqualifiedAndroidResourceDeclaredName::class
      .sealedSubclassInstances("some name")
      .toList()

    val expectedPrefixes = allPossibleInstances
      .map { it.prefix }
      // The type names and prefixes don't match perfectly, since `AndroidString` and
      // `AndroidInteger` are different in order to avoid collisions.  So, re-sort after they're
      // just a list of the prefixes.
      .sorted()

    UnqualifiedAndroidResourceDeclaredName.prefixes() shouldContainExactly expectedPrefixes
  }
}
