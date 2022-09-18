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
import modulecheck.parsing.source.McName.CompatibleLanguage.XML
import modulecheck.parsing.source.ReferenceName
import modulecheck.parsing.source.SimpleName.Companion.asSimpleName
import modulecheck.parsing.source.UnqualifiedAndroidResource.Companion.id
import modulecheck.parsing.source.UnqualifiedAndroidResourceReferenceName
import modulecheck.testing.BaseTest
import modulecheck.utils.child
import modulecheck.utils.createSafely
import org.intellij.lang.annotations.Language
import org.junit.jupiter.api.Test

internal class XmlFileTest : BaseTest() {

  @Test
  fun `an id which is declared in a layout should count as a declaration`() = test {
    @Language("xml")
    val text = """
    <?xml version="1.0" encoding="utf-8"?>
    <LinearLayout
      xmlns:android="http://schemas.android.com/apk/res/android"
      xmlns:tools="http://schemas.android.com/tools"
      android:id="@+id/fragment_container"
      android:layout_width="match_parent"
      android:layout_height="wrap_content"
      android:orientation="vertical"
      tools:ignore="UnusedResources"
      >

      <LinearLayout
        android:id="@+id/fragment_container_nested"
        android:layout_width="match_parent"
        style="@style/Theme.AppCompat.Light.DarkActionBar"
        android:layout_height="wrap_content"
        android:orientation="horizontal"
        />

    </LinearLayout>
    """.trimIndent()

    val xml = testProjectDir
      .child("stubs.xml")
      .createSafely(text)

    val file = XmlFile.LayoutFile(xml)

    file.idDeclarations shouldBe setOf(
      id("fragment_container".asSimpleName()),
      id("fragment_container_nested".asSimpleName())
    )

    file.resourceReferencesAsRReferences shouldBe setOf(
      "R.style.Theme_AppCompat_Light_DarkActionBar"
    )

    file.references.toList() shouldBe listOf(
      ReferenceName("LinearLayout", XML),
      UnqualifiedAndroidResourceReferenceName("R.style.Theme_AppCompat_Light_DarkActionBar", XML)
    )

    file.customViews.value shouldBe setOf(ReferenceName("LinearLayout", XML))
  }
}
