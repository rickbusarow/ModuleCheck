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

package modulecheck.gradle.internal

import org.gradle.api.Action
import org.gradle.api.Task
import org.gradle.api.tasks.TaskContainer
import org.gradle.api.tasks.TaskProvider

/** lazily adds [tasks] as dependencies to the receiver task */
fun <T : Task> TaskProvider<out T>.dependsOn(
  tasks: Collection<TaskProvider<out Task>>
): TaskProvider<out T> {
  if (tasks.isEmpty().not()) {
    configure { it.dependsOn(tasks) }
  }

  return this
}

/** lazily adds [tasks] as dependencies to the receiver task */
fun <T : Task> TaskProvider<out T>.dependsOn(
  vararg tasks: TaskProvider<out Task>
): TaskProvider<out T> {
  if (tasks.isEmpty().not()) {
    configure { it.dependsOn(*tasks) }
  }

  return this
}

/**
 * Lazily configures the provided Task without relying upon the Kotlin Gradle DSL.
 *
 * ex:
 * ```
 * tasks.register("myTask", MyTaskClass::class.java, arg0).configuring { task ->
 *   task.someInput.set(...)
 * }
 * ```
 */
fun <T : Task> TaskProvider<T>.configuring(action: (T) -> Unit) = apply {
  configure(action)
}

/**
 * Adds a task of this name and type if it doesn't exist. [configurationAction]
 * is performed on the new task, or the existing task if one already existed.
 */
@JvmName("registerOnceInline")
inline fun <reified T : Task> TaskContainer.registerOnce(
  name: String,
  configurationAction: Action<in T>
): TaskProvider<T> = registerOnce(name, T::class.java, configurationAction)

/**
 * Adds a task of this name and type if it doesn't exist. [configurationAction]
 * is performed on the new task, or the existing task if one already existed.
 */
fun <T : Task> TaskContainer.registerOnce(
  name: String,
  type: Class<T>,
  configurationAction: Action<in T>
): TaskProvider<T> = if (names.contains(name)) {
  named(name, type, configurationAction)
} else {
  register(name, type, configurationAction)
}
