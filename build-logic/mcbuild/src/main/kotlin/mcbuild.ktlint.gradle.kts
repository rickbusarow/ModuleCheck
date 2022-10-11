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

plugins {
  id("org.jmailen.kotlinter")
}

kotlinter {
  ignoreFailures = false
  reporters = arrayOf("checkstyle", "plain")
  experimentalRules = true
  disabledRules = arrayOf(
    // manually formatting still does this, and KTLint will still wrap long chains when possible
    "max-line-length",
    // same as Detekt's MatchingDeclarationName, but Detekt's version can be suppressed and this can't
    "filename",
    // code golf...
    "no-empty-first-line-in-method-block",
    // added in 0.46.0
    "experimental:function-signature"
  )
}
// dummy ktlint-gradle plugin task names which just delegate to the Kotlinter ones
tasks.register("ktlintCheck") {
  dependsOn("lintKotlin")
}
tasks.register("ktlintFormat") {
  dependsOn("formatKotlin")
}
