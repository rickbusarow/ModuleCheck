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
import modulecheck.api.settings.ModuleCheckSettings
import modulecheck.core.InheritedImplementationDependencyFinding
import modulecheck.core.parser.InheritedImplementationParser

class InheritedImplementationRule(
  override val settings: ModuleCheckSettings
) : ModuleCheckRule<InheritedImplementationDependencyFinding>() {

  override val id = "InheritedImplementation"
  override val description = "Finds project dependencies which are used in the current module, " +
    "but are not actually directly declared as dependencies in the current module"

  override fun check(project: Project2): List<InheritedImplementationDependencyFinding> {
    return InheritedImplementationParser.parse(project)
      .all()
      .toList()
  }
}
