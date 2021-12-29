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

package modulecheck.parsing.psi.internal

import modulecheck.parsing.gradle.SourceSetName
import modulecheck.parsing.psi.kotlinStdLibNames
import modulecheck.parsing.source.KotlinFile
import modulecheck.project.McProject
import org.jetbrains.kotlin.com.intellij.psi.PsiElement
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.psi.KtAnnotated
import org.jetbrains.kotlin.psi.KtAnnotationEntry
import org.jetbrains.kotlin.psi.KtBlockExpression
import org.jetbrains.kotlin.psi.KtCallExpression
import org.jetbrains.kotlin.psi.KtClassLiteralExpression
import org.jetbrains.kotlin.psi.KtDotQualifiedExpression
import org.jetbrains.kotlin.psi.KtNameReferenceExpression
import org.jetbrains.kotlin.psi.KtNullableType
import org.jetbrains.kotlin.psi.KtPureElement
import org.jetbrains.kotlin.psi.KtScriptInitializer
import org.jetbrains.kotlin.psi.KtSuperTypeListEntry
import org.jetbrains.kotlin.psi.KtTypeArgumentList
import org.jetbrains.kotlin.psi.KtTypeReference
import org.jetbrains.kotlin.psi.KtUserType
import org.jetbrains.kotlin.psi.psiUtil.getChildOfType
import org.jetbrains.kotlin.psi.psiUtil.getNonStrictParentOfType
import org.jetbrains.kotlin.psi.psiUtil.parentsWithSelf
import java.io.File

inline fun <reified T : PsiElement> PsiElement.isPartOf() = getNonStrictParentOfType<T>() != null

inline fun <reified T : PsiElement> PsiElement.getChildrenOfTypeRecursive(): List<T> {
  return generateSequence(children.asSequence()) { children ->
    children.toList()
      .flatMap { it.children.toList() }
      .takeIf { it.isNotEmpty() }
      ?.asSequence()
  }
    .flatten()
    .filterIsInstance<T>()
    .toList()
}

fun KtAnnotated.hasAnnotation(file: KotlinFile, annotationFqName: FqName): Boolean {

  if (annotationEntries.any { it.typeReference?.typeElement?.text == annotationFqName.asString() }) {
    return true
  }

  val samePackage = annotationFqName.parent().asString() == file.packageFqName

  if (!samePackage && !file.imports.contains(annotationFqName.asString())) {
    return false
  }

  return annotationEntries
    .mapNotNull { it.typeReference?.typeElement?.text }
    .any { it == annotationFqName.shortName().asString() }
}

suspend fun McProject.canResolveFqName(
  declarationName: FqName,
  sourceSetName: SourceSetName
): Boolean {
  return resolveFqNameOrNull(declarationName, sourceSetName) != null
}

fun PsiElement.file(): File {
  val path = containingFile.virtualFile.path
  return File(path)
}

@Suppress("NestedBlockDepth", "ComplexMethod")
suspend fun PsiElement.fqNameOrNull(
  project: McProject,
  sourceSetName: SourceSetName
): FqName? {

  val containingKtFile = parentsWithSelf
    .filterIsInstance<KtPureElement>()
    .first()
    .containingKtFile

  val classReference = when (this) {
    // If a fully qualified name is used, then we're done and don't need to do anything further.
    // An inner class reference like Abc.Inner is also considered a KtDotQualifiedExpression in
    // some cases.
    is KtDotQualifiedExpression -> {
      project.resolveFqNameOrNull(FqName(text), sourceSetName)
        ?.let { return it }
        ?: text
    }
    is KtNameReferenceExpression -> getReferencedName()
    is KtUserType -> {
      val isGenericType = children.any { it is KtTypeArgumentList }
      if (isGenericType) {
        // For an expression like Lazy<Abc> the qualifier will be null. If the qualifier exists,
        // then it may refer to the package and the referencedName refers to the class name, e.g.
        // a KtUserType "abc.def.GenericType<String>" has three children: a qualifier "abc.def",
        // the referencedName "GenericType" and the KtTypeArgumentList.
        val qualifierText = qualifier?.text
        val className = referencedName

        if (qualifierText != null) {

          // The generic might be fully qualified. Try to resolve it and return early.
          project.resolveFqNameOrNull(FqName("$qualifierText.$className"), sourceSetName)
            ?.let { return it }

          // If the name isn't fully qualified, then it's something like "Outer.Inner".
          // We can't use `text` here because that includes the type parameter(s).
          "$qualifierText.$className"
        } else {
          className ?: return null
        }
      } else {
        val text = text

        // Sometimes a KtUserType is a fully qualified name. Give it a try and return early.
        if (text.contains(".") && text[0].isLowerCase()) {
          project.resolveFqNameOrNull(FqName(text), sourceSetName)
            ?.let { return it }
        }

        // We can't use referencedName here. For inner classes like "Outer.Inner" it would only
        // return "Inner", whereas text returns "Outer.Inner", what we expect.
        text
      }
    }
    is KtTypeReference -> {
      val children = children
      if (children.size == 1) {
        // Could be a KtNullableType or KtUserType.
        children[0].fqNameOrNull(project, sourceSetName)
          ?.let { return it } ?: text
      } else {
        text
      }
    }
    is KtNullableType -> return innerType?.fqNameOrNull(project, sourceSetName)
    is KtAnnotationEntry -> return typeReference?.fqNameOrNull(project, sourceSetName)
    is KtClassLiteralExpression -> {
      // Returns "Abc" for "Abc::class".
      return children.singleOrNull()
        ?.fqNameOrNull(project, sourceSetName)
    }
    is KtSuperTypeListEntry -> return typeReference?.fqNameOrNull(project, sourceSetName)
    else -> return null
  }

  // E.g. OuterClass.InnerClass
  val classReferenceOuter = classReference.substringBefore(".")

  val importPaths = containingKtFile.importDirectives.mapNotNull { it.importPath }

  // First look in the imports for the reference name. If the class is imported, then we know the
  // fully qualified name.
  importPaths
    .filter { it.alias == null && it.fqName.shortName().asString() == classReference }
    .also { matchingImportPaths ->
      when {
        matchingImportPaths.size == 1 ->
          return matchingImportPaths[0].fqName
        matchingImportPaths.size > 1 ->
          return matchingImportPaths.firstOrNull { importPath ->
            project.canResolveFqName(importPath.fqName, sourceSetName)
          }?.fqName
      }
    }

  importPaths
    .filter { it.alias == null && it.fqName.shortName().asString() == classReferenceOuter }
    .also { matchingImportPaths ->
      when {
        matchingImportPaths.size == 1 ->
          return FqName("${matchingImportPaths[0].fqName.parent()}.$classReference")
        matchingImportPaths.size > 1 ->
          return matchingImportPaths.firstOrNull { importPath ->
            project.canResolveFqName(
              importPath.fqName.child(Name.identifier(classReference)),
              sourceSetName
            )
          }?.fqName
      }
    }

  containingKtFile.importDirectives
    .asSequence()
    .filter { it.isAllUnder }
    .mapNotNull {
      // This fqName is everything in front of the star, e.g. for "import java.io.*" it
      // returns "java.io".
      it.importPath?.fqName
    }
    .forEach { importFqName ->
      project.resolveFqNameOrNull(
        importFqName.child(Name.identifier(classReference)),
        sourceSetName
      )
        ?.let { return it }
    }

  // If there is no import, then try to resolve the class with the same package as this file.
  project.resolveFqNameOrNull(
    containingKtFile.packageFqName.child(Name.identifier(classReference)),
    sourceSetName
  )
    ?.let { return it }

  // Check if it's a named import.
  containingKtFile.importDirectives
    .firstOrNull { classReference == it.importPath?.importedName?.asString() }
    ?.importedFqName
    ?.let { return it }

  // If this doesn't work, then maybe a class from the Kotlin package is used.
  sequenceOf(
    "kotlin.jvm.$classReference",
    "java.lang.$classReference",
    "kotlin.$classReference",
    "kotlin.collections.$classReference"
  )
    .firstOrNull { it in kotlinStdLibNames }
    ?.let { return FqName(it) }

  return null
}

fun KtCallExpression.nameSafe(): String? {
  return getChildOfType<KtNameReferenceExpression>()?.text
}

fun KtBlockExpression.nameSafe(): String? {

  val call: KtCallExpression? = getChildOfType<KtScriptInitializer>()
    ?.getChildOfType()
    ?: getChildOfType()

  call?.getChildOfType<KtNameReferenceExpression>()
    ?.text
    ?.let { simpleName -> return simpleName }

  val dotQualified: KtDotQualifiedExpression? = getChildOfType<KtScriptInitializer>()
    ?.getChildOfType()
    ?: getChildOfType()

  dotQualified?.let { dot ->

    val sel = dot.selectorExpression
      ?.getChildOfType<KtNameReferenceExpression>()
      ?.text

    return "${dot.receiverExpression.text}.$sel"
  }

  return getChildOfType<KtBlockExpression>()
    ?.getChildOfType<KtDotQualifiedExpression>()
    ?.text
}
