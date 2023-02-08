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

package modulecheck.finding

import modulecheck.reporting.logging.McLogger
import modulecheck.utils.CaseMatcher
import modulecheck.utils.capitalize
import modulecheck.utils.decapitalize

data class FindingName(
  /**
   * some-finding-name
   *
   * @since 0.12.0
   */
  val id: String
) {
  init {
    check(CaseMatcher.KebabCaseMatcher().matches(id)) {
      "The base name of a finding must be 'kebab-case', such as 'some-finding-name'." +
        "  This provided name was '$id'."
    }
  }

  /**
   * SomeFindingName
   *
   * @since 0.12.0
   */
  val titleCase: String
    get() = id.split('-').joinToString("") { it.capitalize() }

  /**
   * some_finding_name
   *
   * @since 0.12.0
   */
  val snakeCase: String get() = id.replace('-', '_')

  /**
   * someFindingName
   *
   * @since 0.12.0
   */
  val pascalCase: String
    get() = id.split('-')
      .joinToString("") { it.capitalize() }
      .decapitalize()

  /**
   * 'Some finding Name'
   *
   * @since 0.12.0
   */
  val words: String
    get() = id.split('-')
      .joinToString(" ") { it.capitalize() }
      .decapitalize()

  companion object {

    /**
     * @return a [FindingName] if [maybeFindingName] is `kebab-case`, otherwise `null`.
     * @since 0.12.4
     */
    fun safe(maybeFindingName: String): FindingName? {
      return if (CaseMatcher.KebabCaseMatcher().matches(maybeFindingName)) {
        FindingName(maybeFindingName)
      } else {
        null
      }
    }

    @Deprecated("This will be removed soon.")
    fun migrateLegacyIdOrNull(legacyID: String, logger: McLogger): String? {

      @Suppress("SpellCheckingInspection")
      val migrated = when (legacyID.lowercase()) {
        "useanvilfactories" -> "use-anvil-factory-generation"
        "depth" -> "project-depth"
        "disableandroidresources" -> "disable-android-resources"
        "disableviewbinding" -> "disable-view-binding"
        "inheriteddependency" -> "inherited-dependency"
        "mustbeapi" -> "must-be-api"
        "overshot" -> "overshot-dependency"
        "redundant" -> "redundant-dependency"
        "unsorteddependencies" -> "sort-dependencies"
        "unsortedplugins" -> "sort-plugins"
        "unused" -> "unused-dependency"
        "unusedkaptplugin" -> "unused-kapt-plugin"
        "unusedkaptprocessor" -> "unused-kapt-processor"
        "unusedkotlinandroidextensions" -> "unused-kotlin-android-extensions"
        else -> null
      }

      if (migrated != null) {
        logger.printWarningLine(
          "The suppressed issue `$legacyID` is using a deprecated ID.  " +
            "The new name for this issue is `$migrated`."
        )
      }

      return migrated
    }
  }
}
