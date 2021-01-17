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

import com.rickbusarow.modulecheck.CPP
import com.rickbusarow.modulecheck.internal.Output
import com.rickbusarow.modulecheck.rule.OvershotRule
import com.rickbusarow.modulecheck.rule.RedundantRule
import com.rickbusarow.modulecheck.rule.UnusedRule
import kotlinx.coroutines.runBlocking
import org.gradle.api.tasks.TaskAction

abstract class ModuleCheckTask : AbstractModuleCheckTask() {

  @TaskAction
  fun execute() = runBlocking {
    val alwaysIgnore = alwaysIgnore.get()
    val ignoreAll = ignoreAll.get()

    measured {
      val all = OvershotRule(project, alwaysIgnore, ignoreAll).check() +
        RedundantRule(project, alwaysIgnore, ignoreAll).check() +
        UnusedRule(project, alwaysIgnore, ignoreAll).check()

      all.distinctBy { it.dependentProject to CPP(it.config, it.dependencyProject) }
        .finish()
    }

    project.moduleCheckProjects().groupBy { it.getMainDepth() }.toSortedMap()
      .forEach { (depth, modules) ->
        Output.printBlue("""$depth  ${modules.joinToString { it.path }}""")
      }
  }
}
