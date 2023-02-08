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

package modulecheck.rule

import modulecheck.config.ModuleCheckSettings
import modulecheck.finding.Finding
import modulecheck.finding.FindingName
import modulecheck.project.McProject
import javax.inject.Qualifier

interface ModuleCheckRule<T : Finding> {

  val name: FindingName
  val description: String
  val documentationUrl: String

  suspend fun check(project: McProject): List<T>
  fun shouldApply(settings: ModuleCheckSettings): Boolean
}

interface ReportOnlyRule<T : Finding> : ModuleCheckRule<T>
interface SortRule<T : Finding> : ModuleCheckRule<T>

@Qualifier
annotation class AllRules
