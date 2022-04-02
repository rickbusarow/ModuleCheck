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

package modulecheck.parsing.source

import modulecheck.parsing.source.Reference.UnqualifiedAndroidResourceReference
import modulecheck.utils.safeAs
import modulecheck.utils.unsafeLazy
import java.io.File
import kotlin.io.path.name

sealed interface AndroidResourceDeclaredName : DeclaredName {
  val prefix: String
  val identifier: String
}

class GeneratedAndroidResourceDeclaredName(
  val sourceR: AndroidRDeclaredName,
  val sourceResource: UnqualifiedAndroidResourceDeclaredName
) : AndroidResourceDeclaredName,
  JavaCompatibleDeclaredName,
  KotlinCompatibleDeclaredName,
  XmlCompatibleDeclaredName {

  override val identifier: String
    get() = sourceResource.identifier
  override val prefix: String
    get() = sourceResource.prefix

  override val name: String by unsafeLazy {
    "${sourceR.name}.$prefix.${sourceResource.identifier}"
  }
}

sealed class UnqualifiedAndroidResourceDeclaredName(
  override val prefix: String
) : AndroidResourceDeclaredName {

  override val name: String by unsafeLazy { "R.$prefix.${this.identifier}" }

  class AndroidInteger(
    override val identifier: String
  ) : UnqualifiedAndroidResourceDeclaredName("integer")

  class AndroidString(
    override val identifier: String
  ) : UnqualifiedAndroidResourceDeclaredName("string")

  class Anim(override val identifier: String) : UnqualifiedAndroidResourceDeclaredName("anim")
  class Animator(
    override val identifier: String
  ) : UnqualifiedAndroidResourceDeclaredName("animator")

  class Arrays(override val identifier: String) : UnqualifiedAndroidResourceDeclaredName("array")
  class Bool(override val identifier: String) : UnqualifiedAndroidResourceDeclaredName("bool")
  class Color(override val identifier: String) : UnqualifiedAndroidResourceDeclaredName("color")
  class Dimen(override val identifier: String) : UnqualifiedAndroidResourceDeclaredName("dimen")
  class Drawable(
    override val identifier: String
  ) : UnqualifiedAndroidResourceDeclaredName("drawable")

  class Font(override val identifier: String) : UnqualifiedAndroidResourceDeclaredName("font")
  class ID(override val identifier: String) : UnqualifiedAndroidResourceDeclaredName("id")
  class Layout(override val identifier: String) : UnqualifiedAndroidResourceDeclaredName("layout")
  class Menu(override val identifier: String) : UnqualifiedAndroidResourceDeclaredName("menu")
  class Mipmap(override val identifier: String) : UnqualifiedAndroidResourceDeclaredName("mipmap")
  class Raw(override val identifier: String) : UnqualifiedAndroidResourceDeclaredName("raw")
  class Style(override val identifier: String) : UnqualifiedAndroidResourceDeclaredName("style")

  fun toNamespacedDeclaredName(
    androidRDeclaration: AndroidRDeclaredName
  ): GeneratedAndroidResourceDeclaredName {
    return GeneratedAndroidResourceDeclaredName(
      sourceR = androidRDeclaration,
      sourceResource = this
    )
  }

  override fun equals(other: Any?): Boolean {
    return matches(
      other,
      ifReference = { name == it.safeAs<UnqualifiedAndroidResourceReference>()?.name },
      ifDeclaration = { it::class == this::class && name == it.name }
    )
  }

  override fun hashCode(): Int = name.hashCode()

  override fun toString(): String = "(${this::class.java.simpleName}) `$name`"

  companion object {

    private val REGEX = """"?@(.*)/(.*)"?""".toRegex()

    fun prefixes() = listOf(
      "anim",
      "animator",
      "array",
      "bool",
      "color",
      "dimen",
      "drawable",
      "font",
      "id",
      "integer",
      "layout",
      "menu",
      "mipmap",
      "raw",
      "string",
      "style"
    )

    @Suppress("ComplexMethod")
    fun fromFile(file: File): UnqualifiedAndroidResourceDeclaredName? {
      val dir = file.toPath().parent?.name ?: return null
      val name = file.nameWithoutExtension

      return when {
        dir.startsWith("anim") -> Anim(name)
        dir.startsWith("animator") -> Animator(name)
        dir.startsWith("color") -> Color(name)
        dir.startsWith("dimen") -> Dimen(name)
        dir.startsWith("drawable") -> Drawable(name)
        dir.startsWith("font") -> Font(name)
        dir.startsWith("layout") -> Layout(name)
        dir.startsWith("menu") -> Menu(name)
        dir.startsWith("mipmap") -> Mipmap(name)
        dir.startsWith("raw") -> Raw(name)
        dir.startsWith("xml") -> Style(name)
        else -> null
      }
    }

    @Suppress("ComplexMethod")
    fun fromValuePair(type: String, name: String): UnqualifiedAndroidResourceDeclaredName? {
      return when (type) {
        "anim" -> Anim(name)
        "animator" -> Animator(name)
        "array" -> Arrays(name)
        "bool" -> Bool(name)
        "color" -> Color(name)
        "dimen" -> Dimen(name)
        "drawable" -> Drawable(name)
        "font" -> Font(name)
        "id" -> ID(name)
        "integer" -> AndroidInteger(name)
        "integer-array" -> Arrays(name)
        "layout" -> Layout(name)
        "menu" -> Menu(name)
        "mipmap" -> Mipmap(name)
        "raw" -> Raw(name)
        "string" -> AndroidString(name)
        "style" -> Style(name)
        else -> null
      }
    }

    fun fromString(str: String): UnqualifiedAndroidResourceDeclaredName? {
      val (prefix, name) = REGEX.find(str)?.destructured ?: return null

      return fromValuePair(prefix, name)
    }
  }
}
