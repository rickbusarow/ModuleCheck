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

import com.github.javaparser.JavaParser
import com.github.javaparser.ParserConfiguration
import com.github.javaparser.ParserConfiguration.LanguageLevel
import com.github.javaparser.ast.CompilationUnit
import com.github.javaparser.ast.Node
import com.github.javaparser.ast.body.ClassOrInterfaceDeclaration
import com.github.javaparser.ast.body.FieldDeclaration
import com.github.javaparser.ast.body.MethodDeclaration
import com.github.javaparser.ast.nodeTypes.NodeWithTypeParameters
import com.github.javaparser.ast.nodeTypes.modifiers.NodeWithPrivateModifier
import com.github.javaparser.ast.nodeTypes.modifiers.NodeWithStaticModifier
import com.github.javaparser.ast.type.ClassOrInterfaceType
import com.github.javaparser.ast.type.Type
import com.github.javaparser.resolution.Resolvable
import com.github.javaparser.symbolsolver.JavaSymbolSolver
import com.github.javaparser.symbolsolver.resolution.typesolvers.CombinedTypeSolver
import com.github.javaparser.symbolsolver.resolution.typesolvers.ReflectionTypeSolver
import modulecheck.parsing.source.JavaFile
import modulecheck.parsing.source.JavaVersion
import modulecheck.parsing.source.JavaVersion.VERSION_11
import modulecheck.parsing.source.JavaVersion.VERSION_12
import modulecheck.parsing.source.JavaVersion.VERSION_13
import modulecheck.parsing.source.JavaVersion.VERSION_14
import modulecheck.parsing.source.JavaVersion.VERSION_15
import modulecheck.parsing.source.JavaVersion.VERSION_16
import modulecheck.parsing.source.JavaVersion.VERSION_17
import modulecheck.parsing.source.JavaVersion.VERSION_18
import modulecheck.parsing.source.JavaVersion.VERSION_19
import modulecheck.parsing.source.JavaVersion.VERSION_1_1
import modulecheck.parsing.source.JavaVersion.VERSION_1_10
import modulecheck.parsing.source.JavaVersion.VERSION_1_2
import modulecheck.parsing.source.JavaVersion.VERSION_1_3
import modulecheck.parsing.source.JavaVersion.VERSION_1_4
import modulecheck.parsing.source.JavaVersion.VERSION_1_5
import modulecheck.parsing.source.JavaVersion.VERSION_1_6
import modulecheck.parsing.source.JavaVersion.VERSION_1_7
import modulecheck.parsing.source.JavaVersion.VERSION_1_8
import modulecheck.parsing.source.JavaVersion.VERSION_1_9
import modulecheck.parsing.source.JavaVersion.VERSION_20
import modulecheck.parsing.source.JavaVersion.VERSION_HIGHER
import modulecheck.parsing.source.asDeclarationName
import modulecheck.utils.LazyDeferred
import modulecheck.utils.lazyDeferred
import modulecheck.utils.mapToSet
import org.jetbrains.kotlin.name.FqName
import java.io.File
import kotlin.LazyThreadSafetyMode.NONE
import kotlin.contracts.contract

class RealJavaFile(
  val file: File,
  private val javaVersion: JavaVersion
) : JavaFile {

  override val name = file.name

  private val parserConfiguration by lazy(NONE) {
    // Set up a minimal type solver that only looks at the classes used to run this sample.
    val combinedTypeSolver = CombinedTypeSolver()
      .apply {
        add(ReflectionTypeSolver())
        // TODO
        //  consider adding this with source dirs for all dependencies?
        //  add(JavaParserTypeSolver())
      }

    val symbolSolver = JavaSymbolSolver(combinedTypeSolver)

    ParserConfiguration()
      .apply {
        setSymbolResolver(symbolSolver)
        languageLevel = javaVersion.toLanguageLevel()
      }
  }

  private val compilationUnit: CompilationUnit by lazy {

    JavaParser(parserConfiguration)
      .parse(file)
      .result
      .get()
  }

  private val parsed by ParsedFile.fromCompilationUnitLazy(compilationUnit)

  override val packageFqName by lazy { parsed.packageFqName }

  override val imports by lazy {
    compilationUnit.imports
      .filterNot { it.isAsterisk }
      .map { it.nameAsString }
      .toSet()
  }

  override val declarations by lazy {
    parsed.typeDeclarations
      .asSequence()
      .filterNot { it.isPrivate }
      .mapNotNull { declaration ->
        declaration.fullyQualifiedName
          .getOrNull()
          ?.asDeclarationName()
      }
      .toSet()
      .plus(parsed.fieldDeclarations)
      .plus(parsed.enumDeclarations)
  }

  override val wildcardImports: Set<String> by lazy {
    compilationUnit.imports
      .filter { it.isAsterisk }
      .map { it.nameAsString }
      .toSet()
  }

  private val typeReferenceNames: Set<String> by lazy {

    compilationUnit
      .getChildrenOfTypeRecursive<ClassOrInterfaceType>()
      // A qualified type like `com.Foo` will have a nested ClassOrInterfaceType of `com`.
      // Filter out those nested types, since they seem like they're always just noise.
      .filterNot { it.parentNode.getOrNull() is ClassOrInterfaceType }
      .flatMap { it.typeReferencesRecursive() }
      .mapNotNull { type ->

        val typeNames = type.getTypeParameterNamesInScope().toSet()

        type.nameWithScope
          .takeIf { it !in typeNames }
      }
      .toSet()
  }

  override val maybeExtraReferences: LazyDeferred<Set<String>> = lazyDeferred {

    val unresolved = typeReferenceNames
      .filter { name -> imports.none { import -> import.endsWith(name) } }
      .filter { name -> name.javaLangFqNameOrNull() == null }

    val all = unresolved + unresolved.flatMap { reference ->
      reference.javaLangFqNameOrNull()?.let { listOf(it.asString()) }
        ?: (wildcardImports.map { "$it.$reference" } + "$packageFqName.$reference")
    }

    all.toSet()
  }

  override val apiReferences: Set<FqName> by lazy {

    val members = compilationUnit.childrenRecursive()
      // Only look at references which are inside public classes.  This includes nested classes
      // which may be (incorrectly) inside private or package-private classes.
      .filter { node ->
        node.getParentsOfTypeRecursive<ClassOrInterfaceDeclaration>()
          .all { parentClass -> parentClass.isPublic || parentClass.isProtected }
      }

    val simpleRefs = members
      .flatMap {
        when (it) {
          is MethodDeclaration -> it.apiReferences()
          is FieldDeclaration -> it.apiReferences()
          else -> emptyList()
        }
      }
      .toList()

    val resolved = mutableSetOf<String>()
    val unresolved = mutableSetOf<String>()

    simpleRefs.forEach { reference ->

      val resolvedOrNull = imports.firstOrNull { it.endsWith(reference) }
        ?: reference.javaLangFqNameOrNull()?.asString()

      if (resolvedOrNull != null) {
        resolved.add(resolvedOrNull)
        return@forEach
      }

      unresolved.add(reference)
    }

    val guesses = unresolved + unresolved.flatMap { reference ->
      reference.javaLangFqNameOrNull()?.let { listOf(it.asString()) }
        ?: (wildcardImports.map { "$it.$reference" } + "$packageFqName.$reference")
    }

    (resolved + guesses)
      .mapToSet { FqName(it) }
  }
}

/**
 * Includes all types referenced by the receiver [ClassOrInterfaceType], optionally including
 * itself.
 *
 * For instance, given the function:
 *
 * ```
 * public javax.inject.Provider<List<String>> getStringListProvider() { /* ... */ }
 * ```
 *
 * This function with will return a sequence of ['javax.inject.Provider', 'List', 'String'].
 *
 * @return A Sequence of all [Type]s referenced by the receiver class type.
 */
fun ClassOrInterfaceType.typeReferencesRecursive(): Sequence<ClassOrInterfaceType> {

  return generateSequence(sequenceOf(this)) { types ->
    types.map { type ->
      type.typeArguments
        ?.getOrNull()
        ?.asSequence()
        ?.filterIsInstance<ClassOrInterfaceType>()
        .orEmpty()
    }
      .flatten()
      .takeIf { it.iterator().hasNext() }
  }
    .flatten()
}

fun FieldDeclaration.apiReferences(): List<String> {
  return (elementType as? ClassOrInterfaceType)?.typeReferencesRecursive()
    .orEmpty()
    .map { it.nameWithScope }
    .toList()
}

fun MethodDeclaration.apiReferences(): List<String> {

  if (!isProtected && !isPublic) return emptyList()

  val typeParameterBounds = typeParameters.flatMap { it.typeBound }
    .flatMap { it.typeReferencesRecursive() }
    .map { it.nameWithScope }
    .distinct()

  val typeParameterNames = typeParameters.mapToSet { it.nameAsString }

  val returnTypes: Sequence<String> = sequenceOf(type as? ClassOrInterfaceType)
    .filterNotNull()
    .plus(type.getChildrenOfTypeRecursive())
    .filterNot { it.parentNode.getOrNull() is ClassOrInterfaceType }
    .flatMap { classType ->
      classType.typeReferencesRecursive()
        .map { it.nameWithScope }
    }
    .filterNot { it in typeParameterNames }

  val arguments = parameters
    .map { it.type }
    .filterIsInstance<ClassOrInterfaceType>()
    .flatMap { classType ->
      classType.typeReferencesRecursive()
        .map { it.nameWithScope }
    }
    .filterNot { it in typeParameterNames }

  return typeParameterBounds + returnTypes + arguments
}

internal fun Node.getTypeParameterNamesInScope(): Sequence<String> {
  val parent = parentNode.getOrNull() ?: return emptySequence()
  return generateSequence(parent) { p ->
    p.parentNode.getOrNull()
  }
    .filterIsInstance<NodeWithTypeParameters<*>>()
    .flatMap { node -> node.typeParameters.map { it.nameAsString } }
}

fun <T> T.canBeImported(): Boolean
  where T : NodeWithStaticModifier<T>, T : NodeWithPrivateModifier<T> {

  contract {
    returns(true) implies (this@canBeImported is Resolvable<*>)
  }

  return isStatic() && !isPrivate() && this is Resolvable<*>
}

@Suppress("ComplexMethod")
internal fun JavaVersion.toLanguageLevel(): LanguageLevel {
  return when (this) {
    VERSION_1_1 -> LanguageLevel.JAVA_1_1
    VERSION_1_2 -> LanguageLevel.JAVA_1_2
    VERSION_1_3 -> LanguageLevel.JAVA_1_3
    VERSION_1_4 -> LanguageLevel.JAVA_1_4
    VERSION_1_5 -> LanguageLevel.JAVA_5
    VERSION_1_6 -> LanguageLevel.JAVA_6
    VERSION_1_7 -> LanguageLevel.JAVA_7
    VERSION_1_8 -> LanguageLevel.JAVA_8
    VERSION_1_9 -> LanguageLevel.JAVA_9
    VERSION_1_10 -> LanguageLevel.JAVA_10
    VERSION_11 -> LanguageLevel.JAVA_11
    VERSION_12 -> LanguageLevel.JAVA_12
    VERSION_13 -> LanguageLevel.JAVA_13
    VERSION_14 -> LanguageLevel.JAVA_14
    // TODO
    //  Gradle itself leaks JavaParser 3.17.0 to its classpath, so these later versions of Java
    //  won't resolve
    // VERSION_15 -> LanguageLevel.JAVA_15
    // VERSION_16 -> LanguageLevel.JAVA_16
    // VERSION_17 -> LanguageLevel.JAVA_17
    VERSION_15 -> LanguageLevel.CURRENT
    VERSION_16 -> LanguageLevel.CURRENT
    VERSION_17 -> LanguageLevel.CURRENT
    VERSION_18 -> LanguageLevel.CURRENT
    VERSION_19 -> LanguageLevel.CURRENT
    VERSION_20 -> LanguageLevel.CURRENT
    VERSION_HIGHER -> LanguageLevel.CURRENT
  }
}
