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

package modulecheck.name

import modulecheck.utils.lazy.unsafeLazy

/**
 * example: `com.example.R.string.app_name`
 *
 * @property androidRName the R declaration used when AGP generates this fully qualified resource
 * @property resourceName the resource declaration, like `string.app_name`
 */
class AndroidResourceNameWithRName(
  val androidRName: AndroidRName,
  val resourceName: UnqualifiedAndroidResourceName
) : NameWithPackageName, AndroidResourceName {
  override val packageName: PackageName
    get() = androidRName.packageName

  override val simpleNames: List<SimpleName> by unsafeLazy {
    androidRName.simpleNames + resourceName.simpleNames
  }
  override val prefix: SimpleName get() = resourceName.prefix
  override val identifier: SimpleName get() = resourceName.identifier
}
