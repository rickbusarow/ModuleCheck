package com.rickbusarow.modulecheck.internal

import org.jetbrains.kotlin.psi.KtProperty

fun KtProperty.hasAnnotation(textWithAt: String) = annotationEntries.any { it.text == textWithAt }
