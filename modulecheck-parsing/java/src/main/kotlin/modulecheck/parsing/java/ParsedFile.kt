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

import com.github.javaparser.ast.CompilationUnit
import com.github.javaparser.ast.ImportDeclaration
import com.github.javaparser.ast.body.EnumConstantDeclaration
import com.github.javaparser.ast.body.FieldDeclaration
import com.github.javaparser.ast.body.MethodDeclaration
import com.github.javaparser.ast.body.TypeDeclaration
import com.github.javaparser.ast.type.ClassOrInterfaceType
import modulecheck.parsing.source.DeclarationName
import modulecheck.utils.mapToSet
import org.jetbrains.kotlin.name.FqName

internal data class ParsedFile(
  val packageFqName: String,
  val imports: List<ImportDeclaration>,
  val classOrInterfaceTypes: Set<FqName>,
  val typeDeclarations: List<TypeDeclaration<*>>,
  val fieldDeclarations: Set<DeclarationName>,
  val enumDeclarations: Set<DeclarationName>
) {
  companion object {
    fun fromCompilationUnitLazy(compilationUnit: CompilationUnit): Lazy<ParsedFile> {
      return lazy {
        val packageFqName = compilationUnit.packageDeclaration.getOrNull()?.nameAsString ?: ""
        val imports = compilationUnit.imports.orEmpty()

        val classOrInterfaceTypes = mutableSetOf<ClassOrInterfaceType>()
        val typeDeclarations = mutableListOf<TypeDeclaration<*>>()
        val memberDeclarations = mutableSetOf<DeclarationName>()
        val enumDeclarations = mutableSetOf<DeclarationName>()

        compilationUnit.childrenRecursive()
          .forEach { node ->

            when (node) {
              is ClassOrInterfaceType -> classOrInterfaceTypes.add(node)
              is TypeDeclaration<*> -> typeDeclarations.add(node)
              is MethodDeclaration -> {

                if (node.canBeImported()) {
                  memberDeclarations.add(DeclarationName(node.fqName(typeDeclarations)))
                }
              }
              is FieldDeclaration -> {
                if (node.canBeImported()) {
                  memberDeclarations.add(DeclarationName(node.fqName(typeDeclarations)))
                }
              }
              is EnumConstantDeclaration -> {
                enumDeclarations.add(DeclarationName(node.fqName(typeDeclarations)))
              }
            }
          }

        ParsedFile(
          packageFqName = packageFqName,
          imports = imports,
          classOrInterfaceTypes = classOrInterfaceTypes.mapToSet { FqName(it.nameWithScope) },
          typeDeclarations = typeDeclarations.distinct(),
          fieldDeclarations = memberDeclarations,
          enumDeclarations = enumDeclarations
        )
      }
    }
  }
}
