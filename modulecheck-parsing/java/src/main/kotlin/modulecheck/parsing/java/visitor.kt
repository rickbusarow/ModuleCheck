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

package modulecheck.parsing.java

import com.github.javaparser.ast.Node
import com.github.javaparser.ast.body.EnumConstantDeclaration
import com.github.javaparser.ast.body.MethodDeclaration
import com.github.javaparser.ast.body.TypeDeclaration
import com.github.javaparser.ast.body.VariableDeclarator
import com.github.javaparser.ast.expr.SimpleName
import com.github.javaparser.resolution.Resolvable
import com.github.javaparser.resolution.declarations.ResolvedDeclaration

internal inline fun Node.visit(
  crossinline predicate: (node: Node) -> Boolean
) {

  childrenRecursive()
    .takeWhile { predicate(it) }
}

inline fun <reified T : Node> Node.getChildOfType(): T? {
  return getChildrenOfType<T>().singleOrNull()
}

inline fun <reified T : Node> Node.requireChildOfType(): T {
  return getChildrenOfType<T>().single()
}

inline fun <reified T : Node> Node.getChildrenOfType(): List<T> {
  return childNodes.filterIsInstance<T>()
}

internal inline fun <reified T : Node> Node.getParentOfType(): T? {
  val parent = parentNode.getOrNull() ?: return null
  return generateSequence(parent) { p ->
    p.parentNode.getOrNull()
  }
    .filterIsInstance<T>()
    .firstOrNull()
}

internal inline fun <reified T : Node> Node.getParentsOfTypeRecursive(): Sequence<T> {
  val parent = parentNode.getOrNull() ?: return emptySequence()
  return generateSequence(parent) { p ->
    p.parentNode.getOrNull()
  }
    .filterIsInstance<T>()
}

internal fun Node.getParentsRecursive(): Sequence<Node> {
  val parent = parentNode.getOrNull() ?: return emptySequence()
  return generateSequence(parent) { p ->
    p.parentNode.getOrNull()
  }
}

fun Node.childrenRecursive(): Sequence<Node> {
  return generateSequence(childNodes.asSequence()) { children ->
    children.toList()
      .flatMap { it.childNodes }
      .takeIf { it.isNotEmpty() }
      ?.asSequence()
  }
    .flatten()
}

inline fun <reified T : Node> Node.getChildrenOfTypeRecursive(): Sequence<T> {
  return childrenRecursive()
    .filterIsInstance<T>()
}

fun <T, R : ResolvedDeclaration> T.fqNameOrNull(
  typeDeclarations: List<TypeDeclaration<*>>
): String?
  where T : Node, T : Resolvable<R> {
  val simpleName = when (this) {
    is MethodDeclaration -> name.asString()
    is VariableDeclarator -> nameAsString
    is EnumConstantDeclaration -> nameAsString
    else -> {

      kotlin.runCatching { resolve().name }
        .getOrNull()
        ?: simpleName()
    }
  }

  val parentTypeFqName = typeDeclarations
    .last { isDescendantOf(it) }
    .fullyQualifiedName.getOrNull() ?: return null
  return "$parentTypeFqName.$simpleName"
}

fun <T : Node> T.simpleName(): String {

  return if (this is SimpleName) {
    asString()
  } else {
    getChildrenOfTypeRecursive<SimpleName>()
      .first()
      .asString()
  }
}
