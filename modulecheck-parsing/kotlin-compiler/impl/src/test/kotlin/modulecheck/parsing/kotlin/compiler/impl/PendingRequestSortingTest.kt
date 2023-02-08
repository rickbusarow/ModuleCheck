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

package modulecheck.parsing.kotlin.compiler.impl

import io.kotest.assertions.asClue
import io.kotest.assertions.throwables.shouldNotThrow
import modulecheck.parsing.kotlin.compiler.HasAnalysisResult
import modulecheck.parsing.kotlin.compiler.impl.SafeAnalysisResultAccessImpl.PendingRequest
import modulecheck.testing.BaseTest
import modulecheck.utils.lazy.LazyDeferred
import modulecheck.utils.lazy.lazyDeferred
import modulecheck.utils.mapToSet
import org.jetbrains.kotlin.analyzer.AnalysisResult
import org.jetbrains.kotlin.descriptors.impl.ModuleDescriptorImpl
import org.jetbrains.kotlin.resolve.BindingContext
import org.junit.jupiter.api.Test
import kotlin.random.Random

internal class PendingRequestSortingTest : BaseTest() {

  @Test
  fun `sorting does not violate general contract`() {

    repeat(1000) {
      val subject = createPendingRequests(100)

      subject.pretty().asClue {
        shouldNotThrow<IllegalArgumentException> {
          subject.sorted()
        }
      }
    }
  }

  fun createPendingRequests(size: Int): List<PendingRequest> {

    val seed = List(size) {
      val name = it.toString(16)
        .padStart(4, '0')
        .lowercase()
      PendingPending(name, mutableSetOf())
    }

    return seed.onEach { target ->
      seed.asSequence()
        .filterNot { other -> other == target }
        .filterNot { other ->
          other.deps.contains(target) || other.deps.any { it.deps.contains(target) }
        }
        .forEach { dep ->
          if (Random.nextBoolean()) {
            target.deps.addAll(dep.deps + dep)
          }
        }
    }
      .onEach { target ->
        target.deps.forEach { dep ->
          require(!dep.deps.contains(target)) {
            """
              |target -- ${target.name} -- ${target.deps.map { it.name }}
              |dep    -- ${dep.name} -- ${dep.deps.map { it.name }}
            """.trimMargin()
          }
        }
      }
      .map { it.toPendingRequest() }
  }

  fun List<PendingRequest>.pretty() = joinToString("\n") { it.pretty() }
  fun PendingRequest.pretty(): String {
    return "${requester.name} - ${dependencies.map { it.name }})"
  }

  data class PendingPending(
    val name: String,
    val deps: MutableSet<PendingPending> = mutableSetOf()
  ) {

    fun toPendingRequest() = PendingRequest(
      requester = TestHasAnalysisResult(name),
      dependencies = deps.mapToSet { TestHasAnalysisResult(it.name) }
    )

    override fun equals(other: Any?): Boolean {
      if (this === other) return true
      if (other !is PendingPending) return false

      if (name != other.name) return false

      return true
    }

    override fun hashCode(): Int {
      return name.hashCode()
    }
  }

  private data class TestHasAnalysisResult(
    val name: String
  ) : HasAnalysisResult {
    override val analysisResultDeferred: LazyDeferred<AnalysisResult> = lazyDeferred { TODO() }
    override val bindingContextDeferred: LazyDeferred<BindingContext> =
      lazyDeferred { BindingContext.EMPTY }
    override val moduleDescriptorDeferred: LazyDeferred<ModuleDescriptorImpl> =
      lazyDeferred { TODO() }
  }

  private val HasAnalysisResult.name: String
    get() = (this as TestHasAnalysisResult).name
}
