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

import io.kotest.matchers.shouldBe
import modulecheck.parsing.source.McName.CompatibleLanguage.JAVA
import modulecheck.parsing.source.McName.CompatibleLanguage.KOTLIN
import modulecheck.parsing.source.PackageName.Companion.asPackageName
import modulecheck.parsing.source.SimpleName.Companion.asSimpleName
import org.jetbrains.kotlin.name.FqName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test

internal class AsDeclaredNameTest {

  @Nested
  inner class `FqName` {

    @Test
    fun `FqName asDeclaredName with nested type treats outer type as simple name`() {

      val packageName = "com.test".asPackageName()
      val simpleNames = listOf("Outer".asSimpleName(), "Inner".asSimpleName())

      val asString = packageName.append(simpleNames.map { it.name })

      FqName(asString).asDeclaredName(packageName) shouldBe DeclaredName.agnostic(
        packageName = packageName,
        simpleNames = simpleNames
      )
    }

    @Test
    fun `asDeclaredName with no language creates agnostic declared name`() {

      val packageName = "com.test".asPackageName()
      val simpleNames = listOf("Subject".asSimpleName())

      val asString = packageName.append(simpleNames.map { it.name })

      FqName(asString).asDeclaredName(packageName) shouldBe DeclaredName.agnostic(
        packageName = packageName,
        simpleNames = simpleNames
      )
    }

    @Test
    fun `asDeclaredName with Kotlin language creates Kotlin declared name`() {

      val packageName = "com.test".asPackageName()
      val simpleNames = listOf("Subject".asSimpleName())

      val asString = packageName.append(simpleNames.map { it.name })

      FqName(asString).asDeclaredName(packageName, KOTLIN) shouldBe DeclaredName.kotlin(
        packageName = packageName,
        simpleNames = simpleNames
      )
    }

    @Test
    fun `asDeclaredName with Java language creates Java declared name`() {

      val packageName = "com.test".asPackageName()
      val simpleNames = listOf("Subject".asSimpleName())

      val asString = packageName.append(simpleNames.map { it.name })

      FqName(asString).asDeclaredName(packageName, JAVA) shouldBe DeclaredName.java(
        packageName = packageName,
        simpleNames = simpleNames
      )
    }

    @Test
    fun `asDeclaredName with Java and Kotlin languages creates agnostic declared name`() {

      val packageName = "com.test".asPackageName()
      val simpleNames = listOf("Subject".asSimpleName())

      val asString = packageName.append(simpleNames.map { it.name })

      FqName(asString).asDeclaredName(packageName, JAVA, KOTLIN) shouldBe DeclaredName.agnostic(
        packageName = packageName,
        simpleNames = simpleNames
      )
    }
  }

  @Nested
  inner class `iterable receiver` {

    @Test
    fun `asDeclaredName with no language creates agnostic declared name`() {

      val packageName = "com.test".asPackageName()
      val simpleNames = listOf("Subject".asSimpleName())

      simpleNames.asDeclaredName(packageName) shouldBe DeclaredName.agnostic(
        packageName = packageName,
        simpleNames = simpleNames
      )
    }

    @Test
    fun `asDeclaredName with Kotlin language creates Kotlin declared name`() {

      val packageName = "com.test".asPackageName()
      val simpleNames = listOf("Subject".asSimpleName())

      simpleNames.asDeclaredName(packageName, KOTLIN) shouldBe DeclaredName.kotlin(
        packageName = packageName,
        simpleNames = simpleNames
      )
    }

    @Test
    fun `asDeclaredName with Java language creates Java declared name`() {

      val packageName = "com.test".asPackageName()
      val simpleNames = listOf("Subject".asSimpleName())

      simpleNames.asDeclaredName(packageName, JAVA) shouldBe DeclaredName.java(
        packageName = packageName,
        simpleNames = simpleNames
      )
    }

    @Test
    fun `asDeclaredName with Java and Kotlin languages creates agnostic declared name`() {

      val packageName = "com.test".asPackageName()
      val simpleNames = listOf("Subject".asSimpleName())

      simpleNames.asDeclaredName(packageName, JAVA, KOTLIN) shouldBe DeclaredName.agnostic(
        packageName = packageName,
        simpleNames = simpleNames
      )
    }
  }
}
