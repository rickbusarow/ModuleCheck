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

package modulecheck.parsing.gradle.dsl

import modulecheck.parsing.gradle.model.ProjectPath
import modulecheck.parsing.gradle.model.ProjectPath.StringProjectPath
import modulecheck.parsing.gradle.model.ProjectPath.TypeSafeProjectPath

sealed interface ProjectAccessor {
  val statementText: String
  val projectPath: ProjectPath

  data class StringProjectAccessor(
    override val statementText: String,
    override val projectPath: StringProjectPath
  ) : ProjectAccessor {
    init {
      require(statementText.matches("""^\s?project\s?\([\s\S]*""".toRegex())) {
        "expected a normal project function but instead got `$statementText`."
      }
    }
  }

  data class TypeSafeProjectAccessor(
    override val statementText: String,
    override val projectPath: TypeSafeProjectPath
  ) : ProjectAccessor {
    init {
      require(statementText.matches("""^\s?projects\s?\.[\s\S]*""".toRegex())) {
        "expected a type-safe project accessor but instead got `$statementText`."
      }
    }
  }

  companion object {
    fun from(rawString: String, projectPath: ProjectPath): ProjectAccessor {

      return when (projectPath) {
        is StringProjectPath -> StringProjectAccessor(rawString, projectPath)
        is TypeSafeProjectPath -> TypeSafeProjectAccessor(rawString, projectPath)
      }
    }
  }
}
