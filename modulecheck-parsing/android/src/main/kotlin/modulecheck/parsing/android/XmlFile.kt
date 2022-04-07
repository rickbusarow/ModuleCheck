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

package modulecheck.parsing.android

import modulecheck.parsing.source.HasReferences
import modulecheck.parsing.source.Reference
import modulecheck.parsing.source.Reference.ExplicitXmlReference
import modulecheck.parsing.source.UnqualifiedAndroidResourceDeclaredName
import modulecheck.parsing.source.UnqualifiedAndroidResourceReference
import modulecheck.utils.LazySet
import modulecheck.utils.asDataSource
import modulecheck.utils.dataSource
import modulecheck.utils.mapToSet
import modulecheck.utils.toLazySet
import java.io.File

interface XmlFile : HasReferences {

  val file: File

  val resourceReferencesAsRReferences: Set<String>

  data class LayoutFile(
    override val file: File
  ) : XmlFile {

    val name: String = file.nameWithoutExtension

    val customViews: Lazy<Set<Reference>> = lazy {
      AndroidLayoutParser().parseViews(file).mapToSet { ExplicitXmlReference(it) }
    }

    private val attributes by lazy {
      AndroidLayoutParser().parseResources(file)
    }

    val idDeclarations: Set<UnqualifiedAndroidResourceDeclaredName> by lazy {
      attributes.filter { attribute ->
        attribute.startsWith("@+id/")
      }
        .mapNotNull { UnqualifiedAndroidResourceDeclaredName.fromString(it) }
        .toSet()
    }

    private val rawResources: Set<String> by lazy {
      attributes
        .filter { attribute ->
          REFERENCE_PREFIXES
            .any { attribute.startsWith(it) }
        }
        .toSet()
    }

    override val resourceReferencesAsRReferences: Set<String> by lazy {
      rawResources
        .mapNotNull { UnqualifiedAndroidResourceDeclaredName.fromString(it) }
        .map { it.name }
        .toSet()
    }

    override val references: LazySet<Reference> = listOf(
      customViews.asDataSource(),
      dataSource {
        resourceReferencesAsRReferences.mapToSet { UnqualifiedAndroidResourceReference(it) }
      }
    ).toLazySet()
  }

  data class ManifestFile(
    override val file: File
  ) : XmlFile {

    val basePackage: String? by lazy {
      AndroidManifestParser().parse(file)["package"]
    }

    private val rawResources: Set<String> by lazy {
      AndroidManifestParser().parseResources(file)
        .filter { attribute -> REFERENCE_PREFIXES.any { attribute.startsWith(it) } }
        .toSet()
    }

    private val declarations by lazy {
      rawResources
        .mapNotNull { UnqualifiedAndroidResourceDeclaredName.fromString(it) }
    }

    override val resourceReferencesAsRReferences: Set<String> by lazy {
      declarations.mapToSet { it.name }
    }

    override val references: LazySet<Reference> = listOf(
      dataSource {
        resourceReferencesAsRReferences.mapToSet { UnqualifiedAndroidResourceReference(it) }
      }
    ).toLazySet()
  }

  companion object {
    val REFERENCE_PREFIXES = listOf(
      "@anim/",
      "@animator/",
      "@arrays/",
      "@color/",
      "@dimen/",
      "@drawable/",
      "@font/",
      "@layout/",
      "@menu/",
      "@mipmap/",
      "@raw/",
      "@string/",
      "@style/"
    )
  }
}
