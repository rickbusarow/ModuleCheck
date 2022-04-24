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

package modulecheck.api.rule

import modulecheck.utils.CaseMatcher
import modulecheck.utils.capitalize
import modulecheck.utils.decapitalize

data class RuleName(
  /** some-rule-name */
  val kebabCase: String
) {
  init {
    check(CaseMatcher.KebabCaseMatcher().matches(kebabCase)) {
      "The base name of a rule must be 'kebab-case', such as 'some-rule-name'." +
        "  This provided name was '$kebabCase'."
    }
  }

  /** SomeRuleName */
  val titleCase: String
    get() = kebabCase.split('-').joinToString { it.capitalize() }

  /** some_rule_name */
  val snakeCase: String get() = kebabCase.replace('-', '_')

  /** someRuleName */
  val pascalCase: String
    get() = kebabCase.split('-')
      .joinToString { it.capitalize() }
      .decapitalize()

  /** 'Some Rule Name' */
  val words: String
    get() = kebabCase.split('-')
      .joinToString(" ") { it.capitalize() }
      .decapitalize()

  companion object {
    fun fromLegacyIDOrNull(legacyID: String): RuleName? {

      return when (legacyID) {
        "AnvilFactoryGeneration" -> RuleName("use-anvil-factory-generation")
        "Depth" -> RuleName("project-depth")
        "DisableAndroidResources" -> RuleName("disable-android-resources")
        "DisableViewBinding" -> RuleName("disable-view-binding")
        "InheritedDependency" -> RuleName("inherited-dependency")
        "MustBeApi" -> RuleName("must-be-api")
        "OverShotDependency" -> RuleName("overshot-dependency")
        "RedundantDependency" -> RuleName("redundant-dependency")
        "SortDependencies" -> RuleName("sort-dependencies")
        "SortPlugins" -> RuleName("sort-plugins")
        "UnusedDependency" -> RuleName("unused-dependency")
        "UnusedKapt" -> RuleName("unused-kapt-processor")
        "UnusedKotlinAndroidExtensions" -> RuleName("unused-kotlin-android-extensions")
        else -> null
      }
    }
  }
}
