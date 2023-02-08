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

package modulecheck.utils.trace

import io.kotest.matchers.shouldBe
import org.junit.jupiter.api.Test

class TraceTest {

  @Test
  fun `asString formatting`() {

    val trace = Trace.start("Weather Underground")
      .child(tags = listOf(Kitchen::class), args = listOf("spaghetti"))
      .child(tags = listOf(Computer::class), args = listOf(Website("reddit.com")))
      .child(tags = listOf(Garage::class), args = listOf("bike"))
      .child(tags = listOf("Oak Leaf Trail"), args = listOf())
      .child(tags = listOf("home"), args = listOf("shower"))

    trace.asString() shouldBe """
      <ROOT OF TRACE>
      tags: [Weather Underground]
      └─ tags: [Kitchen]  --  args: [spaghetti]
         └─ tags: [Computer]  --  args: [Website(name=reddit.com)]
            └─ tags: [Garage]  --  args: [bike]
               └─ tags: [Oak Leaf Trail]  --  args: []
                  └─ tags: [home]  --  args: [shower]
    """.trimIndent()
  }

  data class Website(val name: String)
  object Computer
  object Kitchen
  object Garage
}
