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

package modulecheck.gradle

import modulecheck.api.*
import modulecheck.api.settings.*
import org.gradle.api.model.ObjectFactory
import org.gradle.api.provider.ListProperty
import org.gradle.api.provider.Property
import org.gradle.api.provider.SetProperty
import org.gradle.kotlin.dsl.listProperty
import org.gradle.kotlin.dsl.property
import org.gradle.kotlin.dsl.setProperty

@Suppress("UnnecessaryAbstractClass")
abstract class ModuleCheckExtension(objects: ObjectFactory) {

  @Suppress("UnstableApiUsage")
  val autoCorrect: Property<Boolean> = objects.property<Boolean>().convention(true)

  val alwaysIgnore: SetProperty<String> = objects.setProperty()

  val ignoreAll: SetProperty<String> = objects.setProperty()

  val additionalKaptMatchers: ListProperty<KaptMatcher> = objects.listProperty()

  val checks: Property<ChecksExtension> =
    objects.property<ChecksExtension>().convention(ChecksExtension())

  fun checks(block: ChecksExtension.() -> Unit) {
    checks.get().block()
  }
}

@Suppress("UnstableApiUsage")
class ChecksExtension : ChecksSettings {
  override var overshot: Boolean = true
  override var redundant: Boolean = false
  override var unused: Boolean = true
  override var mustBeApi: Boolean = true
  override var used: Boolean = false
  override var sortDependencies: Boolean = false
  override var sortPlugins: Boolean = false
  override var kapt: Boolean = true
  override var anvilFactories: Boolean = true
  override var disableAndroidResources: Boolean = false
  override var disableViewBinding: Boolean = false
}
