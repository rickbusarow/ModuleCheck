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
import modulecheck.project.McProject
import modulecheck.project.test.ProjectTest
import org.intellij.lang.annotations.Language
import org.jetbrains.kotlin.idea.KotlinLanguage
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.psi.KtFile
import org.junit.jupiter.api.Test

internal class KotlinFileTest : ProjectTest() {

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

    file.constructorInjectedParams.await() shouldBe listOf(
      FqName("com.lib1.Lib1Class")
    )
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

    file.constructorInjectedParams.await() shouldBe listOf(
      FqName("com.lib1.Lib1Class")
    )
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

    file.constructorInjectedParams.await() shouldBe listOf(
      FqName("com.lib1.Lib1Class")
    )
  }

  @Test
  fun `wildcard-imported annotated primary constructor arguments should be injected`() = test {

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

    file.constructorInjectedParams.await() shouldContainExactlyInAnyOrder listOf(
      FqName("com.lib1.Lib1Class")
    )
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

    file.constructorInjectedParams.await() shouldContainExactlyInAnyOrder listOf(
      FqName("com.lib1.Lib1Class")
    )
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

    file.constructorInjectedParams.await() shouldBe listOf(
      FqName("com.lib1.Lib1Class")
    )
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

    file.constructorInjectedParams.await() shouldBe listOf(
      FqName("com.lib1.Lib1Class")
    )
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
