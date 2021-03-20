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

import modulecheck.api.Finding
import modulecheck.api.KaptMatcher
import modulecheck.api.Project2
import modulecheck.api.settings.*
import modulecheck.core.rule.ModuleCheckRule
import modulecheck.core.rule.ModuleCheckRuleFactory
import modulecheck.gradle.task.ModuleCheckAllTask
import modulecheck.gradle.task.ModuleCheckTask
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.model.ObjectFactory
import org.gradle.api.provider.ListProperty
import org.gradle.api.provider.Property
import org.gradle.api.provider.SetProperty
import org.gradle.api.tasks.Internal
import org.gradle.kotlin.dsl.*
import javax.inject.Inject

fun Project.moduleCheck(config: ModuleCheckExtension.() -> Unit) {
  extensions.configure(ModuleCheckExtension::class, config)
}

typealias GradleProject = Project

class ModuleCheckPlugin : Plugin<Project> {

  override fun apply(target: Project) {
    val settings = target.extensions.create("moduleCheck", ModuleCheckExtension::class.java)

    val factory = ModuleCheckRuleFactory()

    // AnvilFactoryRule is defined in this module, so it can't be statically registered like the others
    factory.register { AnvilFactoryRule(it) }

    val rules = factory.create(settings)

    rules
      .onEach { rule ->
        target.tasks.register("moduleCheck${rule.id}", DynamicModuleCheckTask::class, rule)
      }

    target.tasks.register("moduleCheck", ModuleCheckAllTask::class.java, rules)
  }
}

abstract class DynamicModuleCheckTask<T : Finding> @Inject constructor(
  @Internal val rule: ModuleCheckRule<T>
) : ModuleCheckTask() {

  init {
    description = rule.description
  }

  override fun List<Project2>.getFindings(): List<T> {
    return flatMap { project ->
      rule.check(project)
    }
  }
}

@Suppress("UnstableApiUsage")
open class ModuleCheckExtension @Inject constructor(
  private val objects: ObjectFactory
) : ModuleCheckSettings {

  @get:Internal
  val autoCorrectProp: Property<Boolean> = objects.property<Boolean>().convention(true)
  override var autoCorrect: Boolean
    @Internal get() = autoCorrectProp.get()
    set(value) = autoCorrectProp.set(value)

  @get:Internal
  val alwaysIgnoreProp: SetProperty<String> = objects.setProperty<String>().convention(emptySet())
  override var alwaysIgnore: Set<String>
    @Internal get() = alwaysIgnoreProp.get()
    set(value) = alwaysIgnoreProp.set(value)

  @get:Internal
  val ignoreAllProp: SetProperty<String> = objects.setProperty<String>().convention(emptySet())
  override var ignoreAll: Set<String>
    @Internal get() = ignoreAllProp.get()
    set(value) = ignoreAllProp.set(value)

  @get:Internal
  val additionalKaptMatchersProp: ListProperty<KaptMatcher> =
    objects.listProperty<KaptMatcher>().convention(emptyList())
  override var additionalKaptMatchers: List<KaptMatcher>
    @Internal get() = additionalKaptMatchersProp.get()
    set(value) {
      additionalKaptMatchersProp.set(value)
    }

  @get:Internal
  val checksSettingsProp: Property<ChecksSettings> =
    objects.property<ChecksSettings>().convention(ChecksExtension())
  override var checks: ChecksSettings
    @Internal get() = checksSettingsProp.get()
    set(value) = checksSettingsProp.set(value)

  override fun checks(block: ChecksSettings.() -> Unit) = block.invoke(checks)

  @get:Internal
  val sortSettingsProp: Property<SortSettings> = objects.property<SortSettings>().convention(
    SortExtension()
  )
  override var sort: SortSettings
    @Internal get() = sortSettingsProp.get()
    set(value) = sortSettingsProp.set(value)

  override fun sort(block: SortSettings.() -> Unit) = block.invoke(sort)
}
