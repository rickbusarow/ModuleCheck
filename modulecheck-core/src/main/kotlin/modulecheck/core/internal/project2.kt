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

package modulecheck.core.internal

import modulecheck.api.ConfigurationName
import modulecheck.api.Finding.Position
import modulecheck.api.Project2
import modulecheck.api.util.positionOf
import modulecheck.psi.DslBlockVisitor
import modulecheck.psi.ProjectDependencyDeclarationVisitor
import modulecheck.psi.PsiElementWithSurroundingText
import modulecheck.psi.internal.asKtsFileOrNull
import org.jetbrains.kotlin.psi.KtCallExpression
import java.io.File

fun Project2.psiElementIn(
  parentBuildFile: File,
  configuration: ConfigurationName
): PsiElementWithSurroundingText? {
  val kotlinBuildFile = parentBuildFile.asKtsFileOrNull() ?: return null

  val result = DslBlockVisitor("dependencies")
    .parse(kotlinBuildFile)
    ?: return null

  val p = ProjectDependencyDeclarationVisitor(configuration, path)

  return result.elements
    .firstOrNull { element ->

      p.find(element.psiElement as KtCallExpression)
    }
}

fun Project2.positionIn(
  parentBuildFile: File,
  configuration: ConfigurationName
): Position? = parentBuildFile
  .readText()
  .lines()
  .positionOf(this, configuration)
