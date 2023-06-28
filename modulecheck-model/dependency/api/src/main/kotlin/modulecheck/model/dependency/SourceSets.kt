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

package modulecheck.model.dependency

import modulecheck.model.sourceset.SourceSetName
import modulecheck.utils.requireNotNull

/** Common interface for providing a [SourceSets] instance. */
interface HasSourceSets {
  val sourceSets: SourceSets
}

/** Cache of [sourceSets][McSourceSet], probably at the project level. */
class SourceSets(
  delegate: Map<SourceSetName, McSourceSet>
) : Map<SourceSetName, McSourceSet> by delegate

/**
 * shorthand for the source set associated with 'androidTest'.
 * @throws IllegalArgumentException if there is no such source set
 */
val SourceSets.androidTest: McSourceSet get() = requireSourceSet(SourceSetName.ANDROID_TEST)

/** shorthand for `get("androidTest".asSourceSetName())` */
val SourceSets.androidTestOrNull: McSourceSet? get() = get(SourceSetName.ANDROID_TEST)

/**
 * shorthand for the source set associated with 'debug'.
 * @throws IllegalArgumentException if there is no such source set
 */
val SourceSets.debug: McSourceSet get() = requireSourceSet(SourceSetName.DEBUG)

/** shorthand for `get("debug".asSourceSetName())` */
val SourceSets.debugOrNull: McSourceSet? get() = get(SourceSetName.DEBUG)

/**
 * shorthand for the source set associated with 'main'.
 * @throws IllegalArgumentException if there is no such source set
 */
val SourceSets.main: McSourceSet get() = requireSourceSet(SourceSetName.MAIN)

/** shorthand for `get("main".asSourceSetName())` */
val SourceSets.mainOrNull: McSourceSet? get() = get(SourceSetName.MAIN)

/**
 * shorthand for the source set associated with 'release'.
 * @throws IllegalArgumentException if there is no such source set
 */
val SourceSets.release: McSourceSet get() = requireSourceSet(SourceSetName.RELEASE)

/** shorthand for `get("release".asSourceSetName())` */
val SourceSets.releaseOrNull: McSourceSet? get() = get(SourceSetName.RELEASE)

/**
 * shorthand for the source set associated with 'test'.
 * @throws IllegalArgumentException if there is no such source set
 */
val SourceSets.test: McSourceSet get() = requireSourceSet(SourceSetName.TEST)

/** shorthand for `get("test".asSourceSetName())` */
val SourceSets.testOrNull: McSourceSet? get() = get(SourceSetName.TEST)

/**
 * shorthand for the source set associated with 'testFixtures'.
 * @throws IllegalArgumentException if there is no such source set
 */
val SourceSets.testFixtures: McSourceSet get() = requireSourceSet(SourceSetName.TEST_FIXTURES)

/** shorthand for `get("testFixtures".asSourceSetName())` */
val SourceSets.testFixturesOrNull: McSourceSet? get() = get(SourceSetName.TEST_FIXTURES)

/**
 * @return the source set associated with [sourceSetName], or throws if there is no value
 * @throws IllegalArgumentException if this collection
 *   does not contain a source set for the requested name
 */
fun SourceSets.requireSourceSet(sourceSetName: SourceSetName): McSourceSet {
  return get(sourceSetName).requireNotNull {
    val simpleName = SourceSets::class.java.simpleName
    "This $simpleName instance does not have a value for ${sourceSetName.value}.  " +
      "The existing keys are: ${keys.map { it.value }}"
  }
}
