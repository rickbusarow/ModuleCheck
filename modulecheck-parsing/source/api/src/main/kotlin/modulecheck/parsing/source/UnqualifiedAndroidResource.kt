/*
 * Copyright (C) 2021-2023 Rick Busarow
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

import modulecheck.parsing.source.McName.CompatibleLanguage.XML
import modulecheck.parsing.source.SimpleName.Companion.asSimpleName
import modulecheck.parsing.source.SimpleName.Companion.asString
import modulecheck.utils.lazy.unsafeLazy
import java.io.File
import kotlin.io.path.name

/**
 * example: `R.string.app_name`
 *
 * @property prefix 'string' in `R.string.app_name`
 * @property identifier 'app_name' in `R.string.app_name`
 * @since 0.12.0
 */
class UnqualifiedAndroidResource private constructor(
  val prefix: SimpleName,
  val identifier: SimpleName
) : AndroidResourceDeclaredName, McName, HasSimpleNames {

  override val simpleNames: List<SimpleName> by unsafeLazy {
    listOf("R".asSimpleName(), prefix, identifier)
  }
  override val segments: List<String> by unsafeLazy { simpleNames.map { it.name } }
  override val name: String by unsafeLazy { simpleNames.asString() }

  /**
   * @return the fully qualified name of a generated Android
   *   resource, like `com.example.R.string.app_name` from the
   *   combination of `com.example.R` and `R.string.app_name`
   * @since 0.12.0
   */
  fun toQualifiedDeclaredName(
    androidRDeclaration: AndroidRDeclaredName
  ): QualifiedAndroidResourceDeclaredName {
    return AndroidResourceDeclaredName.qualifiedAndroidResource(
      sourceR = AndroidRReferenceName(androidRDeclaration.packageName, XML),
      sourceResource = UnqualifiedAndroidResourceReferenceName(this.name, XML)
    )
  }

  override fun equals(other: Any?): Boolean {
    if (this === other) return true

    when (other) {
      is ReferenceName -> {

        if (name != other.name) return false
      }

      is UnqualifiedAndroidResource -> {

        if (prefix != other.prefix) return false
        if (identifier != other.identifier) return false
      }

      else -> return false
    }
    return true
  }

  override fun hashCode(): Int = name.hashCode()

  override fun toString(): String = "(${this::class.java.simpleName}) `$name`"

  companion object {

    /**
     * `R.anim.foo`
     *
     * @since 0.12.0
     */
    fun anim(identifier: SimpleName): UnqualifiedAndroidResource =
      UnqualifiedAndroidResource("anim".asSimpleName(), identifier = identifier)

    /**
     * `R.animator.foo`
     *
     * @since 0.12.0
     */
    fun animator(identifier: SimpleName): UnqualifiedAndroidResource =
      UnqualifiedAndroidResource("animator".asSimpleName(), identifier = identifier)

    /**
     * `R.array.foo`
     *
     * @since 0.12.0
     */
    fun array(identifier: SimpleName): UnqualifiedAndroidResource =
      UnqualifiedAndroidResource("array".asSimpleName(), identifier = identifier)

    /**
     * `R.bool.foo`
     *
     * @since 0.12.0
     */
    fun bool(identifier: SimpleName): UnqualifiedAndroidResource =
      UnqualifiedAndroidResource("bool".asSimpleName(), identifier = identifier)

    /**
     * `R.color.foo`
     *
     * @since 0.12.0
     */
    fun color(identifier: SimpleName): UnqualifiedAndroidResource =
      UnqualifiedAndroidResource("color".asSimpleName(), identifier = identifier)

    /**
     * `R.dimen.foo`
     *
     * @since 0.12.0
     */
    fun dimen(identifier: SimpleName): UnqualifiedAndroidResource =
      UnqualifiedAndroidResource("dimen".asSimpleName(), identifier = identifier)

    /**
     * `R.drawable.foo`
     *
     * @since 0.12.0
     */
    fun drawable(identifier: SimpleName): UnqualifiedAndroidResource =
      UnqualifiedAndroidResource("drawable".asSimpleName(), identifier = identifier)

    /**
     * `R.font.foo`
     *
     * @since 0.12.0
     */
    fun font(identifier: SimpleName): UnqualifiedAndroidResource =
      UnqualifiedAndroidResource("font".asSimpleName(), identifier = identifier)

    /**
     * `R.id.foo`
     *
     * @since 0.12.0
     */
    fun id(identifier: SimpleName): UnqualifiedAndroidResource =
      UnqualifiedAndroidResource("id".asSimpleName(), identifier = identifier)

    /**
     * `R.integer.foo`
     *
     * @since 0.12.0
     */
    fun integer(identifier: SimpleName): UnqualifiedAndroidResource =
      UnqualifiedAndroidResource("integer".asSimpleName(), identifier = identifier)

    /**
     * `R.layout.foo`
     *
     * @since 0.12.0
     */
    fun layout(identifier: SimpleName): UnqualifiedAndroidResource =
      UnqualifiedAndroidResource("layout".asSimpleName(), identifier = identifier)

    /**
     * `R.menu.foo`
     *
     * @since 0.12.0
     */
    fun menu(identifier: SimpleName): UnqualifiedAndroidResource =
      UnqualifiedAndroidResource("menu".asSimpleName(), identifier = identifier)

    /**
     * `R.mipmap.foo`
     *
     * @since 0.12.0
     */
    fun mipmap(identifier: SimpleName): UnqualifiedAndroidResource =
      UnqualifiedAndroidResource("mipmap".asSimpleName(), identifier = identifier)

    /**
     * `R.raw.foo`
     *
     * @since 0.12.0
     */
    fun raw(identifier: SimpleName): UnqualifiedAndroidResource =
      UnqualifiedAndroidResource("raw".asSimpleName(), identifier = identifier)

    /**
     * `R.string.foo`
     *
     * @since 0.12.0
     */
    fun string(identifier: SimpleName): UnqualifiedAndroidResource =
      UnqualifiedAndroidResource("string".asSimpleName(), identifier = identifier)

    /**
     * `R.style.foo`
     *
     * @since 0.12.0
     */
    fun style(identifier: SimpleName): UnqualifiedAndroidResource =
      UnqualifiedAndroidResource("style".asSimpleName(), identifier = identifier)

    /**
     * @return all resources declared within the given [file]
     * @since 0.12.0
     */
    fun fromFile(file: File): UnqualifiedAndroidResource? {
      val dir = file.toPath().parent?.name ?: return null
      val name = file.nameWithoutExtension

      return when {
        dir.startsWith("anim") -> anim(name.asSimpleName())
        dir.startsWith("animator") -> animator(name.asSimpleName())
        dir.startsWith("color") -> color(name.asSimpleName())
        dir.startsWith("dimen") -> dimen(name.asSimpleName())
        dir.startsWith("drawable") -> drawable(name.asSimpleName())
        dir.startsWith("font") -> font(name.asSimpleName())
        dir.startsWith("layout") -> layout(name.asSimpleName())
        dir.startsWith("menu") -> menu(name.asSimpleName())
        dir.startsWith("mipmap") -> mipmap(name.asSimpleName())
        dir.startsWith("raw") -> raw(name.asSimpleName())
        else -> null
      }
    }

    /**
     * @return `R.id.foo` for [type] `id` and [name] `foo`
     * @since 0.12.0
     */
    fun fromValuePair(type: String, name: String): UnqualifiedAndroidResource? {
      val fixedName = name.replace('.', '_')
      return when (type.removePrefix("android:")) {
        "anim" -> anim(fixedName.asSimpleName())
        "animator" -> animator(fixedName.asSimpleName())
        "array" -> array(fixedName.asSimpleName())
        "bool" -> bool(fixedName.asSimpleName())
        "color" -> color(fixedName.asSimpleName())
        "dimen" -> dimen(fixedName.asSimpleName())
        "drawable" -> drawable(fixedName.asSimpleName())
        "font" -> font(fixedName.asSimpleName())
        "id" -> id(fixedName.asSimpleName())
        "integer" -> integer(fixedName.asSimpleName())
        "integer-array" -> array(fixedName.asSimpleName())
        "layout" -> layout(fixedName.asSimpleName())
        "menu" -> menu(fixedName.asSimpleName())
        "mipmap" -> mipmap(fixedName.asSimpleName())
        "raw" -> raw(fixedName.asSimpleName())
        "string" -> string(fixedName.asSimpleName())
        "style" -> style(fixedName.asSimpleName())
        else -> null
      }
    }

    private val REGEX = """"?@\+?(.*)\/(.*)"?""".toRegex()

    /**
     * @return a resource declaration from a string in XML, like `@+id/______`
     * @since 0.12.0
     */
    fun fromXmlString(str: String): UnqualifiedAndroidResource? {
      val (prefix, name) = REGEX.find(str)?.destructured ?: return null

      return fromValuePair(prefix, name)
    }
  }
}
