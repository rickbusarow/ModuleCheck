/*
 * Copyright (C) 2021 Rick Busarow
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

package modulecheck.gradle.task

import modulecheck.api.*
import modulecheck.core.mcp
import modulecheck.core.rule.sort.SortPluginsRule
import modulecheck.gradle.project2
import modulecheck.psi.DslBlockVisitor

abstract class SortPluginsTask : AbstractModuleCheckTask() {

  override fun getFindings(): List<Finding> {
    val alwaysIgnore = alwaysIgnore.get()
    val ignoreAll = ignoreAll.get()
    val visitor = DslBlockVisitor("plugins")

    return measured {
      project
        .project2()
        .allprojects
        .filter { it.buildFile.exists() }
        .sortedByDescending { it.mcp().getMainDepth() }
        .flatMap { proj ->
          SortPluginsRule(
            project = proj,
            alwaysIgnore = alwaysIgnore,
            ignoreAll = ignoreAll,
            visitor = visitor,
            comparator = pluginComparator
          )
            .check()
        }
    }
  }
}
