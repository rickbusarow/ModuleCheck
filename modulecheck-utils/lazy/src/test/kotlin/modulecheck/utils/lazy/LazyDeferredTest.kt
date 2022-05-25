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

package modulecheck.utils.lazy

import io.kotest.matchers.collections.shouldContainExactlyInAnyOrder
import io.kotest.matchers.shouldBe
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Test
import java.util.concurrent.atomic.AtomicBoolean

internal class LazyDeferredTest {

  @Test
  fun `nothing should be invoked until await`() = runBlocking {

    val completed = AtomicBoolean(false)

    val deferred = lazyDeferred {
      completed.lazySet(true)
      true
    }

    completed.get() shouldBe false

    deferred.await() shouldBe true
    completed.get() shouldBe true
  }

  @Test
  fun `awaitAll should not deadlock`() = runBlocking<Unit> {

    val one = lazyDeferred { 1 }
    val two = lazyDeferred { 2 }
    val three = lazyDeferred { 3 }

    val all = listOf(one, two, three).awaitAll()

    all shouldContainExactlyInAnyOrder listOf(1, 2, 3)
  }
}
