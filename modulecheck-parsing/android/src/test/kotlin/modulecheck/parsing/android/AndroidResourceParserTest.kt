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

package modulecheck.parsing.android

import modulecheck.parsing.source.SimpleName.Companion.asSimpleName
import modulecheck.parsing.source.UnqualifiedAndroidResource.Companion.string
import modulecheck.parsing.source.UnqualifiedAndroidResource.Companion.style
import modulecheck.testing.BaseTest
import modulecheck.utils.createSafely
import modulecheck.utils.resolve
import org.junit.jupiter.api.Test

internal class AndroidResourceParserTest : BaseTest {

  @Test
  fun `a node without attributes should not be parsed`() = test {
    val text = """
      <?xml version="1.0" encoding="utf-8"?>
      <resources>
        <foo/>
      </resources>
    """.trimIndent()

    val xml = workingDir
      .resolve("styles.xml")
      .createSafely(text)

    val declarations = AndroidResourceParser().parseFile(xml)

    declarations shouldBe emptySet()
  }

  @Test
  fun `a node without a name attribute should not be parsed`() = test {
    val text = """
      <?xml version="1.0" encoding="utf-8"?>
      <resources>
        <string someOtherAttribute="hi"/>
      </resources>
    """.trimIndent()

    val xml = workingDir
      .resolve("styles.xml")
      .createSafely(text)

    val declarations = AndroidResourceParser().parseFile(xml)

    declarations shouldBe emptySet()
  }

  @Test
  fun `a node with a name as the second attribute should be parsed`() = test {
    val text = """
      <?xml version="1.0" encoding="utf-8"?>
      <resources>
        <string someOtherAttribute="hi" name="app_name"/>
      </resources>
    """.trimIndent()

    val xml = workingDir
      .resolve("styles.xml")
      .createSafely(text)

    val declarations = AndroidResourceParser().parseFile(xml)

    declarations shouldBe setOf(
      string("app_name".asSimpleName())
    )
  }

  @Test
  fun `a node with dots in its name should be parsed using underscores`() = test {
    val text = """
      <?xml version="1.0" encoding="utf-8"?>
      <resources>
        <style name="AppTheme.ClearActionBar" parent="Theme.AppCompat.Light.DarkActionBar"/>
      </resources>
    """.trimIndent()

    val xml = workingDir
      .resolve("styles.xml")
      .createSafely(text)

    val declarations = AndroidResourceParser().parseFile(xml)

    declarations shouldBe setOf(
      style("AppTheme_ClearActionBar".asSimpleName())
    )
  }
}
