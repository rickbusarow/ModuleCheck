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

import modulecheck.parsing.source.Reference.ExplicitKotlinReference

sealed interface NamedSymbol {
  val fqName: String

  fun startsWith(symbol: NamedSymbol): Boolean {
    return fqName.startsWith(symbol.fqName)
  }

  fun startingWith(str: String): List<String> {
    return if (fqName.startsWith(str)) {
      listOf(fqName)
    } else {
      listOf()
    }
  }

  fun startingWith(symbol: NamedSymbol): List<String> {
    return if (fqName.startsWith(symbol.fqName)) {
      listOf(fqName)
    } else {
      listOf()
    }
  }

  fun endsWith(str: String): Boolean {
    return fqName.endsWith(str)
  }

  fun endsWith(symbol: NamedSymbol): Boolean {
    return fqName.endsWith(symbol.fqName)
  }

  fun endsWith(str: ExplicitKotlinReference): Boolean {
    return fqName.endsWith(str.fqName)
  }

  fun endingWith(symbol: NamedSymbol): List<String> {
    return if (fqName.endsWith(symbol.fqName)) {
      listOf(fqName)
    } else {
      listOf()
    }
  }

  fun endingWith(str: ExplicitKotlinReference): List<String> {
    return if (fqName.endsWith(str.fqName)) {
      listOf(fqName)
    } else {
      listOf()
    }
  }
}
