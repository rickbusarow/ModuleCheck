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

import java.lang.StackWalker.StackFrame
import java.lang.reflect.AnnotatedElement
import java.lang.reflect.Method
import kotlin.annotation.AnnotationRetention.RUNTIME
import kotlin.annotation.AnnotationTarget.CLASS
import kotlin.annotation.AnnotationTarget.FUNCTION
import kotlin.annotation.AnnotationTarget.PROPERTY
import kotlin.annotation.AnnotationTarget.PROPERTY_GETTER
import kotlin.annotation.AnnotationTarget.PROPERTY_SETTER

/**
 * Indicates that the annotated function/property should be ignored when walking a
 * stack trace, such as in assertions or when trying to parse a test function's name.
 *
 * @see StackTraceElement.isSkipped
 */
@Target(
  FUNCTION,
  PROPERTY,
  PROPERTY_GETTER,
  PROPERTY_SETTER,
  CLASS
)
@Retention(RUNTIME)
annotation class SkipInStackTrace

/**
 * Checks if an [AnnotatedElement] is annotated with [SkipInStackTrace].
 *
 * @receiver [AnnotatedElement] The element to check.
 * @return `true` if the [AnnotatedElement] is annotated with [SkipInStackTrace], `false` otherwise.
 */
@PublishedApi
internal fun AnnotatedElement.hasSkipAnnotation(): Boolean {
  return isAnnotationPresent(SkipInStackTrace::class.java)
}

private val sdkPackagePrefixes = setOf("java", "jdk", "kotlin")

/**
 * Checks if the [StackTraceElement] should be skipped based on the [SkipInStackTrace] annotation.
 *
 * @receiver [StackTraceElement] The element to check.
 * @return `true` if the [StackTraceElement] should be skipped, `false` otherwise.
 */
@SkipInStackTrace
@PublishedApi
internal fun StackTraceElement.isSkipped(): Boolean {
  // return declaringClass().isSkipped(methodName = methodName.removeSuffix("\$default"))
  return isSkipped(clazz = declaringClass(), methodName = methodName.substringBeforeLast('$'))
}

/**
 * Retrieves the class from a [StackTraceElement].
 *
 * @receiver [StackTraceElement] The element to inspect.
 * @return The class object for the stack trace element.
 */
@SkipInStackTrace
@PublishedApi
internal fun StackTraceElement.declaringClass(): Class<*> = Class.forName(className)

/**
 * Checks if the [StackFrame] should be skipped based on the [SkipInStackTrace] annotation.
 *
 * @receiver [StackFrame] The frame to check.
 * @return `true` if the [StackFrame] should be skipped, `false` otherwise.
 */
@SkipInStackTrace
@PublishedApi
internal fun StackFrame.isSkipped(): Boolean {
  return isSkipped(
    clazz = declaringClass(),
    methodName = methodName.removeSuffix("\$default")
  )
}

/**
 * Retrieves the class from a [StackFrame].
 *
 * @receiver [StackFrame] The frame to inspect.
 * @return The class object for the stack frame.
 */
@SkipInStackTrace
@PublishedApi
internal fun StackFrame.declaringClass(): Class<*> = Class.forName(className)

/**
 * Determines whether a method within the given class should be skipped.
 *
 * @param clazz The class in which the method is declared.
 * @param methodName The name of the method.
 * @return `true` if the method should be skipped, `false` otherwise.
 */
internal fun isSkipped(clazz: Class<*>, methodName: String): Boolean {

  // trim off all the stuff like "$$inlined$$execute$1""
  val actualClass = clazz.firstNonSyntheticClass()

  val enclosingClasses = generateSequence(clazz) { c -> c.enclosingClass }

  if (enclosingClasses.any { it.hasSkipAnnotation() }) return true

  val packageRoot = clazz.canonicalName?.split('.')?.firstOrNull()

  if (packageRoot in sdkPackagePrefixes) {
    return true
  }

  // nested classes and functions have the java `$` delimiter
  // ex: "com.example.MyTest$nested class$my test"
  fun String.segments(): List<String> = split(".", "$")
    .filter { it.isNotBlank() }

  val actualMethodName = clazz.name.removePrefix(actualClass.name)
    .segments()
    .firstOrNull()
    ?: methodName

  return actualClass
    .methods
    .filter { it.name == actualMethodName }
    .requireAllOrNoneAreAnnotated()
}

/**
 * Validates that all methods in the list are either annotated with [SkipInStackTrace]
 * or not. If only some methods are annotated, an exception will be thrown.
 *
 * @receiver List of [Method] to check.
 * @return `true` if all methods are annotated with [SkipInStackTrace], `false` otherwise.
 */
@SkipInStackTrace
private fun List<Method>.requireAllOrNoneAreAnnotated(): Boolean {

  val (annotated, notAnnotated) = partition {
    it.hasSkipAnnotation()
  }

  require(annotated.size == size || notAnnotated.size == size) {
    "The function named '${first().name}' is overloaded, " +
      "and only some those overloads are annotated with `@SkipInStackTrace`.  " +
      "Either all must be annotated or none of them."
  }

  return annotated.size == size
}
