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

package modulecheck.utils

import io.kotest.matchers.shouldBe
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Test

internal class FlowFlatMapTest {

  @Test
  fun `flow of lists should flatten to list`() = runBlocking<Unit> {

    flowOf(listOf(1, 2, 3), listOf(4, 5, 6))
      .flatMapListConcat { it } shouldBe listOf(1, 2, 3, 4, 5, 6)
  }

  @Test
  fun `flow of lists should flatten to provided destination list`() = runBlocking<Unit> {

    val destination = mutableListOf<Int>()

    flowOf(listOf(1, 2, 3), listOf(4, 5, 6))
      .flatMapListConcat(destination) { it }

    destination shouldBe listOf(1, 2, 3, 4, 5, 6)
  }

  @Test
  fun `flow of sets should flatten to set`() = runBlocking<Unit> {

    flowOf(setOf(1, 2, 3), setOf(4, 5, 6))
      .flatMapSetConcat { it } shouldBe setOf(1, 2, 3, 4, 5, 6)
  }

  @Test
  fun `flow of sets should flatten to provided destination set`() = runBlocking<Unit> {

    val destination = mutableSetOf<Int>()

    flowOf(setOf(1, 2, 3), setOf(4, 5, 6))
      .flatMapSetConcat(destination) { it }

    destination shouldBe setOf(1, 2, 3, 4, 5, 6)
  }
}
