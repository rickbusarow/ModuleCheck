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

import kotlinx.serialization.Serializable
import modulecheck.parsing.source.HasSimpleNames.Companion.checkSimpleNames
import modulecheck.parsing.source.McName.CompatibleLanguage.XML
import modulecheck.parsing.source.SimpleName.Companion.asSimpleName
import modulecheck.utils.capitalize
import modulecheck.utils.lazy.unsafeLazy

/**
 * - fully qualified generated resources like `com.example.R.string.app_name`
 * - generated data-/view-binding declarations like `com.example.databinding.FragmentListBinding`
 * - unqualified resources which can be consumed in downstream projects, like `R.string.app_name`
 * - R declarations, like `com.example.R`
 *
 * @since 0.12.0
 */
sealed interface AndroidResourceDeclaredName : DeclaredName, HasSimpleNames {

  companion object {
    /**
     * @return example: `com.example.app.R`
     * @since 0.12.0
     */
    fun r(packageName: PackageName): AndroidRDeclaredName = AndroidRDeclaredName(packageName)

    /**
     * @return `com.example.R.string.app_name`
     * @since 0.12.0
     */
    fun qualifiedAndroidResource(
      sourceR: AndroidRReferenceName,
      sourceResource: UnqualifiedAndroidResourceReferenceName
    ): QualifiedAndroidResourceDeclaredName {
      return QualifiedAndroidResourceDeclaredName(sourceR, sourceResource)
    }

    /**
     * @return `com.example.databinding.FragmentListBinding`
     * @since 0.12.0
     */
    fun dataBinding(
      sourceLayout: UnqualifiedAndroidResourceReferenceName,
      packageName: PackageName
    ): AndroidDataBindingDeclaredName {
      return AndroidDataBindingDeclaredName(sourceLayout, packageName)
    }

    /**
     * @return `com.example.databinding.FragmentListBinding`
     * @since 0.12.0
     */
    fun dataBinding(
      sourceLayoutDeclaration: UnqualifiedAndroidResource,
      packageName: PackageName
    ): AndroidDataBindingDeclaredName {
      return AndroidDataBindingDeclaredName(
        UnqualifiedAndroidResourceReferenceName(
          name = sourceLayoutDeclaration.name,
          language = XML
        ),
        packageName
      )
    }
  }
}

/**
 * example: `com.example.app.R`
 *
 * @since 0.12.0
 */
@Serializable
class AndroidRDeclaredName(
  override val packageName: PackageName
) : QualifiedDeclaredName(), AndroidResourceDeclaredName {

  override val simpleNames: List<SimpleName> by lazy { listOf("R".asSimpleName()) }
}

/**
 * example: `com.example.R.string.app_name`
 *
 * @property sourceR the R declaration used when AGP generates this fully qualified resource
 * @property sourceResource the resource declaration, like `_.string.app_name`, used when AGP
 *   generates this fully qualified resource
 * @since 0.12.0
 */
class QualifiedAndroidResourceDeclaredName(
  val sourceR: AndroidRReferenceName,
  val sourceResource: UnqualifiedAndroidResourceReferenceName
) : QualifiedDeclaredName(), AndroidResourceDeclaredName, Generated {

  override val packageName: PackageName by unsafeLazy { sourceR.packageName }

  override val simpleNames: List<SimpleName> by unsafeLazy { sourceResource.simpleNames }

  override val name: String by unsafeLazy {
    "${sourceR.name}.${sourceResource.prefix.name}.${sourceResource.identifier.name}"
  }

  override val sources: Set<ReferenceName> = setOf(sourceR, sourceResource)

  init {
    checkSimpleNames()
  }
}

/**
 * example: `com.example.databinding.FragmentListBinding`
 *
 * @since 0.12.0
 */
class AndroidDataBindingDeclaredName(
  sourceLayout: UnqualifiedAndroidResourceReferenceName,
  override val packageName: PackageName
) : QualifiedDeclaredName(), AndroidResourceDeclaredName, Generated {

  override val simpleNames: List<SimpleName> by unsafeLazy {

    val simpleBindingName = sourceLayout.identifier.name
      .split("_")
      .joinToString("") { fragment -> fragment.capitalize() }
      .plus("Binding")
      .asSimpleName()

    listOf(
      "databinding".asSimpleName(),
      simpleBindingName
    )
  }
  override val sources: Set<ReferenceName> by unsafeLazy { setOf(sourceLayout) }

  init {
    checkSimpleNames()
  }
}
