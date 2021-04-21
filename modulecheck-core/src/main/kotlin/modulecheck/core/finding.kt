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

package modulecheck.core

import modulecheck.api.ConfiguredProjectDependency
import modulecheck.api.Finding
import modulecheck.api.Project2
import modulecheck.api.SourceSetName
import modulecheck.api.context.declarations
import modulecheck.api.context.resolvedReferencesForSourceSetName
import modulecheck.psi.DeclarationName
import modulecheck.psi.internal.asKtsFileOrNull
import org.jetbrains.kotlin.psi.KtFile

fun Finding.kotlinBuildFileOrNull(): KtFile? = buildFile.asKtsFileOrNull()

fun DeclarationName.resolveSourceOrNull(
  dependentProject: Project2,
  sourceSetName: SourceSetName
): ConfiguredProjectDependency? {
  return dependentProject
    .resolvedReferencesForSourceSetName(sourceSetName)
    .firstOrNull { cpd ->
      cpd
        .project
        .declarations[SourceSetName.MAIN]
        .orEmpty()
        .contains(this)
    }
}
