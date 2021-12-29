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

import com.github.javaparser.ast.type.ClassOrInterfaceType
import com.github.javaparser.resolution.UnsolvedSymbolException
import kotlinx.coroutines.flow.asFlow
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.map
import modulecheck.parsing.gradle.SourceSetName
import modulecheck.project.McProject
import modulecheck.utils.remove
import org.jetbrains.kotlin.name.FqName

class JavaParserNodeResolver(
  private val project: McProject,
  private val sourceSetName: SourceSetName
) {
  suspend fun fqNameOrNull(
    node: ClassOrInterfaceType,
    packageName: String,
    directImports: Collection<String>,
    wildcardImports: Collection<String>
  ): FqName? {

    val nameWithScope = node.nameWithScope

    directImports.singleOrNull { it.endsWith(nameWithScope) }
      ?.let { return FqName(it) }

    nameWithScope.javaLangFqNameOrNull()?.let { return it }

    wildcardImports.fold(
      sequenceOf(nameWithScope, "$packageName.$nameWithScope")
    ) { acc, wildcard ->
      acc + wildcard.remove(".*").plus(".$nameWithScope")
    }
      .asFlow()
      .map { project.resolveFqNameOrNull(FqName(it), sourceSetName) }
      .firstOrNull()
      ?.let { return it }

    return node.fqNameOrNull()
  }
}

internal fun String.javaLangFqNameOrNull(): FqName? {

  val maybeJavaLang = "java.lang.$this"

  return if (maybeJavaLang in javaStdLibNames) {
    FqName(maybeJavaLang)
  } else {
    null
  }
}

@Suppress("SwallowedException")
internal fun ClassOrInterfaceType.fqNameOrNull(): FqName? {

  return try {
    resolve()?.qualifiedName
      ?.let { name -> FqName(name) }
  } catch (e: UnsolvedSymbolException) {
    return null
  } catch (e: UnsupportedOperationException) {
    return null
  }
}
