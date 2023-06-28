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

package modulecheck.name.testing

import modulecheck.testing.BaseTest
import modulecheck.testing.TestEnvironment

@Suppress("UnnecessaryAbstractClass")
abstract class BaseNameTest : BaseTest<TestEnvironment>() {

  // fun allNames(
  //   name: String = "com.test.Subject",
  //   skipUnqualified: Boolean = false
  // ): List<Name> {
  //
  //   return   listOf(
  //       SimpleName(name),
  //       AndroidDataBindingReferenceName(name, lang),
  //       QualifiedAndroidResourceReferenceName(name, lang)
  //     ).letIf(!skipUnqualified) {
  //       it + UnqualifiedAndroidResourceReferenceName(name, lang)
  //     }
  //   }
}
