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

package modulecheck.api

sealed class BuildType(val name: String) {

  object AndroidTest : BuildType("androidTest")
  object Debug : BuildType("debug")
  object Release : BuildType("release")
  object Main : BuildType("main")
  object Test : BuildType("test")

  class Custom(name: String) : BuildType(name)

  companion object {
    fun from(name: String): BuildType = when (name) {
      AndroidTest.name -> AndroidTest
      Debug.name -> Debug
      Main.name -> Main
      Release.name -> Release
      Test.name -> Test
      else -> Custom(name)
    }
  }

  override fun equals(other: Any?): Boolean {
    if (this === other) return true
    if (other !is BuildType) return false

    if (name != other.name) return false

    return true
  }

  override fun hashCode(): Int {
    return name.hashCode()
  }

  override fun toString(): String {
    return "BuildType(name='$name')"
  }
}
