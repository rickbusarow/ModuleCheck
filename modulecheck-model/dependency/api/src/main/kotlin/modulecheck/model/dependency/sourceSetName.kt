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

package modulecheck.model.dependency

import modulecheck.model.sourceset.SourceSetName
import modulecheck.utils.capitalize
import modulecheck.utils.requireNotNull

/**
 * @return true if this source set is `androidTest` or `test`, or any
 *   other source set downstream of them, like `androidTestDebug`.
 */
fun SourceSetName.isTestingOnly(sourceSets: SourceSets): Boolean {

  return sourceSets.getOrElse(this) { sourceSets.getValue(SourceSetName.MAIN) }
    .withUpstream()
    .any { upstream ->
      upstream == SourceSetName.TEST || upstream == SourceSetName.ANDROID_TEST
    }
}

/**
 * @return the name of the non-test/published SourceSet associated with a given SourceSet
 *   name. For SourceSets which are published, this just returns the same name. For testing
 *   SourceSets, this returns the most-downstream source set which it's testing against.
 */
fun SourceSetName.nonTestSourceSetName(sourceSets: SourceSets): SourceSetName {

  return sourceSets.getOrElse(this) { sourceSets.getValue(SourceSetName.MAIN) }
    .withUpstream()
    .sortedByDescending { sourceSets.getValue(it).upstream.size }
    .firstOrNull { upstream -> !upstream.isTestingOnly(sourceSets) }
    .requireNotNull {
      val possible = sourceSets.getValue(this)
        .withUpstream()
        .sortedByDescending { sourceSets.getValue(it).upstream.size }

      "Could not find a non-test source set out of $possible"
    }
}

fun SourceSetName.javaConfigurationNames(): List<ConfigurationName> {

  return if (this == SourceSetName.MAIN) {
    ConfigurationName.main()
  } else {
    ConfigurationName.mainConfigurations
      .filterNot { it.asConfigurationName().isKapt() }
      .map { "${this.value}${it.capitalize()}".asConfigurationName() }
      .plus(kaptVariant())
  }
}

fun SourceSetName.apiConfig(): ConfigurationName {
  return if (this == SourceSetName.MAIN) {
    ConfigurationName.api
  } else {
    "${value}Api".asConfigurationName()
  }
}

fun SourceSetName.implementationConfig(): ConfigurationName {
  return if (this == SourceSetName.MAIN) {
    ConfigurationName.implementation
  } else {
    "${value}Implementation".asConfigurationName()
  }
}

/**
 * @return the 'kapt' name for this source set, such as `kapt`, `kaptTest`, or `kaptAndroidTest`
 * @since 0.12.0
 */
fun SourceSetName.kaptVariant(): ConfigurationName {
  return if (this == SourceSetName.MAIN) {
    ConfigurationName.kapt
  } else {
    "${ConfigurationName.kapt.value}${value.capitalize()}".asConfigurationName()
  }
}
