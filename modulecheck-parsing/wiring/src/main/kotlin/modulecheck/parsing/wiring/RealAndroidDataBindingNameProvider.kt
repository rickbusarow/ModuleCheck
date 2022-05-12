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

package modulecheck.parsing.wiring

import modulecheck.api.context.androidDataBindingDeclarationsForSourceSetName
import modulecheck.api.context.classpathDependencies
import modulecheck.parsing.gradle.model.SourceSetName
import modulecheck.parsing.source.AndroidDataBindingDeclaredName
import modulecheck.parsing.source.internal.AndroidDataBindingNameProvider
import modulecheck.project.McProject
import modulecheck.project.isAndroid
import modulecheck.project.project
import modulecheck.utils.LazySet
import modulecheck.utils.emptyLazySet
import modulecheck.utils.toLazySet

class RealAndroidDataBindingNameProvider constructor(
  private val project: McProject,
  private val sourceSetName: SourceSetName
) : AndroidDataBindingNameProvider {

  override suspend fun get(): LazySet<AndroidDataBindingDeclaredName> {
    if (!project.isAndroid()) return emptyLazySet()

    val local = project.androidDataBindingDeclarationsForSourceSetName(sourceSetName)

    val transitive = project.classpathDependencies()
      .get(sourceSetName)
      .map { tpd ->
        tpd.contributed.project(project.projectCache)
          .androidDataBindingDeclarationsForSourceSetName(tpd.source.declaringSourceSetName())
      }

    return listOf(local)
      .plus(transitive)
      .toLazySet()
  }
}
