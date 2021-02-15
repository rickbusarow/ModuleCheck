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

package modulecheck.core.rule

import modulecheck.api.Project2
import modulecheck.core.InheritedImplementationDependencyFinding
import modulecheck.core.parser.InheritedImplementationParser

class InheritedImplementationRule(
  project: Project2,
  alwaysIgnore: Set<String>,
  ignoreAll: Set<String>
) : AbstractRule<InheritedImplementationDependencyFinding>(
  project, alwaysIgnore, ignoreAll
) {

  override fun check(): List<InheritedImplementationDependencyFinding> {
    if (project.path in ignoreAll) return emptyList()

    return InheritedImplementationParser.parse(project)
      .all()
      .toList()
  }
}
