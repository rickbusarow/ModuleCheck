/*
 * Copyright (C) 2020 Rick Busarow
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

package com.rickbusarow.modulecheck

import com.rickbusarow.modulecheck.parser.android.AndroidLayoutParser
import java.io.File

sealed class XmlFile {

  data class LayoutFile(val file: File) : XmlFile() {

    val customViews: Set<String> by lazy {
      AndroidLayoutParser.parseViews(file)
        .map {
          it.split(".")
            .dropLast(1)
            .joinToString(".")
        }
        .toSet()
    }
    val resourceReferences: Set<String> by lazy {
      AndroidLayoutParser.parseResources(file)
        .filter { attribute -> PREFIXES.any { attribute.startsWith(it) } }
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
