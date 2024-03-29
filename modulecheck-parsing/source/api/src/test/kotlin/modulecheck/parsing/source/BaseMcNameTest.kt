/*
 * Copyright (C) 2021-2024 Rick Busarow
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

import com.rickbusarow.kase.DefaultTestEnvironment.Factory
import com.rickbusarow.kase.HasTestEnvironmentFactory
import com.rickbusarow.kase.Kase1
import com.rickbusarow.kase.kases
import modulecheck.parsing.source.McName.CompatibleLanguage
import modulecheck.parsing.source.McName.CompatibleLanguage.JAVA
import modulecheck.parsing.source.McName.CompatibleLanguage.KOTLIN
import modulecheck.parsing.source.McName.CompatibleLanguage.XML
import modulecheck.testing.assertions.TrimmedAsserts
import modulecheck.utils.letIf

@Suppress("UnnecessaryAbstractClass")
abstract class BaseMcNameTest :
  HasTestEnvironmentFactory<Factory>,
  TrimmedAsserts {

  override val testEnvironmentFactory = Factory()

  fun allReferenceNames(
    name: String = "com.test.Subject",
    skipUnqualified: Boolean = false,
    languages: List<CompatibleLanguage> = listOf(JAVA, KOTLIN, XML)
  ): List<Kase1<ReferenceName>> {

    return languages.flatMap { lang ->
      listOf(
        ReferenceName(name, lang),
        AndroidDataBindingReferenceName(name, lang),
        QualifiedAndroidResourceReferenceName(name, lang)
      ).letIf(!skipUnqualified) {
        it + UnqualifiedAndroidResourceReferenceName(name, lang)
      }
    }
      .let { kases(it) }
  }
}
