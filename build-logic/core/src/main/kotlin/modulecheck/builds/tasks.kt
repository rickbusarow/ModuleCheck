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

import org.gradle.api.Task
import org.gradle.api.tasks.TaskCollection
import org.gradle.api.tasks.TaskContainer

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
