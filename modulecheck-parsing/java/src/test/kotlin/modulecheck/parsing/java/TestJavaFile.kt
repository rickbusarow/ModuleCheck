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

import kotlinx.coroutines.runBlocking
import modulecheck.parsing.source.DeclaredName
import modulecheck.parsing.source.JavaFile
import modulecheck.parsing.source.Reference
import modulecheck.parsing.source.asExplicitJavaReference
import modulecheck.testing.trimmedShouldBe
import modulecheck.utils.LazyDeferred
import modulecheck.utils.LazySet.DataSource
import modulecheck.utils.lazyDeferred
import modulecheck.utils.mapToSet

data class TestJavaFile(
  override val name: String,
  override val packageFqName: String,
  val imports: Set<Reference>,
  override val declarations: Set<DeclaredName>,
  val interpretedReferences: Set<Reference>,
  val apiReferencesStrings: Set<String>
) : JavaFile {

  override val apiReferences: LazyDeferred<Set<Reference>> = lazyDeferred {
    apiReferencesStrings.mapToSet { it.asExplicitJavaReference() }
  }

  override val interpretedReferencesLazy: Lazy<Set<Reference>> = lazy { interpretedReferences }

  override val importsLazy: Lazy<Set<Reference>> = lazy { imports }

  override fun references(): List<DataSource<Reference>> {
    return super.references()
  }
}

interface JavaFileTestUtils {

  infix fun JavaFile.shouldBe(other: JavaFile)

  fun javaFile(
    name: String = "JavaFile.java",
    packageFqName: String = "com.test",
    imports: Set<String> = emptySet(),
    declarations: Set<String> = emptySet(),
    wildcardImports: Set<String> = emptySet(),
    interpretedReferences: Set<Reference> = emptySet(),
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
    interpretedReferences: Set<Reference>,
    apiReferences: Set<String>
  ): TestJavaFile = TestJavaFile(
    name = name,
    packageFqName = packageFqName,
    imports = imports.mapToSet { it.asExplicitJavaReference() },
    declarations = declarations.map { DeclaredName(it) }.toSet(),
    interpretedReferences = interpretedReferences,
    apiReferencesStrings = apiReferences
  )

  override fun JavaFile.toTestFile(): TestJavaFile = runBlocking {
    (this@toTestFile as? TestJavaFile)
      ?: TestJavaFile(
        name = name,
        packageFqName = packageFqName,
        imports = importsLazy.value,
        declarations = declarations,
        interpretedReferences = interpretedReferencesLazy.value,
        apiReferencesStrings = apiReferences.await().mapToSet { it.name }
      )
  }
}
