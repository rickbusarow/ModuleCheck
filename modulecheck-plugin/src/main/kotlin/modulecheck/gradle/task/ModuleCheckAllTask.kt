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

import modulecheck.api.Finding
import modulecheck.api.settings.ChecksSettings
import modulecheck.core.rule.ModuleCheckRule
import modulecheck.parsing.McProject
import javax.inject.Inject
import kotlin.reflect.full.declaredMemberProperties

abstract class ModuleCheckAllTask @Inject constructor(
  private val rules: List<ModuleCheckRule<Finding>>
) : ModuleCheckTask<Finding>() {

  override fun evaluate(projects: List<McProject>): List<Finding> {
    val props = ChecksSettings::class.declaredMemberProperties
      .associate { it.name to it.get(settings.checks) as Boolean }

    val findings = projects.flatMap { proj ->
      @Suppress("DEPRECATION")
      @Suppress("DEPRECATION")
      this.rules
        .filter { props[it.id.decapitalize()] ?: false }
        .flatMap { it.check(proj) }
    }

    return findings
  }
}
