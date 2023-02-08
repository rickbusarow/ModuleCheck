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

package modulecheck.utils

import app.cash.turbine.test
import io.kotest.matchers.shouldBe
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.yield
import modulecheck.utils.coroutines.filterAsync
import org.junit.jupiter.api.Test

internal class SequenceFilterAsyncTest {

  @Test
  fun `should only emit elements which match the predicate`() = runBlocking {

    sequenceOf(1, 2, 3, 4)
      .filterAsync { it % 2 == 0 }
      .toList() shouldBe listOf(2, 4)
  }

  @Test
  fun `should emit async elements as soon as they're complete`() = runBlocking {

    val one = CompletableDeferred<Int>()
    val two = CompletableDeferred<Int>()
    val three = CompletableDeferred<Int>()

    val throughFilter = mutableListOf<Int>()

    sequenceOf(one, two, three)
      .filterAsync {

        val done = it.await()
        throughFilter.add(done)
        done == 2
      }
      .test {
        expectNoEvents()

        // should trigger predicate and be filtered out
        one.complete(1)
        yield()
        yield()
        throughFilter shouldBe listOf(1)
        expectNoEvents()

        // should trigger predicate and pass through
        two.complete(2)
        awaitItem() shouldBe two
        throughFilter shouldBe listOf(1, 2)
        expectNoEvents()

        // should trigger predicate and be filtered out
        three.complete(3)
        yield()
        throughFilter shouldBe listOf(1, 2, 3)

        // filtered flow ends as soon as the source flow has no more elements
        awaitComplete()
      }
  }
}
