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

@file:Suppress("MatchingDeclarationName")

package modulecheck.api

data class KaptMatcher(
  val name: String,
  val processor: String,
  val annotationImports: List<String>
) {

  override fun equals(other: Any?): Boolean {
    if (this === other) return true
    if (other !is KaptMatcher) return false

    if (processor != other.processor) return false

    return true
  }

  override fun hashCode(): Int = processor.hashCode()

  override fun toString(): String {
    return """KaptMatcher(
      |  name='$name',
      |  processor='$processor',
      |  annotationImports=${annotationImports.joinToString("\n    ", "\n    ")}
      |)""".trimMargin()
  }
}

fun List<KaptMatcher>.asMap(): Map<String, KaptMatcher> = associateBy { it.processor }
