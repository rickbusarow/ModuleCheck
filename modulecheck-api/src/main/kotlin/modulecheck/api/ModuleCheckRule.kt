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

package modulecheck.api

import modulecheck.api.settings.ChecksSettings
import modulecheck.api.settings.ModuleCheckSettings
import modulecheck.parsing.McProject
import modulecheck.parsing.psi.internal.asKtsFileOrNull
import org.jetbrains.kotlin.psi.KtFile

interface ModuleCheckRule<T> {

  val settings: ModuleCheckSettings
  val id: String
  val description: String

  fun check(project: McProject): List<T>
  fun shouldApply(checksSettings: ChecksSettings): Boolean

  fun McProject.kotlinBuildFileOrNull(): KtFile? = buildFile.asKtsFileOrNull()
}

fun interface RuleFactory {

  fun create(settings: ModuleCheckSettings): List<ModuleCheckRule<out Finding>>
}
