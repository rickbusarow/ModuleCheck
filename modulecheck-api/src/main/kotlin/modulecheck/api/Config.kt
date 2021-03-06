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

package modulecheck.api

typealias ConfigurationName = String

data class Config(
  val name: String,
  val externalDependencies: Set<ExternalDependency>
)

fun <T : Any> Map<ConfigurationName, Collection<T>>.main(): List<T> {
  return listOfNotNull(
    get("api"),
    get("compileOnly"),
    get("implementation"),
    get("runtimeOnly")
  ).flatten()
}

fun <K : Any, T : Any> Map<K, Collection<T>>.all(): List<T> {
  return values.flatten()
}
