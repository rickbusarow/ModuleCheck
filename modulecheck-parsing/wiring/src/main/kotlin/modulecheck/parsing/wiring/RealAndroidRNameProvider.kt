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

import modulecheck.api.context.androidRDeclaredNameForSourceSetName
import modulecheck.api.context.classpathDependencies
import modulecheck.model.sourceset.SourceSetName
import modulecheck.parsing.source.AndroidRDeclaredName
import modulecheck.parsing.source.internal.AndroidRNameProvider
import modulecheck.project.McProject
import modulecheck.project.isAndroid
import modulecheck.project.project
import modulecheck.utils.lazy.LazySet
import modulecheck.utils.lazy.dataSource
import modulecheck.utils.lazy.dataSourceOf
import modulecheck.utils.lazy.emptyLazySet
import modulecheck.utils.lazy.toLazySet
import modulecheck.utils.letIf
import modulecheck.utils.requireNotNull

class RealAndroidRNameProvider(
  private val project: McProject,
  private val sourceSetName: SourceSetName
) : AndroidRNameProvider {

  override suspend fun getLocalOrNull(): AndroidRDeclaredName? {
    return project.androidRDeclaredNameForSourceSetName(sourceSetName)
  }

  override suspend fun getAll(): LazySet<AndroidRDeclaredName> {
    if (!project.isAndroid()) return emptyLazySet()

    val localR = getLocalOrNull()

    return project.classpathDependencies()
      .get(sourceSetName)
      .map { transitiveDependency ->

        val transitiveSourceSetName = transitiveDependency.source
          .declaringSourceSetName(
            transitiveDependency.source.project(project).sourceSets
          )

        dataSource {
          setOfNotNull(
            transitiveDependency.contributed
              .project(project)
              .androidRDeclaredNameForSourceSetName(transitiveSourceSetName)
          )
        }
      }
      .letIf(localR != null) {
        it.plus(dataSourceOf(localR.requireNotNull()))
      }
      .toLazySet()
  }
}
