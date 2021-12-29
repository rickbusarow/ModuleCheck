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

import kotlinx.coroutines.runBlocking
import modulecheck.parsing.source.DeclarationName
import modulecheck.parsing.source.JavaFile
import modulecheck.testing.trimmedShouldBe
import modulecheck.utils.lazyDeferred
import modulecheck.utils.mapToSet
import org.jetbrains.kotlin.name.FqName

data class TestJavaFile(
  override val name: String,
  override val packageFqName: String,
  override val imports: Set<String>,
  override val declarations: Set<DeclarationName>,
  override val wildcardImports: Set<String>,
  val maybeExtraReferencesSet: Set<String>,
  val apiReferencesStrings: Set<String>
) : JavaFile {

  override val maybeExtraReferences = lazyDeferred { maybeExtraReferencesSet }

  override val apiReferences: Set<FqName> = apiReferencesStrings.mapToSet { FqName(it) }
}

interface JavaFileTestUtils {

  infix fun JavaFile.shouldBe(other: JavaFile)

  fun javaFile(
    name: String = "JavaFile.java",
    packageFqName: String = "com.test",
    imports: Set<String> = emptySet(),
    declarations: Set<String> = emptySet(),
    wildcardImports: Set<String> = emptySet(),
    maybeExtraReferences: Set<String> = emptySet(),
    apiReferences: Set<String> = emptySet()
  ): TestJavaFile

  fun JavaFile.toTestFile(): TestJavaFile
}

class RealJavaFileTestUtils : JavaFileTestUtils {

  override infix fun JavaFile.shouldBe(other: JavaFile) {

    this.toTestFile().trimmedShouldBe(other.toTestFile())
  }

  override fun javaFile(
    name: String,
    packageFqName: String,
    imports: Set<String>,
    declarations: Set<String>,
    wildcardImports: Set<String>,
    maybeExtraReferences: Set<String>,
    apiReferences: Set<String>
  ): TestJavaFile = TestJavaFile(
    name = name,
    packageFqName = packageFqName,
    imports = imports,
    declarations = declarations.map { DeclarationName(it) }.toSet(),
    wildcardImports = wildcardImports,
    maybeExtraReferencesSet = maybeExtraReferences,
    apiReferencesStrings = apiReferences
  )

  override fun JavaFile.toTestFile(): TestJavaFile = (this as? TestJavaFile)
    ?: runBlocking {
      TestJavaFile(
        name = name,
        packageFqName = packageFqName,
        imports = imports,
        declarations = declarations,
        wildcardImports = wildcardImports,
        maybeExtraReferencesSet = maybeExtraReferences.await(),
        apiReferencesStrings = apiReferences.mapToSet { it.asString() }
      )
    }
}
