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

@file:UseSerializers(FileAsStringSerializer::class)

package modulecheck.parsing.android

import kotlinx.serialization.Serializable
import kotlinx.serialization.UseSerializers
import modulecheck.parsing.source.HasReferences
import modulecheck.parsing.source.McName.CompatibleLanguage.XML
import modulecheck.parsing.source.PackageName
import modulecheck.parsing.source.ReferenceName
import modulecheck.parsing.source.UnqualifiedAndroidResource
import modulecheck.parsing.source.UnqualifiedAndroidResourceReferenceName
import modulecheck.utils.lazy.LazySet
import modulecheck.utils.lazy.asDataSource
import modulecheck.utils.lazy.dataSource
import modulecheck.utils.lazy.toLazySet
import modulecheck.utils.mapToSet
import modulecheck.utils.serialization.FileAsStringSerializer
import java.io.File

interface XmlFile : HasReferences {

  val file: File

  val resourceReferencesAsRReferences: Set<String>

  @Serializable
  data class LayoutFile(
    override val file: File
  ) : XmlFile {

    val name: String = file.nameWithoutExtension

    /**
     * All custom view types *used* within this file. Note that this is not the declaration of a
     * view.
     *
     * @since 0.12.0
     */
    val customViews: Lazy<Set<ReferenceName>> = lazy {
      AndroidLayoutParser().parseViews(file).mapToSet { ReferenceName(it, XML) }
    }

    private val attributes by lazy {
      AndroidLayoutParser().parseResources(file)
    }

    /**
     * Declared ids, using `@+id/________`, which are expressed as `R.id._____`
     *
     * @since 0.12.0
     */
    val idDeclarations: Set<UnqualifiedAndroidResource> by lazy {
      attributes.filter { attribute ->
        attribute.startsWith("@+id/")
      }
        .mapNotNull { UnqualifiedAndroidResource.fromXmlString(it) }
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
        .mapNotNull { UnqualifiedAndroidResource.fromXmlString(it) }
        .map { it.name }
        .toSet()
    }

    override val references: LazySet<ReferenceName> = listOf(
      customViews.asDataSource(),
      dataSource {
        resourceReferencesAsRReferences.mapToSet {
          UnqualifiedAndroidResourceReferenceName(it, XML)
        }
      }
    ).toLazySet()
  }

  @Serializable
  data class ManifestFile(
    override val file: File
  ) : XmlFile {

    val basePackage: PackageName? by lazy {
      AndroidManifestParser().parse(file)["package"]?.let { PackageName(it) }
    }

    private val rawResources: Set<String> by lazy {
      AndroidManifestParser().parseResources(file)
        .filter { attribute -> REFERENCE_PREFIXES.any { attribute.startsWith(it) } }
        .toSet()
    }

    private val declarations by lazy {
      rawResources
        .mapNotNull { UnqualifiedAndroidResource.fromXmlString(it) }
    }

    override val resourceReferencesAsRReferences: Set<String> by lazy {
      declarations.mapToSet { it.name }
    }

    override val references: LazySet<ReferenceName> = listOf(
      dataSource {
        resourceReferencesAsRReferences.mapToSet {
          UnqualifiedAndroidResourceReferenceName(it, XML)
        }
      }
    ).toLazySet()
  }

  companion object {
    val REFERENCE_PREFIXES: List<String> = listOf(
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
