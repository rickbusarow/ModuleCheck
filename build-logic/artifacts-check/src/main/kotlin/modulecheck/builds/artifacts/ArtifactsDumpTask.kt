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

package modulecheck.builds.artifacts

import org.gradle.api.file.ProjectLayout
import org.gradle.api.tasks.TaskAction
import javax.inject.Inject

/**
 * Evaluates all published artifacts in the project and writes the results to `/artifacts.json`
 */
open class ArtifactsDumpTask @Inject constructor(
  projectLayout: ProjectLayout
) : ArtifactsTask(projectLayout) {

  init {
    description = "Parses the Maven artifact parameters for all modules " +
      "and writes them to artifacts.json"
    group = "other"
  }

  @TaskAction
  fun run() {
    val json = moshiAdapter.indent("  ").toJson(currentList)

    reportFile.asFile.writeText(json)
  }
}
