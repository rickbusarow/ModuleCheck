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

package modulecheck.core.kapt

import modulecheck.api.KaptProcessor
import modulecheck.api.ParsedKapt
import modulecheck.api.Project2

object KaptParser {

  fun parseLazy(project: Project2): Lazy<ParsedKapt<KaptProcessor>> = lazy {
    parse(project)
  }

  fun parse(project: Project2): ParsedKapt<KaptProcessor> {
    val all = project.externalDependencies + project.projectDependencies

    val grouped = all
      .groupBy { it.config.name }
      .mapValues { (_, lst) ->

        lst.map { dep ->

          val name = dep.name

          KaptProcessor(name)
        }.toSet()
      }

    return ParsedKapt(
      grouped.getOrDefault("kaptAndroidTest", setOf()),
      grouped.getOrDefault("kapt", setOf()),
      grouped.getOrDefault("kaptTest", setOf())
    )
  }
}
