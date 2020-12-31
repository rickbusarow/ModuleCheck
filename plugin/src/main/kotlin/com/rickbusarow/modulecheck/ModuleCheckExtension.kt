/*
 * Copyright (C) 2020 Rick Busarow
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

package com.rickbusarow.modulecheck

import com.rickbusarow.modulecheck.parser.KaptMatcher
import org.gradle.api.model.ObjectFactory
import org.gradle.api.provider.ListProperty
import org.gradle.api.provider.SetProperty
import org.gradle.kotlin.dsl.listProperty
import org.gradle.kotlin.dsl.setProperty

@Suppress("UnnecessaryAbstractClass")
abstract class ModuleCheckExtension(objects: ObjectFactory) {

  val alwaysIgnore: SetProperty<String> = objects.setProperty()
  val ignoreAll: SetProperty<String> = objects.setProperty()

  val additionalKaptMatchers: ListProperty<KaptMatcher> = objects.listProperty()
}
