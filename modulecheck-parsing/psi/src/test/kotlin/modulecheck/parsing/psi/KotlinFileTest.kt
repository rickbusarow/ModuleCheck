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

package modulecheck.parsing.psi

import io.kotest.matchers.collections.shouldContainExactlyInAnyOrder
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.runBlocking
import modulecheck.parsing.gradle.SourceSetName
import modulecheck.parsing.psi.internal.PsiElementResolver
import modulecheck.parsing.psi.internal.psiFileFactory
import modulecheck.parsing.source.AnvilBindingReference
import modulecheck.parsing.source.AnvilBoundType
import modulecheck.parsing.source.AnvilScopeName
import modulecheck.project.McProject
import modulecheck.project.test.ProjectTest
import org.intellij.lang.annotations.Language
import org.jetbrains.kotlin.idea.KotlinLanguage
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.psi.KtFile
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test

internal class KotlinFileTest : ProjectTest() {

  @Nested
  inner class `constructor injection` {

    @Test
    fun `fully qualified annotated primary constructor arguments should be injected`() = test {

      val file = createFile(
        """
      package com.test

      import com.lib1.Lib1Class

      class InjectClass @javax.inject.Inject constructor(
        val lib1Class: Lib1Class
      )
    """
      )

      file.constructorInjectedTypes.await() shouldBe listOf("com.lib1.Lib1Class")
      file.simpleBoundTypes.await() shouldBe listOf("com.test.InjectClass")
    }

    @Test
    fun `fully qualified annotated secondary constructor arguments should be injected`() = test {

      val file = createFile(
        """
      package com.test

      import com.lib1.Lib1Class

      class InjectClass {
        val lib1Class: Lib1Class

        @javax.inject.Inject
        constructor(lib1Class: Lib1Class) {
          this.lib1Class = lib1Class
        }
      }
    """
      )

      file.constructorInjectedTypes.await() shouldBe listOf("com.lib1.Lib1Class")
      file.simpleBoundTypes.await() shouldBe listOf("com.test.InjectClass")
    }

    @Test
    fun `imported annotated primary constructor arguments should be injected`() = test {

      val file = createFile(
        """
      package com.test

      import com.lib1.Lib1Class
      import javax.inject.Inject

      class InjectClass @Inject constructor(
        val lib1Class: Lib1Class
      )
    """
      )

      file.constructorInjectedTypes.await() shouldBe listOf("com.lib1.Lib1Class")
      file.simpleBoundTypes.await() shouldBe listOf("com.test.InjectClass")
    }

    @Test
    fun `wildcard-imported types and imported annotated primary constructor arguments should be injected`() =
      test {

        val file = createFile(
          """
      package com.test

      import com.lib1.*
      import javax.inject.Inject

      class InjectClass @Inject constructor(
        val lib1Class: Lib1Class
      )
    """
        )

        file.constructorInjectedTypes.await() shouldBe listOf("com.lib1.Lib1Class")
        file.simpleBoundTypes.await() shouldBe listOf("com.test.InjectClass")
      }

    @Test
    fun `wildcard-imported types and wildcard-imported annotated primary constructor arguments should be injected`() =
      test {

        val file = createFile(
          """
      package com.test

      import com.lib1.*
      import javax.inject.*

      class InjectClass @Inject constructor(
        val lib1Class: Lib1Class
      )
    """
        )

        file.constructorInjectedTypes.await() shouldBe listOf("com.lib1.Lib1Class")
        file.simpleBoundTypes.await() shouldBe listOf("com.test.InjectClass")
      }

    @Test
    fun `fully qualified arguments in annotated primary constructor should be injected`() = test {

      val file = createFile(
        """
      package com.test

      import javax.inject.Inject

      class InjectClass @Inject constructor(
        val lib1Class: com.lib1.Lib1Class
      )
      """
      )

      file.constructorInjectedTypes.await() shouldBe listOf("com.lib1.Lib1Class")
      file.simpleBoundTypes.await() shouldBe listOf("com.test.InjectClass")
    }

    @Test
    fun `types wrapped in a Provider in annotated primary constructor should be injected`() = test {

      val file = createFile(
        """
      package com.test

      import javax.inject.Inject
      import javax.inject.Provider

      class InjectClass @Inject constructor(
        val lib1Class: Provider<com.lib1.Lib1Class>
      )
      """
      )

      file.constructorInjectedTypes.await() shouldBe listOf(
        "com.lib1.Lib1Class",
        "javax.inject.Provider"
      )
      file.constructorInjectedTypes.await() shouldBe listOf(
        "com.lib1.Lib1Class",
        "javax.inject.Provider"
      )
      file.simpleBoundTypes.await() shouldBe listOf("com.test.InjectClass")
    }

    @Test
    fun `imported annotated secondary constructor arguments should be injected`() = test {

      val file = createFile(
        """
      package com.test

      import com.lib1.Lib1Class
      import javax.inject.Inject

      class InjectClass {
        val lib1Class: Lib1Class

        @Inject
        constructor(lib1Class: Lib1Class) {
          this.lib1Class = lib1Class
        }
      }
    """
      )

      file.constructorInjectedTypes.await() shouldBe listOf("com.lib1.Lib1Class")
      file.simpleBoundTypes.await() shouldBe listOf("com.test.InjectClass")
    }

    @Test
    fun `arguments from overloaded constructor without annotation should not be injected`() = test {

      val file = createFile(
        """
      package com.test

      import com.lib1.Lib1Class
      import org.jetbrains.kotlin.name.FqName
      import javax.inject.Inject

      class InjectClass @Inject constructor(
        val lib1Class: Lib1Class
      ) {

        constructor(lib1Class: Lib1Class, other: FqName) : this(lib1Class)
      }
    """
      )

      file.constructorInjectedTypes.await() shouldBe listOf("com.lib1.Lib1Class")
      file.simpleBoundTypes.await() shouldBe listOf("com.test.InjectClass")
    }
  }

  @Nested
  inner class `member injection` {

    @Test
    fun `fully qualified annotated lateinit properties with explicit imported types should be injected`() =
      test {

        val file = createFile(
          """
      package com.test

      import com.lib1.Lib1Class

      class InjectClass {
        @javax.inject.Inject lateinit var lib1Class: Lib1Class
      }
    """
        )

        file.memberInjectedTypes.await() shouldBe listOf("com.lib1.Lib1Class")
        file.simpleBoundTypes.await() shouldBe emptyList()
      }

    @Test
    fun `fully qualified annotated nullable properties with explicit imported types should be injected`() =
      test {

        val file = createFile(
          """
      package com.test

      import com.lib1.Lib1Class

      class InjectClass {
        @javax.inject.Inject var lib1Class: Lib1Class? = null
      }
    """
        )

        file.memberInjectedTypes.await() shouldBe listOf("com.lib1.Lib1Class")
        file.simpleBoundTypes.await() shouldBe emptyList()
      }

    @Test
    fun `fully qualified annotated lateinit properties with explicit fully qualified types should be injected`() =
      test {

        val file = createFile(
          """
      package com.test

      class InjectClass {
        @javax.inject.Inject lateinit var lib1Class: com.lib1.Lib1Class
      }
    """
        )

        file.memberInjectedTypes.await() shouldBe listOf("com.lib1.Lib1Class")
        file.simpleBoundTypes.await() shouldBe emptyList()
      }

    @Test
    fun `fully qualified annotated nullable properties with explicit fully qualified types should be injected`() =
      test {

        val file = createFile(
          """
      package com.test

      class InjectClass {
        @javax.inject.Inject var lib1Class: com.lib1.Lib1Class? = null
      }
    """
        )

        file.memberInjectedTypes.await() shouldBe listOf("com.lib1.Lib1Class")
        file.simpleBoundTypes.await() shouldBe emptyList()
      }

    @Test
    fun `annotated lateinit properties with explicit imported types should be injected`() = test {

      val file = createFile(
        """
      package com.test

      import com.lib1.Lib1Class
      import javax.inject.Inject

      class InjectClass {
        @Inject lateinit var lib1Class: Lib1Class
      }
    """
      )

      file.memberInjectedTypes.await() shouldBe listOf("com.lib1.Lib1Class")
      file.simpleBoundTypes.await() shouldBe emptyList()
    }

    @Test
    fun `annotated nullable properties with explicit imported types should be injected`() = test {

      val file = createFile(
        """
      package com.test

      import com.lib1.Lib1Class
      import javax.inject.Inject

      class InjectClass {
        @Inject var lib1Class: Lib1Class? = null
      }
    """
      )

      file.memberInjectedTypes.await() shouldBe listOf("com.lib1.Lib1Class")
      file.simpleBoundTypes.await() shouldBe emptyList()
    }

    @Test
    fun `annotated lateinit properties with explicit fully qualified types should be injected`() =
      test {

        val file = createFile(
          """
      package com.test

      import javax.inject.Inject

      class InjectClass {
        @Inject lateinit var lib1Class: com.lib1.Lib1Class
      }
    """
        )

        file.memberInjectedTypes.await() shouldBe listOf("com.lib1.Lib1Class")
        file.simpleBoundTypes.await() shouldBe emptyList()
      }

    @Test
    fun `annotated nullable properties with explicit fully qualified types should be injected`() =
      test {

        val file = createFile(
          """
      package com.test

      import javax.inject.Inject

      class InjectClass {
        @Inject var lib1Class: com.lib1.Lib1Class? = null
      }
    """
        )

        file.memberInjectedTypes.await() shouldBe listOf("com.lib1.Lib1Class")
        file.simpleBoundTypes.await() shouldBe emptyList()
      }
  }

  @Nested
  inner class `component bindings` {

    @Test
    fun `fully qualified ContributesTo component interface with fully qualified types should be a binding reference`() =
      test {

        val file = createFile(
          """
      package com.test

      @com.squareup.anvil.annotations.ContributesTo(Unit::class)
      interface SomeComponent {
        val lib1Class: com.lib1.Lib1Class
      }
    """
        )

        file.componentBindingReferences.await() shouldBe listOf(
          AnvilBindingReference(
            FqName("com.lib1.Lib1Class"),
            AnvilScopeName(FqName("Unit"))
          )
        )
      }
  }

  @Nested
  inner class `module bindings` {

    @Test
    fun `fully qualified Module and ContributesTo interface with getter Binds annotation should be bound`() =
      test {

        val file = createFile(
          """
      package com.test

      import dagger.Binds

      @dagger.Module
      @com.squareup.anvil.annotations.ContributesTo(Unit::class)
      interface SomeModule {
        @get:Binds
        val Lib2Class.lib1Class: com.lib1.Lib1Class
      }

      class Lib2Class: Lib1Class()
    """
        )

        file.moduleBindingReferences.await() shouldBe listOf("com.test.Lib2Class")

        file.boundTypes.await() shouldBe listOf("com.lib1.Lib2Class")
      }
  }

  @Test
  fun `api references should not include concatenated matches if the reference is already imported`() =
    test {

      val file = createFile(
        """
      package com.test

      import androidx.lifecycle.ViewModel

      class MyViewModel : ViewModel() {

        fun someFunction() {
          viewEffect(resourceProvider.getString(R.string.playstore_url))
        }
      }
    """
      )

      file.apiReferences.await() shouldBe listOf("androidx.lifecycle.ViewModel")
    }

  @Test
  fun `explicit type of public property in public class should be api reference`() =
    test {

      val file = createFile(
        """
      package com.test

      import com.lib.Config

      class MyClass {

        val config : Config = ConfigImpl(
          googleApiKey = getString(R.string.google_places_api_key),
        )
      }
    """
      )

      file.apiReferences.await() shouldBe listOf("com.lib.Config")
    }

  @Test
  fun `explicit fully qualified type of public property in public class should be api reference`() =
    test {

      val file = createFile(
        """
      package com.test

      class MyClass {

        val config : com.lib.Config = ConfigImpl(
          googleApiKey = getString(R.string.google_places_api_key),
        )
      }
    """
      )

      file.apiReferences.await() shouldBe listOf("com.lib.Config", "com.test.com.lib.Config")
    }

  @Test
  fun `explicit type of public property in internal class should not be api reference`() =
    test {

      val file = createFile(
        """
      package com.test

      import com.lib.Config

      internal class MyClass {

        val config : Config = ConfigImpl(
          googleApiKey = getString(R.string.google_places_api_key),
        )
      }
    """
      )

      file.apiReferences.await() shouldBe listOf()
    }

  @Test
  fun `implicit type of public property in public class should be api reference`() =
    test {

      val file = createFile(
        """
      package com.test

      import com.lib.Config

      class MyClass {

        val config = Config(
          googleApiKey = getString(R.string.google_places_api_key),
        )
      }
    """
      )

      file.apiReferences.await() shouldBe listOf("com.lib.Config")
    }

  fun simpleProject() = project(":lib") {
    addSource(
      "com/lib1/Lib1Class.kt",
      """
        package com.lib1

        class Lib1Class
      """,
      SourceSetName.MAIN
    )
  }

  @Suppress("NOTHING_TO_INLINE")
  inline infix fun Set<FqName>.shouldBe(expected: List<String>) {
    map { it.asString() } shouldContainExactlyInAnyOrder expected
  }

  @Suppress("NOTHING_TO_INLINE")
  inline infix fun Collection<AnvilBoundType>.shouldBe(expected: List<String>) {

    onEach { boundType ->
      require(boundType.boundType == boundType.realType) {
        """An ${boundType::class.java.simpleName} in this collection has a different bound type than its real type.
          |
          |  bound type: ${boundType.boundType}
          |  real type:  ${boundType.realType}
          |
          |This `shouldBe` is meant to be a convenience function for `@Inject`-annotated constructors only.
          |For complex bindings, compare the collection to a `List<AnvilBoundType>`.
        """.trimMargin()
      }
      require(boundType.scopeOrNull == null) {
        """An ${boundType::class.java.simpleName} in this collection has a defined `${AnvilScopeName::class.java.simpleName}`
          |which cannot be captured in this assertion.
          |
          |This `shouldBe` is meant to be a convenience function for `@Inject`-annotated constructors only.
          |For complex bindings, compare the collection to a `List<AnvilBoundType>`.
        """.trimMargin()
      }
    }
      .map { it.boundType.asString() } shouldContainExactlyInAnyOrder expected
  }

  fun createFile(
    @Language("kotlin")
    content: String,
    project: McProject = simpleProject(),
    sourceSetName: SourceSetName = SourceSetName.MAIN
  ): RealKotlinFile {

    val kt = KtFile(content)

    return RealKotlinFile(kt, PsiElementResolver(project, sourceSetName))
  }

  fun test(action: suspend CoroutineScope.() -> Unit) = runBlocking(block = action)

  fun KtFile(
    @Language("kotlin")
    content: String
  ): KtFile = KtFile(name = "Source.kt", content = content)

  fun KtFile(
    name: String,
    @Language("kotlin")
    content: String
  ): KtFile = psiFileFactory.createFileFromText(
    name,
    KotlinLanguage.INSTANCE,
    content.trimIndent()
  ) as KtFile
}
