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

package modulecheck.parsing.gradle

import java.io.File

data class AndroidGradleSettings(
  val assignments: List<Assignment>,
  val androidBlocks: List<AgpBlock.AndroidBlock>,
  val buildFeaturesBlocks: List<AgpBlock.BuildFeaturesBlock>
)

interface AndroidGradleSettingsProvider {

  fun get(): AndroidGradleSettings

  fun interface Factory {
    fun create(buildFile: File): AndroidGradleSettingsProvider
  }
}

interface AndroidGradleParser {

  fun parse(buildFile: File): AndroidGradleSettings
}
