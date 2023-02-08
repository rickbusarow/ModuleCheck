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

package modulecheck.rule.impl

import modulecheck.config.ModuleCheckSettings
import modulecheck.core.context.unusedKaptProcessors
import modulecheck.finding.Finding
import modulecheck.finding.FindingName
import modulecheck.project.McProject
import javax.inject.Inject

const val KAPT_PLUGIN_ID: String = "org.jetbrains.kotlin.kapt"
const val KAPT_ALTERNATE_PLUGIN_ID: String = "kotlin-kapt"
internal const val KAPT_PLUGIN_FUN = "kapt"

class UnusedKaptProcessorRule @Inject constructor(
  private val settings: ModuleCheckSettings
) : DocumentedRule<Finding>() {

  override val name: FindingName = FindingName("unused-kapt-processor")
  override val description: String = "Finds unused kapt processor dependencies " +
    "and warns if the kapt plugin is applied but unused"

  override suspend fun check(project: McProject): List<Finding> {

    return project.unusedKaptProcessors().all()
  }

  override fun shouldApply(settings: ModuleCheckSettings): Boolean {
    return settings.checks.unusedKapt
  }
}
