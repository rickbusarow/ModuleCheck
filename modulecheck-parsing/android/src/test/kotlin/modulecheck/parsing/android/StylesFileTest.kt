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

package modulecheck.parsing.android

import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.runBlocking
import modulecheck.parsing.source.SimpleName.Companion.asSimpleName
import modulecheck.parsing.source.UnqualifiedAndroidResource
import modulecheck.testing.BaseTest
import modulecheck.utils.child
import modulecheck.utils.createSafely
import org.intellij.lang.annotations.Language
import org.junit.jupiter.api.Test

internal class StylesFileTest : BaseTest() {

  @Test
  fun `style parent and value references should count as references`() = runBlocking {
    @Language("xml")
    val text = """
    <?xml version="1.0" encoding="utf-8"?>
    <resources>

      <style name="AppTheme.ClearActionBar" parent="Theme.AppCompat.Light.DarkActionBar">
        <item name="colorPrimary">@color/transparent</item>
        <item name="alertDialogStyle">@style/SomeOtherStyle</item>
      </style>

    </resources>
    """.trimIndent()

    val xml = testProjectDir
      .child("styles.xml")
      .createSafely(text)

    val file = AndroidStylesFile(xml)

    file.references.toList() shouldBe setOf(
      UnqualifiedAndroidResource.color("transparent".asSimpleName()),
      UnqualifiedAndroidResource.style("SomeOtherStyle".asSimpleName()),
      UnqualifiedAndroidResource.style("Theme_AppCompat_Light_DarkActionBar".asSimpleName())
    )
  }
}
