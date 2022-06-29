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

import modulecheck.parsing.source.ReferenceName.JavaReferenceName
import modulecheck.parsing.source.ReferenceName.KotlinReferenceName
import modulecheck.utils.lazy.LazySet
import modulecheck.utils.safeAs

/**
 * Represents a call-site reference to a name, such as:
 * - the name in an import statement
 * - the type declaration of a parameter or property
 * - a function return type
 * - a super-type declaration
 * - a function call
 * - a property read/write
 */
sealed interface ReferenceName : McName {

  sealed interface JavaReferenceName : ReferenceName {
    companion object {
      operator fun invoke(name: String): JavaReferenceName = JavaReferenceNameImpl(name)
    }
  }

  sealed interface KotlinReferenceName : ReferenceName {
    companion object {
      operator fun invoke(name: String): KotlinReferenceName = KotlinReferenceNameImpl(name)
    }
  }

  sealed interface AgnosticReferenceName : ReferenceName

  /**
   * Marker for any reference made from an XML file.
   *
   * Note that aside from references to android resources, xml follows the same reference names as
   * Java.
   */
  sealed interface XmlReferenceName : ReferenceName, JavaReferenceName

  class KotlinReferenceNameImpl(override val name: String) :
    ReferenceName,
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

  class JavaReferenceNameImpl(override val name: String) :
    ReferenceName,
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

  class XmlReferenceNameImpl(override val name: String) :
    ReferenceName,
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
}

fun String.asKotlinReference(): KotlinReferenceName = KotlinReferenceName(this)
fun String.asJavaReference(): JavaReferenceName = JavaReferenceName(this)

interface HasReferences {

  val references: LazySet<ReferenceName>
}
