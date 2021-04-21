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

package modulecheck.api.files

import com.github.javaparser.StaticJavaParser
import com.github.javaparser.ast.ImportDeclaration
import com.github.javaparser.ast.Node
import com.github.javaparser.ast.NodeList
import com.github.javaparser.ast.body.TypeDeclaration
import com.github.javaparser.ast.type.ClassOrInterfaceType
import modulecheck.api.JvmFile
import modulecheck.psi.asDeclaractionName
import java.io.File

class JavaFile(val file: File) : JvmFile() {

  override val name = file.name

  data class ParsedFile(
    val packageFqName: String,
    val imports: NodeList<ImportDeclaration>,
    val classOrInterfaceTypes: Set<ClassOrInterfaceType>,
    val typeDeclarations: Set<TypeDeclaration<*>>
  )

  private val parsed by lazy {
    val unit = StaticJavaParser.parse(file)

    val classOrInterfaceTypes = mutableSetOf<ClassOrInterfaceType>()
    val typeDeclarations = mutableSetOf<TypeDeclaration<*>>()

    val iterator = NodeIterator { node ->

      when (node) {
        is ClassOrInterfaceType -> classOrInterfaceTypes.add(node)
        is TypeDeclaration<*> -> typeDeclarations.add(node)
      }

      true
    }

    iterator.explore(unit)

    ParsedFile(
      packageFqName = unit.packageDeclaration.get().nameAsString,
      imports = unit.imports,
      classOrInterfaceTypes = classOrInterfaceTypes,
      typeDeclarations = typeDeclarations
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
      .map { it.fullyQualifiedName.get().asDeclaractionName() }
      .toSet()
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
}

class NodeIterator(
  private val predicate: (node: Node) -> Boolean
) {

  fun explore(node: Node) {
    if (predicate(node)) {
      node.childNodes.forEach { child ->
        explore(child)
      }
    }
  }
}
