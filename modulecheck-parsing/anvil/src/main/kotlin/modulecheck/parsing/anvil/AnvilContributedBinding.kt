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

import modulecheck.parsing.psi.KotlinFile
import modulecheck.parsing.source.DeclarationName
import modulecheck.project.McProject
import modulecheck.project.temp.AnvilScopeNameEntry
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.psi.*

data class AnvilContributedBinding(
  val declarationName: DeclarationName,
  val anvilScopeNameEntry: AnvilScopeNameEntry,
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

public inline fun <reified T> KtAnnotationEntry.findAnnotationArgument(
  name: String,
  index: Int
): T? {
  val annotationValues = valueArguments
    .asSequence()
    .filterIsInstance<KtValueArgument>()

  // First check if the is any named parameter. Named parameters allow a different order of
  // arguments.
  annotationValues
    .firstNotNullOfOrNull { valueArgument ->
      val children = valueArgument.children
      if (children.size == 2 && children[0] is KtValueArgumentName &&
        (children[0] as KtValueArgumentName).asName.asString() == name &&
        children[1] is T
      ) {
        children[1] as T
      } else {
        null
      }
    }
    ?.let { return it }

  // If there is no named argument, then take the first argument, which must be a class literal
  // expression, e.g. @ContributesTo(Unit::class)
  return annotationValues
    .elementAtOrNull(index)
    ?.let { valueArgument ->
      valueArgument.children.firstOrNull() as? T
    }
}
