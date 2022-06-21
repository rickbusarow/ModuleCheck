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

package modulecheck.testing

import io.kotest.property.Gen
import io.kotest.property.PropTestConfig
import io.kotest.property.PropertyContext
import kotlinx.coroutines.runBlocking

/** shorthand for the Kotest `forAll`, but blocking and always returning Unit */
fun <A> forAllBlocking(
  genA: Gen<A>,
  property: suspend PropertyContext.(A) -> Unit
) = runBlocking<Unit> {
  io.kotest.property.forAll(PropTestConfig(), genA) {
    property.invoke(this, it)
    true
  }
}

/** shorthand for the Kotest `forAll`, but blocking and always returning Unit */
@JvmName("forAllBlockingExtension")
fun <A> Gen<A>.forAllBlocking(
  property: suspend PropertyContext.(A) -> Unit
): Unit = forAllBlocking(genA = this) {
  property.invoke(this, it)
}
