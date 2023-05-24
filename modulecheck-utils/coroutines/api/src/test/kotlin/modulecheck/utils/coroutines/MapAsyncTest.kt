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

package modulecheck.utils.coroutines

import app.cash.turbine.test
import io.kotest.matchers.shouldBe
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.yield
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test

internal class MapAsyncTest {

  @Nested
  inner class `flow` {

    @Test
    fun `flow executes eagerly when collection starts`() = runTest {

      val lock = CompletableDeferred<Unit>()

      val waiting = mutableListOf<Int>()

      val subject = flowOf(1, 2)
        .mapAsync {

          waiting.add(it)
          lock.await()

          it
        }

      yield()
      yield()
      yield()

      waiting shouldBe emptyList()

      subject.test {

        expectNoEvents()

        yield()
        yield()
        yield()

        waiting shouldBe listOf(1, 2)

        lock.complete(Unit)

        awaitItem() shouldBe 1
        awaitItem() shouldBe 2

        awaitComplete()
      }
    }

    @Test
    fun `flow should emit async elements as soon as they're transformed`() = runTest {

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
          awaitItem() shouldBe 2
          through shouldBe listOf(1)

          // should trigger predicate and be transformed
          two.complete(2)
          awaitItem() shouldBe 4
          through shouldBe listOf(1, 2)
          expectNoEvents()

          // should trigger predicate and be transformed
          three.complete(3)
          awaitItem() shouldBe 6
          through shouldBe listOf(1, 2, 3)

          // filtered flow ends as soon as the source flow has no more elements
          awaitComplete()
        }
    }
  }

  @Nested
  inner class `iterable` {

    @Test
    fun `iterable executes eagerly when collection starts`() = runTest {

      val lock = CompletableDeferred<Unit>()

      val waiting = mutableListOf<Int>()

      val subject = listOf(1, 2)
        .mapAsync {

          waiting.add(it)
          lock.await()

          it
        }

      yield()
      yield()
      yield()

      waiting shouldBe emptyList()

      subject.test {

        expectNoEvents()

        yield()
        yield()
        yield()

        waiting shouldBe listOf(1, 2)

        lock.complete(Unit)

        awaitItem() shouldBe 1
        awaitItem() shouldBe 2

        awaitComplete()
      }
    }

    @Test
    fun `iterable should emit async elements as soon as they're transformed`() = runTest {

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
          awaitItem() shouldBe 2
          through shouldBe listOf(1)

          // should trigger predicate and be transformed
          two.complete(2)
          awaitItem() shouldBe 4
          through shouldBe listOf(1, 2)
          expectNoEvents()

          // should trigger predicate and be transformed
          three.complete(3)
          awaitItem() shouldBe 6
          through shouldBe listOf(1, 2, 3)

          // filtered flow ends as soon as the source flow has no more elements
          awaitComplete()
        }
    }
  }

  @Nested
  inner class `sequence` {

    @Test
    fun `sequence executes eagerly when collection starts`() = runTest {

      val lock = CompletableDeferred<Unit>()

      val waiting = mutableListOf<Int>()

      val subject = sequenceOf(1, 2)
        .mapAsync {

          waiting.add(it)
          lock.await()

          it
        }

      yield()
      yield()
      yield()

      waiting shouldBe emptyList()

      subject.test {

        expectNoEvents()

        yield()
        yield()
        yield()

        waiting shouldBe listOf(1, 2)

        lock.complete(Unit)

        awaitItem() shouldBe 1
        awaitItem() shouldBe 2

        awaitComplete()
      }
    }

    @Test
    fun `sequence should emit async elements as soon as they're transformed`() = runTest {

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
          awaitItem() shouldBe 2
          through shouldBe listOf(1)

          // should trigger predicate and be transformed
          two.complete(2)
          awaitItem() shouldBe 4
          through shouldBe listOf(1, 2)
          expectNoEvents()

          // should trigger predicate and be transformed
          three.complete(3)
          awaitItem() shouldBe 6
          through shouldBe listOf(1, 2, 3)

          // filtered flow ends as soon as the source flow has no more elements
          awaitComplete()
        }
    }
  }
}
