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

import app.cash.turbine.test
import io.kotest.matchers.shouldBe
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Test

internal class MapAsyncTest {

  @Test
  fun `flow should emit async elements as soon as they're transformed`() = runBlocking {

    val one = CompletableDeferred<Int>()
    val two = CompletableDeferred<Int>()
    val three = CompletableDeferred<Int>()

    val through = mutableListOf<Int>()

    flowOf(one, two, three)
      .mapAsync {

        val done = it.await()
        through.add(done)
        done * 2
      }
      .test {
        expectNoEvents()

        // should trigger predicate and be transformed
        one.complete(1)
        through shouldBe listOf(1)
        awaitItem() shouldBe 2

        // should trigger predicate and be transformed
        two.complete(2)
        through shouldBe listOf(1, 2)
        awaitItem() shouldBe 4
        expectNoEvents()

        // should trigger predicate and be transformed
        three.complete(3)
        through shouldBe listOf(1, 2, 3)
        awaitItem() shouldBe 6

        // filtered flow ends as soon as the source flow has no more elements
        awaitComplete()
      }
  }

  @Test
  fun `iterable should emit async elements as soon as they're transformed`() = runBlocking {

    val one = CompletableDeferred<Int>()
    val two = CompletableDeferred<Int>()
    val three = CompletableDeferred<Int>()

    val through = mutableListOf<Int>()

    listOf(one, two, three)
      .mapAsync {

        val done = it.await()
        through.add(done)
        done * 2
      }
      .test {
        expectNoEvents()

        // should trigger predicate and be transformed
        one.complete(1)
        through shouldBe listOf(1)
        awaitItem() shouldBe 2

        // should trigger predicate and be transformed
        two.complete(2)
        through shouldBe listOf(1, 2)
        awaitItem() shouldBe 4
        expectNoEvents()

        // should trigger predicate and be transformed
        three.complete(3)
        through shouldBe listOf(1, 2, 3)
        awaitItem() shouldBe 6

        // filtered flow ends as soon as the source flow has no more elements
        awaitComplete()
      }
  }

  @Test
  fun `sequence should emit async elements as soon as they're transformed`() = runBlocking {

    val one = CompletableDeferred<Int>()
    val two = CompletableDeferred<Int>()
    val three = CompletableDeferred<Int>()

    val through = mutableListOf<Int>()

    sequenceOf(one, two, three)
      .mapAsync {

        val done = it.await()
        through.add(done)
        done * 2
      }
      .test {
        expectNoEvents()

        // should trigger predicate and be transformed
        one.complete(1)
        through shouldBe listOf(1)
        awaitItem() shouldBe 2

        // should trigger predicate and be transformed
        two.complete(2)
        through shouldBe listOf(1, 2)
        awaitItem() shouldBe 4
        expectNoEvents()

        // should trigger predicate and be transformed
        three.complete(3)
        through shouldBe listOf(1, 2, 3)
        awaitItem() shouldBe 6

        // filtered flow ends as soon as the source flow has no more elements
        awaitComplete()
      }
  }
}
