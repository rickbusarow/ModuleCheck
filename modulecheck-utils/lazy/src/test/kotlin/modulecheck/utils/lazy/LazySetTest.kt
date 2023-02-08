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

package modulecheck.utils.lazy

import io.kotest.common.runBlocking
import io.kotest.matchers.shouldBe
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.flow.toSet
import org.junit.jupiter.api.Test

class LazySetTest {
  @Test
  fun `isEmpty with empty LazySet when it's not cached`() = runBlocking {

    val subject = lazySet<Int>()

    subject.isEmpty() shouldBe true
    subject.isNotEmpty() shouldBe false
  }

  @Test
  fun `data source is only evaluated once`() = runBlocking {

    var invocations = 0

    val ds = dataSource {
      setOf(++invocations)
    }

    val ls1 = lazySet(ds)
    val ls2 = lazySet(ds)

    ls1.toSet() shouldBe setOf(1)
    ls2.toSet() shouldBe setOf(1)
  }

  @Test
  fun `isEmpty with empty LazySet when it's already cached`() = runBlocking {

    val subject = lazySet<Int>()

    subject.toList() shouldBe listOf()

    subject.isEmpty() shouldBe true
    subject.isNotEmpty() shouldBe false
  }

  @Test
  fun `isEmpty with non-empty LazySet when it's not cached`() = runBlocking {

    val subject = lazySet(dataSourceOf(1))

    subject.isEmpty() shouldBe false
    subject.isNotEmpty() shouldBe true

    subject.isFullyCached shouldBe true
    subject.snapshot().cache shouldBe setOf(1)
  }

  @Test
  fun `isEmpty with single dataSource when it's not cached`() = runBlocking {

    val subject = lazySet { setOf(1, 2, 3, 4) }

    subject.isEmpty() shouldBe false
    subject.isNotEmpty() shouldBe true

    subject.isFullyCached shouldBe true
    subject.snapshot().cache shouldBe setOf(1, 2, 3, 4)
  }

  @Test
  fun `isEmpty with non-empty LazySet when it's already cached`() = runBlocking {

    val subject = lazySet(dataSourceOf(1))

    subject.toList() shouldBe listOf(1)

    subject.isEmpty() shouldBe false
    subject.isNotEmpty() shouldBe true
  }

  @Test
  fun `isEmpty with non-empty LazySet when it's partially cached`() = runBlocking {

    val subject = lazySet(List(101) { dataSourceOf(it) })

    subject.first() shouldBe 0

    subject.snapshot().cache shouldBe (0..99).toSet()

    subject.isEmpty() shouldBe false
    subject.isNotEmpty() shouldBe true

    subject.snapshot().cache shouldBe (0..99).toSet()

    subject.toList() shouldBe List(101) { it }
  }

  @Test
  fun `should be fully cached if 'contains' returns false`() = runBlocking {

    val subject = lazySet(List(101) { dataSourceOf(it) })

    subject.contains(9000) shouldBe false

    subject.isFullyCached shouldBe true
  }
}
