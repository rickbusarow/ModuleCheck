/*
 * Copyright (C) 2021-2024 Rick Busarow
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
package modulecheck.builds

import com.rickbusarow.kgx.EagerGradleApi
import com.rickbusarow.kgx.isRealRootProject
import com.rickbusarow.kgx.matchingName
import com.rickbusarow.ktlint.KtLintPlugin
import com.rickbusarow.ktlint.KtLintTask
import kotlinx.validation.KotlinApiBuildTask
import kotlinx.validation.KotlinApiCompareTask
import org.gradle.api.Plugin
import org.gradle.api.Project
import kotlin.text.RegexOption.MULTILINE

abstract class KtLintConventionPlugin : Plugin<Project> {
  override fun apply(target: Project) {

    target.plugins.apply(KtLintPlugin::class.java)

    target.dependencies
      .add("ktlint", target.libs.rickBusarow.ktrules)

    @OptIn(EagerGradleApi::class)
    target.tasks.withType(KtLintTask::class.java).configureEach { task ->
      task.dependsOn(":updateEditorConfigVersion")
      task.mustRunAfter(
        target.tasks.matchingName("apiDump"),
        target.tasks.matchingName("dependencyGuard"),
        target.tasks.matchingName("dependencyGuardBaseline"),
        target.tasks.withType(KotlinApiBuildTask::class.java),
        target.tasks.withType(KotlinApiCompareTask::class.java)
      )
    }

    if (target.isRealRootProject()) {

      target.tasks.register("updateEditorConfigVersion") { task ->

        val file = target.file(".editorconfig")

        task.doLast {
          val oldText = file.readText()

          val reg = """^(ktlint_kt-rules_project_version *?= *?)\S*$""".toRegex(MULTILINE)

          val newText = oldText.replace(reg, "$1$VERSION_NAME")

          if (newText != oldText) {
            file.writeText(newText)
          }
        }
      }
    }
  }
}
