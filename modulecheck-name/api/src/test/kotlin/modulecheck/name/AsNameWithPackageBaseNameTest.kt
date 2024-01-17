/*
 * Copyright (C) 2021-2024 Rick Busarow
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

package modulecheck.name

import io.kotest.matchers.shouldBe
import modulecheck.name.PackageName.Companion.asPackageName
import modulecheck.name.SimpleName.Companion.asSimpleName
import org.jetbrains.kotlin.name.FqName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test

internal class AsNameWithPackageBaseNameTest {

  @Nested
  inner class `FqName` {

    @Test
    fun `FqName asDeclaredName with nested type treats outer type as simple name`() {

      val packageName = "com.test".asPackageName()
      val simpleNames = listOf("Outer".asSimpleName(), "Inner".asSimpleName())

      val asString = packageName.appendAsString(simpleNames.map { it.asString })

      FqName(asString).asNameWithPackageName(packageName) shouldBe NameWithPackageNameImpl(
        packageName = packageName,
        simpleNames = simpleNames
      )
    }

    @Test
    fun `asNameWithPackageName with no language creates agnostic declared name`() {

      val packageName = "com.test".asPackageName()
      val simpleNames = listOf("Subject".asSimpleName())

      val asString = packageName.appendAsString(simpleNames.map { it.asString })

      FqName(asString).asNameWithPackageName(packageName) shouldBe NameWithPackageNameImpl(
        packageName = packageName,
        simpleNames = simpleNames
      )
    }

    @Test
    fun `asNameWithPackageName with Kotlin language creates Kotlin declared name`() {

      val packageName = "com.test".asPackageName()
      val simpleNames = listOf("Subject".asSimpleName())

      val asString = packageName.appendAsString(simpleNames.map { it.asString })

      FqName(asString).asNameWithPackageName(packageName) shouldBe NameWithPackageNameImpl(
        packageName = packageName,
        simpleNames = simpleNames
      )
    }

    @Test
    fun `asNameWithPackageName with Java language creates Java declared name`() {

      val packageName = "com.test".asPackageName()
      val simpleNames = listOf("Subject".asSimpleName())

      val asString = packageName.appendAsString(simpleNames.map { it.asString })

      FqName(asString).asNameWithPackageName(packageName) shouldBe NameWithPackageNameImpl(
        packageName = packageName,
        simpleNames = simpleNames
      )
    }

    @Test
    fun `asNameWithPackageName with Java and Kotlin languages creates agnostic declared name`() {

      val packageName = "com.test".asPackageName()
      val simpleNames = listOf("Subject".asSimpleName())

      val asString = packageName.appendAsString(simpleNames.map { it.asString })

      FqName(asString).asNameWithPackageName(packageName) shouldBe NameWithPackageNameImpl(
        packageName = packageName,
        simpleNames = simpleNames
      )
    }
  }

  @Nested
  inner class `iterable receiver` {

    @Test
    fun `asNameWithPackageName with no language creates agnostic declared name`() {

      val packageName = "com.test".asPackageName()
      val simpleNames = listOf("Subject".asSimpleName())

      simpleNames.asNameWithPackageName(packageName) shouldBe NameWithPackageNameImpl(
        packageName = packageName,
        simpleNames = simpleNames
      )
    }

    @Test
    fun `asNameWithPackageName with Kotlin language creates Kotlin declared name`() {

      val packageName = "com.test".asPackageName()
      val simpleNames = listOf("Subject".asSimpleName())

      simpleNames.asNameWithPackageName(packageName) shouldBe NameWithPackageNameImpl(
        packageName = packageName,
        simpleNames = simpleNames
      )
    }

    @Test
    fun `asNameWithPackageName with Java language creates Java declared name`() {

      val packageName = "com.test".asPackageName()
      val simpleNames = listOf("Subject".asSimpleName())

      simpleNames.asNameWithPackageName(packageName) shouldBe NameWithPackageNameImpl(
        packageName = packageName,
        simpleNames = simpleNames
      )
    }

    @Test
    fun `asNameWithPackageName with Java and Kotlin languages creates agnostic declared name`() {

      val packageName = "com.test".asPackageName()
      val simpleNames = listOf("Subject".asSimpleName())

      simpleNames.asNameWithPackageName(packageName) shouldBe NameWithPackageNameImpl(
        packageName = packageName,
        simpleNames = simpleNames
      )
    }
  }
}
