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

package modulecheck.parsing.java

import com.github.javaparser.StaticJavaParser
import com.github.javaparser.ast.ImportDeclaration
import com.github.javaparser.ast.Node
import com.github.javaparser.ast.NodeList
import com.github.javaparser.ast.body.EnumConstantDeclaration
import com.github.javaparser.ast.body.FieldDeclaration
import com.github.javaparser.ast.body.TypeDeclaration
import com.github.javaparser.ast.expr.SimpleName
import com.github.javaparser.ast.type.ClassOrInterfaceType
import com.github.javaparser.resolution.Resolvable
import modulecheck.parsing.JvmFile
import modulecheck.project.DeclarationName
import modulecheck.project.asDeclarationName
import java.io.File
import kotlin.properties.Delegates

class JavaFile(val file: File) : JvmFile() {

  override val name = file.name

  data class ParsedFile(
    val packageFqName: String,
    val imports: NodeList<ImportDeclaration>,
    val classOrInterfaceTypes: Set<ClassOrInterfaceType>,
    val typeDeclarations: List<TypeDeclaration<*>>,
    val fieldDeclarations: Set<DeclarationName>,
    val enumDeclarations: Set<DeclarationName>
  )

  private val parsed by lazy {
    val unit = StaticJavaParser.parse(file)

    val classOrInterfaceTypes = mutableSetOf<ClassOrInterfaceType>()
    val typeDeclarations = mutableListOf<TypeDeclaration<*>>()
    val fieldDeclarations = mutableSetOf<DeclarationName>()
    val enumDeclarations = mutableSetOf<DeclarationName>()

    val iterator = NodeVisitor { node ->

      when (node) {
        is ClassOrInterfaceType -> classOrInterfaceTypes.add(node)
        is TypeDeclaration<*> -> typeDeclarations.add(node)
        is FieldDeclaration -> {

          if (node.isStatic && node.isPublic) {
            fieldDeclarations.add(DeclarationName(node.fqName(typeDeclarations)))
          }
        }
        is EnumConstantDeclaration -> {
          enumDeclarations.add(DeclarationName(node.fqName(typeDeclarations)))
        }
      }

      true
    }

    iterator.visit(unit)

    ParsedFile(
      packageFqName = unit.packageDeclaration.get().nameAsString,
      imports = unit.imports,
      classOrInterfaceTypes = classOrInterfaceTypes,
      typeDeclarations = typeDeclarations.distinct(),
      fieldDeclarations = fieldDeclarations,
      enumDeclarations = enumDeclarations
    )
  }

  override val packageFqName by lazy { parsed.packageFqName }

  override val imports by lazy {
    parsed
      .imports
      .map {
        it.toString()
          .replace("import", "")
          .replace(";", "")
          .trim()
      }.toSet()
  }

  override val declarations by lazy {
    parsed.typeDeclarations
      .map { it.fullyQualifiedName.get().asDeclarationName() }
      .toSet()
      .plus(parsed.fieldDeclarations)
      .plus(parsed.enumDeclarations)
  }

  override val wildcardImports: Set<String> by lazy {
    parsed
      .imports
      .filter { it.isAsterisk }
      .map {
        it.toString()
          .replace("import", "")
          .replace(";", "")
          .trim()
      }.toSet()
  }

  override val maybeExtraReferences: Set<String> by lazy {
    parsed.classOrInterfaceTypes
      .map { it.nameWithScope }
      .flatMap { name ->
        wildcardImports.map { wildcardImport ->
          wildcardImport.replace("*", name)
        }
      }
      .toSet()
  }

  private fun <T> T.fqName(typeDeclarations: List<TypeDeclaration<*>>): String
    where T : Node, T : Resolvable<*> {
    val simpleName = simpleName()

    val parentTypeFqName = typeDeclarations
      .last { isDescendantOf(it) }
      .fullyQualifiedName.get()
    return "$parentTypeFqName.$simpleName"
  }

  private fun <T> T.simpleName(): String
    where T : Node, T : Resolvable<*> {
    var name: String by Delegates.notNull()

    NodeVisitor { node ->
      if (node is SimpleName) {
        name = node.asString()
        false
      } else {
        true
      }
    }.visit(this)

    return name
  }
}

internal class NodeVisitor(
  private val predicate: (node: Node) -> Boolean
) {

  fun visit(node: Node) {
    if (predicate(node)) {
      node.childNodes.forEach { child ->
        visit(child)
      }
    }
  }
}
