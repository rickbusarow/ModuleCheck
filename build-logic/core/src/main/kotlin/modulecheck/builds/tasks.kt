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

package modulecheck.builds

import org.gradle.api.Action
import org.gradle.api.Task
import org.gradle.api.tasks.TaskCollection
import org.gradle.api.tasks.TaskContainer
import org.gradle.api.tasks.TaskProvider

fun TaskContainer.maybeNamed(
  taskName: String,
  configuration: Task.() -> Unit
) {

  if (names.contains(taskName)) {
    named(taskName, configuration)
    return
  }

  matchingName(taskName)
    .configureEach(configuration)
}

/** code golf for `matching { it.name == taskName }` */
fun TaskContainer.matchingName(
  taskName: String
): TaskCollection<Task> = matching { it.name == taskName }

/**
 * adds all [objects] as dependencies to every task in the collection, inside a `configureEach { }`
 */
fun <T : Task> TaskCollection<T>.dependOn(vararg objects: Any): TaskCollection<T> {
  return also { taskCollection ->
    taskCollection.configureEach { task -> task.dependsOn(*objects) }
  }
}

/** adds all [objects] as dependencies inside a configuration block, inside a `configure { }` */
fun <T : Task> TaskProvider<T>.dependsOn(vararg objects: Any): TaskProvider<T> {
  return also { provider ->
    provider.configure { task ->
      task.dependsOn(*objects)
    }
  }
}

/**
 * Returns a collection containing the objects in this collection of the given type. The returned
 * collection is live, so that when matching objects are later added to this collection, they are
 * also visible in the filtered collection.
 *
 * @param S The type of objects to find.
 * @return The matching objects. Returns an empty collection if there are no such objects in this
 *     collection.
 * @see [TaskCollection.withType]
 */
inline fun <reified S : Task> TaskCollection<in S>.withType(): TaskCollection<S> =
  withType(S::class.java)

inline fun <reified T : Task> TaskContainer.register(
  name: String,
  vararg constructorArguments: Any,
  noinline configuration: (T) -> Unit
): TaskProvider<T> = register(name, T::class.java, *constructorArguments)
  .apply { configure { configuration(it) } }

/**
 * Adds a task of this name and type if it doesn't exist. [configurationAction] is performed on the
 * new task, or the existing task if one already existed.
 */
@JvmName("registerOnceInline")
inline fun <reified T : Task> TaskContainer.registerOnce(
  name: String,
  configurationAction: Action<in T>
): TaskProvider<T> = registerOnce(name, T::class.java, configurationAction)

/**
 * Adds a task of this name and type if it doesn't exist. [configurationAction] is performed on the
 * new task, or the existing task if one already existed.
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

/**
 * @return the fully qualified name of this task's type, without any '_Decorated' suffix if one
 *     exists
 */
fun Task.undecoratedTypeName(): String {
  return javaClass.canonicalName.removeSuffix("_Decorated")
}
