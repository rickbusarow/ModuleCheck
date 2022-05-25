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

package modulecheck.utils.coroutines

import io.kotest.assertions.fail
import io.kotest.matchers.shouldBe
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Test

internal class FlowContainsTest {

  @Test
  fun `should return true if flow contains matching element`() = runBlocking {

    flowOf(1, 2, 3).contains(3) shouldBe true
  }

  @Test
  fun `should stop collecting after matching element is collected`() = runBlocking {

    flow {
      emit(1)
      emit(2)
      emit(3)
      fail("collection should stop after 3")
    }
      .contains(3) shouldBe true
  }

  @Test
  fun `should return false if flow does not contain matching element`() = runBlocking {

    flowOf(1, 2, 3).contains(4) shouldBe false
  }

  @Test
  fun `should use equals for comparison`() = runBlocking {

    data class DataClass(val value: Int)

    val dc1 = DataClass(1)
    val dc2 = DataClass(1)

    flowOf(dc1, DataClass(2), DataClass(3)).contains(dc2) shouldBe true
  }
}
