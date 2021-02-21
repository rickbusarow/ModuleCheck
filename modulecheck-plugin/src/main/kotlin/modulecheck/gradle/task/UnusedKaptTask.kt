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

/**
 * Loops through all registered annotation processors for each module,
 * checking that at least one applicable annotation is imported in the source.
 *
 * Throws warnings if a processor is applied without any annotations being used.
 */
/*
abstract class UnusedKaptTask : AbstractModuleCheckTask() {

  init {
    description =
      "Checks all modules with registered annotation processors to ensure they're needed."
  }

  override fun getFindings(): List<Finding> {
    return measured {
      project
        .project2()
        .allprojects
        .filter { it.buildFile.exists() }
        .sortedByDescending { it.mcp().getMainDepth() }
        .flatMap { proj ->
          UnusedKaptRule(settings).check(proj)
        }
    }
  }
}
*/
