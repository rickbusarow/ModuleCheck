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

package modulecheck.utils.cache

import hermit.test.junit.HermitJUnit5
import io.kotest.matchers.shouldBe
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineStart.UNDISPATCHED
import kotlinx.coroutines.async
import kotlinx.coroutines.runBlocking
import modulecheck.utils.lazy.lazyDeferred
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.fail
import java.util.concurrent.atomic.AtomicInteger

internal class SafeCacheTest : HermitJUnit5() {

  val expectCount by resets { AtomicInteger(0) }

  @Test
  fun `concurrent misses should only execute the lambda once`() = runBlocking {

    val cache = SafeCache<String, Int>()
    val lock = CompletableDeferred<Unit>()

    val one = async(start = UNDISPATCHED) {
      expect(1)
      cache.getOrPut("key") {
        lock.await()
        1
      }
    }
    val two = async(start = UNDISPATCHED) {
      expect(2)
      cache.getOrPut("key") { fail("This lambda should never execute") }
    }

    expect(3)

    lock.complete(Unit)

    one.await() shouldBe 1
    two.await() shouldBe 1
  }

  @Test
  fun `loaders can access other keys in the same cache`() = runBlocking {

    val cache = SafeCache<String, Int>()

    val one = lazyDeferred {
      expect(2)
      cache.getOrPut("one") { 1 }
    }
    val two = lazyDeferred {
      expect(1)
      cache.getOrPut("two") { one.await() }
    }

    two.await() shouldBe 1
    one.await() shouldBe 1
  }

  fun expect(value: Int) {
    expectCount.incrementAndGet() shouldBe value
  }
}
