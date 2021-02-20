package modulecheck.core

import modulecheck.api.Finding
import modulecheck.psi.internal.asKtsFileOrNull
import org.jetbrains.kotlin.psi.KtFile

fun Finding.kotlinBuildFileOrNull(): KtFile? = dependentProject.buildFile.asKtsFileOrNull()
