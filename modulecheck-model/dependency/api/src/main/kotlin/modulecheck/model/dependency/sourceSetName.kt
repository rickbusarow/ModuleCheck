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

package modulecheck.model.dependency

import modulecheck.model.sourceset.SourceSetName
import modulecheck.utils.capitalize

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