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

import modulecheck.testing.BaseTest
import modulecheck.testing.createSafely
import modulecheck.utils.child
import modulecheck.utils.requireNotNull
import org.junit.jupiter.api.Test

internal class SafeXmlParserTest : BaseTest() {

  @Test
  fun `should parse if the node contains a zero width space`() {

    val text = """
      <?xml version="1.0" encoding="utf-8"?>
      <resources>
        <style name="Foo"/>
        ​
      </resources>
    """.trimIndent()

    val xml = testProjectDir
      .child("styles.xml")
      .createSafely(text)

    val fileNode = SafeXmlParser().parse(xml).requireNotNull()

    fileNode.children().toString() shouldBe SafeXmlParser().parse(text)!!.children().toString()

    fileNode.children().toString() shouldBe "[style[attributes={name=Foo}; value=[]], \n" +
      "  \u200B\n" +
      "]"
  }
}
