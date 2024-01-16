/*
 * Copyright (C) 2021-2024 Rick Busarow
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

import modulecheck.testing.assertions.requireNotNullOrFail
import kotlin.reflect.KClass
import kotlin.reflect.KFunction
import kotlin.reflect.full.memberProperties
import kotlin.reflect.full.primaryConstructor
import kotlin.reflect.jvm.isAccessible

/**
 * Returns the full tree of classes which implement a base sealed class/interface,
 * including grandchildren, great-grandchildren, etc. This is different from
 * [KClass.sealedSubclasses] in that the latter only returns the direct children.
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

/**
 * Provides a sequence of the primary constructors of all non-abstract, non-sealed
 * subclasses of the sealed class or interface that implement this [KClass].
 *
 * @return A [Sequence] of the primary [KFunction] constructors of all descendant classes.
 */
inline fun <reified T : Any> KClass<T>.sealedSubclassConstructorsRecursive(): Sequence<KFunction<T>> {
  return sealedSubclassesRecursive()
    .filter { !it.isAbstract && !it.isSealed }
    .map { clazz ->
      clazz.primaryConstructor
        .requireNotNullOrFail { "no primary constructor for $clazz???" }
    }
}

/**
 * Provides a sequence of instances of all non-abstract, non-sealed subclasses of the
 * sealed class or interface that implement this [KClass]. The instances are created
 * using the primary constructor of each subclass, with the provided arguments.
 *
 * @param args The arguments to be passed to the primary constructor of each subclass.
 * @return A [Sequence] of instances of all descendant classes.
 */
inline fun <reified T : Any> KClass<T>.sealedSubclassInstances(vararg args: Any?): Sequence<T> {
  return sealedSubclassConstructorsRecursive()
    .mapNotNull {
      kotlin.runCatching { it.call(*args) }.getOrNull()
    }
}

/**
 * Retrieves the value of a private property in the receiver instance by its name.
 *
 * @param name The name of the private property.
 * @return The value of the private property.
 * @throws IllegalArgumentException if the receiver does
 *   not have a private property with the given name.
 */
inline fun <reified T : Any, reified R : Any> T.getPrivateFieldByName(name: String): R {
  val kClass = T::class

  val property = kClass.memberProperties.find { it.name == name }

  requireNotNull(property) { "Cannot find a property named `$name` in ${kClass.qualifiedName}." }

  property.isAccessible = true

  return property.get(this) as R
}

/**
 * Returns the current class if it's a real class, otherwise walks up
 * the hierarchy of enclosing/nesting classes until it finds a real one.
 *
 * In practical terms, this strips away Kotlin's anonymous lambda
 * "classes" and other compatibility shims, returning the real class.
 */
tailrec fun Class<*>.firstNonSyntheticClass(): Class<*> {
  return when {
    canonicalName != null -> this
    else -> enclosingClass.firstNonSyntheticClass()
  }
}

/**
 * Provides a sequence of all the enclosing classes of this [Class].
 *
 * @param includeSelf Determines whether to include the current class in the returned sequence.
 * @return A [Sequence] of all enclosing classes.
 */
fun Class<*>.enclosingClasses(includeSelf: Boolean = false): Sequence<Class<*>> {
  return generateSequence(if (includeSelf) this else enclosingClass) { it.enclosingClass }
}

/**
 * Provides a sequence of the canonical names of all enclosing classes of this [Class].
 *
 * @param includeSelf Determines whether to include the canonical
 *   name of the current class in the returned sequence.
 * @return A [Sequence] of the canonical names of all enclosing classes.
 */
fun Class<*>.enclosingCanonicalNames(includeSelf: Boolean = false): Sequence<String> {
  return enclosingClasses(includeSelf = includeSelf).mapNotNull { it.canonicalName }
}
