/*
 * Copyright (C) 2021-2022 Rick Busarow
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

import modulecheck.builds.libsCatalog
import modulecheck.builds.version
import org.jlleitschuh.gradle.ktlint.KtlintExtension

plugins {
  id("org.jlleitschuh.gradle.ktlint")
}

extensions.configure(KtlintExtension::class.java) {
  val libVersion = libsCatalog.version("ktlint-lib").requiredVersion
  version.set(libVersion)
  debug.set(false)
  outputToConsole.set(true)
  enableExperimentalRules.set(true)
  filter {
    exclude("**/generated/**")
    exclude("**/build/**")
  }
  disabledRules.set(
    setOf(
      "max-line-length", // manually formatting still does this, and KTLint will still wrap long chains when possible
      "filename", // same as Detekt's MatchingDeclarationName, but Detekt's version can be suppressed and this can't
      "experimental:argument-list-wrapping", // doesn't work half the time
      "experimental:no-empty-first-line-in-method-block", // code golf...
      // This can be re-enabled once 0.46.0 is released
      // https://github.com/pinterest/ktlint/issues/1435
      "experimental:type-parameter-list-spacing",
      // added in 0.46.0
      "experimental:function-signature"
    )
  )

  extensions.configure(KtlintExtension::class.java) {

    require(libVersion < "0.46.0") {
      """
      when updating to 0.46.0:
      - Re-enable `experimental:type-parameter-list-spacing`
      - remove 'experimental' from 'argument-list-wrapping'
      - remove 'experimental' from 'no-empty-first-line-in-method-block'
      """.trimIndent()
    }
  }
  dependencies {
    "ktlintRuleset"(project(path = ":ktlint-rules"))
  }
}

tasks.withType<org.jlleitschuh.gradle.ktlint.tasks.BaseKtLintCheckTask> {
  workerMaxHeapSize.set("512m")
}
