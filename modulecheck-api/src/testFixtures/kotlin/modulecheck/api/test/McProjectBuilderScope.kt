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

package modulecheck.api.test

import modulecheck.parsing.*
import java.io.File
import java.util.concurrent.ConcurrentHashMap

class McProjectBuilderScope(
  var path: String,
  var projectDir: File,
  var buildFile: File,
  val configurations: MutableMap<ConfigurationName, Config> = mutableMapOf(),
  val projectDependencies: ProjectDependencies = ProjectDependencies(mutableMapOf()),
  var hasKapt: Boolean = false,
  val sourceSets: MutableMap<SourceSetName, SourceSet> = mutableMapOf(
    SourceSetName.MAIN to SourceSet(SourceSetName.MAIN)
  ),
  var anvilGradlePlugin: AnvilGradlePlugin? = null,
  val projectCache: ConcurrentHashMap<String, McProject> = ConcurrentHashMap()
)
