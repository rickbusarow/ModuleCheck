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

package modulecheck.parsing.anvil

import modulecheck.parsing.psi.internal.findAnnotationArgument
import modulecheck.parsing.source.AnvilScope
import modulecheck.parsing.source.DeclarationName
import modulecheck.parsing.source.KotlinFile
import modulecheck.project.McProject
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.psi.KtAnnotationEntry
import org.jetbrains.kotlin.psi.KtClassLiteralExpression
import org.jetbrains.kotlin.psi.KtClassOrObject

data class AnvilContributedBinding(
  val declarationName: DeclarationName,
  val anvilScope: AnvilScope,
  val boundType: DeclarationName,
  val replaces: List<DeclarationName>
)

fun KtClassOrObject.boundTypeOrNull(
  annotationFqName: FqName,
  annotationEntry: KtAnnotationEntry,
  kotlinFile: KotlinFile,
  clazz: KtClassOrObject,
  project: McProject
): FqName? {

  val fromAnnotationOrNull = annotationEntry
    .findAnnotationArgument<KtClassLiteralExpression>("boundType", 1)

  val fromSuperType = clazz.superTypeListEntries.singleOrNull()
    ?: throw Exception("There's no supertype or too many????????????????????????????")

  fromSuperType.typeReference
  TODO()
}
