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

package modulecheck.parsing.element.kotlin

import modulecheck.parsing.element.resolve.ParsingContext
import org.jetbrains.kotlin.com.intellij.psi.PsiElement
import org.jetbrains.kotlin.resolve.BindingContext
import org.jetbrains.kotlin.util.slicedMap.ReadOnlySlice

interface HasParsingContext {
  val parsingContext: ParsingContext<PsiElement>

  suspend fun bindingContext(): BindingContext {
    return parsingContext.bindingContextDeferred.await()
  }

  suspend fun <K, V> bindingContext(readOnlySlice: ReadOnlySlice<K, V>?, key: K): V? {
    return parsingContext.bindingContextDeferred.await().get(readOnlySlice, key)
  }
}
