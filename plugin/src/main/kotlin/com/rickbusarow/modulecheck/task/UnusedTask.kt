/*
 * Copyright (C) 2020 Rick Busarow
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

package com.rickbusarow.modulecheck.task

abstract class UnusedTask : AbstractModuleCheckTask() {
/*
  @TaskAction
  fun execute() = runBlocking {
    val alwaysIgnore = alwaysIgnore.get()
    val ignoreAll = ignoreAll.get()

    measured {
      UnusedRule(project, alwaysIgnore, ignoreAll).check()
        .finish()
    }

    project.moduleCheckProjects().groupBy { it.getMainDepth() }.toSortedMap()
      .forEach { (depth, modules) ->
        Output.printBlue("""$depth  ${modules.joinToString { it.path }}""")
      }
  }
  */
}
