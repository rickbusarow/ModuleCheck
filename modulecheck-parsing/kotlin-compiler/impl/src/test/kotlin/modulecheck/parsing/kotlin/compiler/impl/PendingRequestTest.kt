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

package modulecheck.parsing.kotlin.compiler.impl

import io.kotest.assertions.asClue
import io.kotest.assertions.throwables.shouldNotThrow
import modulecheck.parsing.kotlin.compiler.HasPsiAnalysis
import modulecheck.parsing.kotlin.compiler.impl.SafeAnalysisResultAccessImpl.PendingRequest
import modulecheck.testing.BaseTest
import modulecheck.utils.lazy.LazyDeferred
import modulecheck.utils.lazy.lazyDeferred
import modulecheck.utils.mapToSet
import org.jetbrains.kotlin.descriptors.impl.ModuleDescriptorImpl
import org.jetbrains.kotlin.resolve.BindingContext
import org.junit.jupiter.api.Test
import kotlin.random.Random

internal class PendingRequestTest : BaseTest() {

  fun createPendingRequests(size: Int): List<PendingRequest> {

    val seed = List(size) {
      val name = it.toString(16)
        .padStart(4, '0')
        .lowercase()
      PendingPending(name, mutableSetOf())
    }

    val r = Random.Default

    return seed.reversed()
      .onEachIndexed { index, target ->

        seed.asSequence()
          // .take(index)
          .filterNot { it == target }
          .filterNot { it.deps.contains(target) }
          .forEach { dep ->
            if (r.nextBoolean()) {
              target.deps.addAll(dep.deps + dep)
            }
          }
      }
      .onEach { target ->
        target.deps.forEach { dep ->
          require(!dep.deps.contains(target)) {
            """
              |target -- ${target.name} -- ${target.deps.map { it.name }}
              |
              |dep    -- ${dep.name} -- ${dep.deps.map { it.name }}
            """.trimMargin()
          }
          // dep.deps shouldNotContain target
        }
      }
      .map { it.toPendingRequest() }
  }

  @Test
  fun `sorting`() {

    repeat(1) {
      val subject = createPendingRequests(10)

      subject.pretty().asClue {
        shouldNotThrow<IllegalArgumentException> {
          subject.sorted()
            .joinToString("\n")
            .also(::println)
        }
      }
    }
  }

  @Test
  fun `sorting2`() {

    // shouldThrow<IllegalArgumentException> {

    knownBad.sorted()
      .joinToString("\n")
      .also(::println)

    // }
  }

  fun List<PendingRequest>.pretty() = joinToString(",\n") { it.pretty() }
  fun PendingRequest.pretty(): String {
    return """PendingPending("${requester.name}") to mutableSetOf<String>(${
    dependencies.joinToString(", ") { "\"${it.name}\"" }
    })"""
  }

  val theMap = mapOf(
    PendingPending("001f") to mutableSetOf("0002"),
    PendingPending("001e") to mutableSetOf(),
    PendingPending("001d") to mutableSetOf(),
    PendingPending("001c") to mutableSetOf(),
    PendingPending("001b") to mutableSetOf(),
    PendingPending("001a") to mutableSetOf(),
    PendingPending("0019") to mutableSetOf(),
    PendingPending("0018") to mutableSetOf(),
    PendingPending("0017") to mutableSetOf(),
    PendingPending("0016") to mutableSetOf(),
    PendingPending("0015") to mutableSetOf(),
    PendingPending("0014") to mutableSetOf(),
    PendingPending("0013") to mutableSetOf(),
    PendingPending("0012") to mutableSetOf(),
    PendingPending("0011") to mutableSetOf(),
    PendingPending("0010") to mutableSetOf("0004", "000f"),
    PendingPending("000f") to mutableSetOf("0002", "000e"),
    PendingPending("000e") to mutableSetOf("0002"),
    PendingPending("000d") to mutableSetOf("0000", "0002", "0005", "0006", "000b"),
    PendingPending("000c") to mutableSetOf("0008"),
    PendingPending("000b") to mutableSetOf("0002", "0003"),
    PendingPending("000a") to mutableSetOf("0008"),
    PendingPending("0009") to mutableSetOf(),
    PendingPending("0008") to mutableSetOf("0003", "0005", "0006"),
    PendingPending("0007") to mutableSetOf(),
    PendingPending("0006") to mutableSetOf("0005"),
    PendingPending("0005") to mutableSetOf("0003"),
    PendingPending("0004") to mutableSetOf(),
    PendingPending("0003") to mutableSetOf(),
    PendingPending("0002") to mutableSetOf(),
    PendingPending("0001") to mutableSetOf(),
    PendingPending("0000") to mutableSetOf()
  )

  val knownBad = theMap.map { (target, deps) ->

    val nameToPending = theMap.keys.associateBy { it.name }

    val depsPending = deps.mapToSet { nameToPending.getValue(it) }
    target.also { it.deps.addAll(depsPending) }
  }.map { it.toPendingRequest() }

  data class PendingPending(
    val name: String,
    val deps: MutableSet<PendingPending> = mutableSetOf()
  ) {

    fun toPendingRequest() = PendingRequest(
      requester = TestHasPsi(name),
      dependencies = deps.mapToSet { TestHasPsi(it.name) }
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
}

internal data class TestHasPsi(val name: String) : HasPsiAnalysis {
  override val bindingContext: LazyDeferred<BindingContext> =
    lazyDeferred { BindingContext.EMPTY }
  override val moduleDescriptorDeferred: LazyDeferred<ModuleDescriptorImpl> =
    lazyDeferred { TODO() }
}

private val HasPsiAnalysis.name: String
  get() = (this as TestHasPsi).name
