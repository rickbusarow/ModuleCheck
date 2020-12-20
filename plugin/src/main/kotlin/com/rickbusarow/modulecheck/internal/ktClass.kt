package com.rickbusarow.modulecheck.internal

import org.jetbrains.kotlin.psi.KtClass

private val viewModelMatcher = """[\s\S]*ViewModel[\s\S]*[(<][\s\S]*""".toRegex()

fun KtClass.isViewModel(): Boolean = superTypeListEntries.any {
  viewModelMatcher.matches(it.text)
}

private val fragmentMatcher = """[\s\S]*Fragment<[\s\S]*""".toRegex()

fun KtClass.isFragment(): Boolean = superTypeListEntries.any {
  fragmentMatcher.matches(it.text)
}

fun KtClass.replaceBody(newBodyText: String): KtClass {

  val oldBodyText = body?.text

  val newText = if (oldBodyText != null) {
    text.replace(oldBodyText, newBodyText)
  } else {
    text + newBodyText
  }

  return psiElementFactory.createClass(newText)
}

fun KtClass.removeSuperType(supertypeText: String): KtClass {

  val originalSuperText = this.getSuperTypeList()?.text ?: return this

  val toRemove = getSuperTypeList()?.entries?.firstOrNull { it.text == supertypeText }

  val new = getSuperTypeList()?.entries?.filterNot { it.text == toRemove?.text }
    ?.joinToString(",\n") { it.text } ?: ""

  val newText = text.replace(originalSuperText, new)

  return psiElementFactory.createClass(newText)
}
