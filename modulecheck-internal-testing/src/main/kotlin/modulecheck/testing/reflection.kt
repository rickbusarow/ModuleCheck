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

package modulecheck.testing

import kotlin.reflect.KClass
import kotlin.reflect.KFunction
import kotlin.reflect.full.memberProperties
import kotlin.reflect.full.primaryConstructor
import kotlin.reflect.jvm.isAccessible

/**
 * Returns the full tree of classes which implement a base sealed class/interface, including
 * grandchildren, great-grandchildren, etc. This is different from [KClass.sealedSubclasses] in that
 * the latter only returns the direct children.
 *
 * @since 0.12.0
 */
inline fun <reified T : Any> KClass<T>.sealedSubclassesRecursive(): Sequence<KClass<out T>> {
  return generateSequence(sealedSubclasses.asSequence()) { subs ->
    subs
      .flatMap { it.sealedSubclasses }
      .takeIf { it.iterator().hasNext() }
  }
    .flatten()
    .distinct()
}

inline fun <reified T : Any> KClass<T>.sealedSubclassConstructorsRecursive(): Sequence<KFunction<T>> {
  return sealedSubclassesRecursive()
    .filter { !it.isAbstract && !it.isSealed }
    .map {
      it.primaryConstructor
        .requireNotNullOrFail { "no primary constructor for $it???" }
    }
}

inline fun <reified T : Any> KClass<T>.sealedSubclassInstances(vararg args: Any?): Sequence<T> {
  return sealedSubclassConstructorsRecursive()
    .mapNotNull {
      kotlin.runCatching { it.call(*args) }.getOrNull()
    }
}

inline fun <reified T : Any, reified R : Any> T.getPrivateFieldByName(name: String): R {
  val kClass = T::class

  val property = kClass.memberProperties.find { it.name == name }

  requireNotNull(property) { "Cannot find a property named `$name` in ${kClass.qualifiedName}." }

  property.isAccessible = true

  return property.get(this) as R
}
