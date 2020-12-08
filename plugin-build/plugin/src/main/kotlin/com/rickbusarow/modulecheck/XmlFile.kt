package com.rickbusarow.modulecheck

import java.io.File

sealed class XmlFile {


  data class LayoutFile(val file: File) : XmlFile() {

    val customViews: Set<String> by  lazy {
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
