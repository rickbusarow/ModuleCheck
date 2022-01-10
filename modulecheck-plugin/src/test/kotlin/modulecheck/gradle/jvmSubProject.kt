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

package modulecheck.gradle

import com.squareup.kotlinpoet.ClassName
import com.squareup.kotlinpoet.FileSpec
import com.squareup.kotlinpoet.TypeSpec
import modulecheck.specs.ProjectBuildSpec
import modulecheck.specs.ProjectSpec
import modulecheck.specs.ProjectSrcSpec
import modulecheck.specs.applyEach
import java.nio.file.Path

@Suppress("LongParameterList")
fun jvmSubProject(
  path: String,
  vararg fqClassName: ClassName,
  apiDependencies: List<ProjectSpec> = emptyList(),
  implementationDependencies: List<ProjectSpec> = emptyList(),
  androidTestDependencies: List<ProjectSpec> = emptyList(),
  testDependencies: List<ProjectSpec> = emptyList()
): ProjectSpec = ProjectSpec(path) {
  addBuildSpec(
    ProjectBuildSpec {
      addPlugin("kotlin(\"jvm\")")
      applyEach(apiDependencies) { addProjectDependency("api", it) }
      applyEach(implementationDependencies) { addProjectDependency("implementation", it) }
      applyEach(androidTestDependencies) { addProjectDependency("androidTestImplementation", it) }
      applyEach(testDependencies) { addProjectDependency("testImplementation", it) }
    }
  )
    .applyEach(fqClassName.toList()) { fq ->
      addSrcSpec(
        ProjectSrcSpec(Path.of("src/main/kotlin")) {
          addFileSpec(
            FileSpec.builder(fq.packageName, fq.simpleName)
              .addType(TypeSpec.classBuilder(fq.simpleName).build())
              .build()
          )
        }
      )
    }
}
