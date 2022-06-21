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

import modulecheck.parsing.source.ReferenceName.ExplicitJavaReferenceName
import modulecheck.parsing.source.ReferenceName.ExplicitKotlinReferenceName
import modulecheck.parsing.source.ReferenceName.InterpretedJavaReferenceName
import modulecheck.parsing.source.ReferenceName.InterpretedKotlinReferenceName
import modulecheck.utils.lazy.LazySet
import modulecheck.utils.safeAs

sealed interface ReferenceName : NamedSymbol {

  sealed interface JavaReferenceName : ReferenceName
  sealed interface KotlinReferenceName : ReferenceName
  sealed interface AgnosticReferenceName : ReferenceName

  /**
   * Marker for any reference made from an XML file.
   *
   * Note that aside from references to android resources, xml follows the same reference names as
   * Java.
   */
  sealed interface XmlReferenceName : ReferenceName, JavaReferenceName

  sealed interface ExplicitReferenceName : ReferenceName

  sealed interface InterpretedReferenceName : ReferenceName

  class ExplicitKotlinReferenceName(override val name: String) :
    ReferenceName,
    ExplicitReferenceName,
    KotlinReferenceName {

    override fun equals(other: Any?): Boolean {
      return matches(
        other = other,
        ifReference = {
          name == (
            it.safeAs<KotlinReferenceName>()?.name
              ?: it.safeAs<AgnosticReferenceName>()?.name
            )
        },
        ifDeclaration = { name == it.safeAs<KotlinCompatibleDeclaredName>()?.name }
      )
    }

    override fun hashCode(): Int = name.hashCode()

    override fun toString(): String = "(${this::class.java.simpleName}) `$name`"
  }

  class InterpretedKotlinReferenceName(override val name: String) :
    ReferenceName,
    InterpretedReferenceName,
    KotlinReferenceName {

    override fun equals(other: Any?): Boolean {
      return matches(
        other = other,
        ifReference = {
          name == (
            it.safeAs<KotlinReferenceName>()?.name
              ?: it.safeAs<AgnosticReferenceName>()?.name
            )
        },
        ifDeclaration = { name == it.safeAs<KotlinCompatibleDeclaredName>()?.name }
      )
    }

    override fun hashCode(): Int = name.hashCode()

    override fun toString(): String = "(${this::class.java.simpleName}) `$name`"
  }

  class ExplicitJavaReferenceName(override val name: String) :
    ReferenceName,
    ExplicitReferenceName,
    JavaReferenceName {

    override fun equals(other: Any?): Boolean {
      return matches(
        other = other,
        ifReference = {
          name == (it.safeAs<JavaReferenceName>()?.name ?: it.safeAs<AgnosticReferenceName>()?.name)
        },
        ifDeclaration = { name == it.safeAs<JavaCompatibleDeclaredName>()?.name }
      )
    }

    override fun hashCode(): Int = name.hashCode()

    override fun toString(): String = "(${this::class.java.simpleName}) `$name`"
  }

  class ExplicitXmlReferenceName(override val name: String) :
    ReferenceName,
    ExplicitReferenceName,
    XmlReferenceName {

    override fun equals(other: Any?): Boolean {
      return matches(
        other = other,
        ifReference = {
          name == (it.safeAs<XmlReferenceName>()?.name ?: it.safeAs<AgnosticReferenceName>()?.name)
        },
        ifDeclaration = { name == it.safeAs<XmlCompatibleDeclaredName>()?.name }
      )
    }

    override fun hashCode(): Int = name.hashCode()

    override fun toString(): String = "(${this::class.java.simpleName}) `$name`"
  }

  class InterpretedJavaReferenceName(override val name: String) :
    ReferenceName,
    InterpretedReferenceName,
    JavaReferenceName {

    override fun equals(other: Any?): Boolean {
      return matches(
        other = other,
        ifReference = {
          name == (it.safeAs<JavaReferenceName>()?.name ?: it.safeAs<AgnosticReferenceName>()?.name)
        },
        ifDeclaration = { name == it.safeAs<JavaCompatibleDeclaredName>()?.name }
      )
    }

    override fun hashCode(): Int = name.hashCode()

    override fun toString(): String = "(${this::class.java.simpleName}) `$name`"
  }
}

fun String.asExplicitKotlinReference(): ExplicitKotlinReferenceName =
  ExplicitKotlinReferenceName(this)

fun String.asInterpretedKotlinReference(): InterpretedKotlinReferenceName =
  InterpretedKotlinReferenceName(this)

fun String.asExplicitJavaReference(): ExplicitJavaReferenceName = ExplicitJavaReferenceName(this)
fun String.asInterpretedJavaReference(): InterpretedJavaReferenceName =
  InterpretedJavaReferenceName(this)

interface HasReferences {

  val references: LazySet<ReferenceName>
}
