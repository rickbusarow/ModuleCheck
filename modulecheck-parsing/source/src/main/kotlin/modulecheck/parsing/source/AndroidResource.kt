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

package modulecheck.parsing.source

import java.io.File

sealed class AndroidResource(val prefix: String) {

  abstract val name: String

  data class AndroidString(override val name: String) : AndroidResource("string")
  data class Anim(override val name: String) : AndroidResource("anim")
  data class Animator(override val name: String) : AndroidResource("animator")
  data class Arrays(override val name: String) : AndroidResource("arrays")
  data class Color(override val name: String) : AndroidResource("color")
  data class Dimen(override val name: String) : AndroidResource("dimen")
  data class Drawable(override val name: String) : AndroidResource("drawable")
  data class Font(override val name: String) : AndroidResource("font")
  data class Layout(override val name: String) : AndroidResource("layout")
  data class Menu(override val name: String) : AndroidResource("menu")
  data class Mipmap(override val name: String) : AndroidResource("mipmap")
  data class Raw(override val name: String) : AndroidResource("raw")
  data class Style(override val name: String) : AndroidResource("style")

  companion object {

    private val REGEX = """"?@(.*)/(.*)"?""".toRegex()

    @Suppress("ComplexMethod")
    fun fromFile(file: File): AndroidResource? {
      val dir = file.parent.split('/').last()
      val name = file.nameWithoutExtension

      return when {
        dir.startsWith("anim") -> Anim(name)
        dir.startsWith("animator") -> Animator(name)
        dir.startsWith("arrays") -> Arrays(name)
        dir.startsWith("color") -> Color(name)
        dir.startsWith("dimen") -> Dimen(name)
        dir.startsWith("drawable") -> Drawable(name)
        dir.startsWith("font") -> Font(name)
        dir.startsWith("layout") -> Layout(name)
        dir.startsWith("menu") -> Menu(name)
        dir.startsWith("mipmap") -> Mipmap(name)
        dir.startsWith("raw") -> Raw(name)
        dir.startsWith("string") -> AndroidString(name)
        dir.startsWith("style") -> Style(name)
        dir.startsWith("values") -> Style(name)
        dir.startsWith("xml") -> Style(name)
        else -> null
      }
    }

    @Suppress("ComplexMethod")
    fun fromValuePair(type: String, name: String): AndroidResource? {
      return when (type) {
        "anim" -> Anim(name)
        "animator" -> Animator(name)
        "arrays" -> Arrays(name)
        "color" -> Color(name)
        "dimen" -> Dimen(name)
        "drawable" -> Drawable(name)
        "font" -> Font(name)
        "layout" -> Layout(name)
        "menu" -> Menu(name)
        "mipmap" -> Mipmap(name)
        "raw" -> Raw(name)
        "string" -> AndroidString(name)
        "style" -> Style(name)
        else -> null
      }
    }

    @Suppress("ComplexMethod")
    fun fromString(str: String): AndroidResource? {
      val (prefix, name) = REGEX.find(str)?.destructured ?: return null

      return fromValuePair(prefix, name)
    }
  }
}
