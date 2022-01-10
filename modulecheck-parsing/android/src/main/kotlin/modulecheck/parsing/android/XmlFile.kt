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

import modulecheck.parsing.source.AndroidResource
import java.io.File

interface XmlFile {

  val file: File

  data class LayoutFile(override val file: File) : XmlFile {

    val name: String = file.nameWithoutExtension

    val customViews: Set<String> by lazy {
      AndroidLayoutParser().parseViews(file)
        .toSet()
    }
    val resourceReferences: Set<String> by lazy {
      AndroidLayoutParser().parseResources(file)
        .filter { attribute -> PREFIXES.any { attribute.startsWith(it) } }
        .toSet()
    }

    val resourceReferencesAsRReferences: Set<String> by lazy {
      resourceReferences
        .mapNotNull { AndroidResource.fromString(it) }
        .map { "R.${it.prefix}.${it.name}" }
        .toSet()
    }
  }

  data class ManifestFile(override val file: File) : XmlFile {

    val resourceReferences: Set<String> by lazy {
      AndroidManifestParser().parseResources(file)
        .filter { attribute -> PREFIXES.any { attribute.startsWith(it) } }
        .toSet()
    }

    val resourceReferencesAsRReferences: Set<String> by lazy {
      resourceReferences
        .mapNotNull { AndroidResource.fromString(it) }
        .map { "R.${it.prefix}.${it.name}" }
        .toSet()
    }
  }

  companion object {
    val PREFIXES = listOf(
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
