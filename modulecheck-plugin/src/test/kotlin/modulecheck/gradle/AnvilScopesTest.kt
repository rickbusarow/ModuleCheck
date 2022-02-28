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

import com.squareup.kotlinpoet.AnnotationSpec
import com.squareup.kotlinpoet.ClassName
import com.squareup.kotlinpoet.FileSpec
import com.squareup.kotlinpoet.TypeSpec
import modulecheck.specs.ProjectBuildSpec
import modulecheck.specs.ProjectBuildSpecBuilder
import modulecheck.specs.ProjectSettingsSpecBuilder
import modulecheck.specs.ProjectSpec
import modulecheck.specs.ProjectSrcSpec
import modulecheck.specs.applyEach
import org.junit.jupiter.api.Test
import java.nio.file.Path

class AnvilScopesTest : BasePluginTest() {

  val projects = List(10) {
    ProjectSpec.builder("lib-$it")
      .build()
  }

  val projectSettings = ProjectSettingsSpecBuilder {
    projects.forEach { project ->
      addInclude(project.gradlePath)
    }
    addInclude("app")
  }

  val projectBuild = ProjectBuildSpecBuilder()
    .addPlugin("id(\"com.rickbusarow.module-check\")")
    .buildScript()

  val jvmSub1 = jvmSubProject("lib-1", ClassName("com.example.lib1", "Lib1Class"))
  val jvmSub2 = jvmSubProject("lib-2", ClassName("com.example.lib2", "Lib2Class"))

  @Test
  fun `module which contributes anvil scopes should not be unused in module which merges that scope`() {
    jvmSub2.edit {
      projectBuildSpec?.edit {
        addPlugin("id(\"com.squareup.anvil\")")
        addExternalDependency("implementation", "com.google.dagger:dagger:2.38.1")
        addProjectDependency("api", jvmSub1)
      }
      addSrcSpec(
        ProjectSrcSpec(Path.of("src/main/kotlin/com/example/lib2")) {
          addRawFile(
            "Lib2FooImpl.kt",
            """package com.example.lib2
            |
            |import com.example.lib1.Lib1Class
            |import com.squareup.anvil.annotations.ContributesBinding
            |
            |@ContributesBinding(Lib1Class::class)
            |public class Lib2FooImpl : Foo
            |
            |interface Foo
            |
            """.trimMargin()
          )
        }
      )
    }

    val appProject = ProjectSpec("app") {
      addBuildSpec(
        ProjectBuildSpec {
          addPlugin("kotlin(\"jvm\")")
          addPlugin("id(\"com.squareup.anvil\")")
          addExternalDependency("implementation", "com.google.dagger:dagger:2.38.1")
          addProjectDependency("api", jvmSub1)
          addProjectDependency("api", jvmSub2)
        }
      )
      addSrcSpec(
        ProjectSrcSpec(Path.of("src/main/kotlin/com/example/app")) {
          addRawFile(
            "AppComponent.kt",
            """package com.example.app

            |import com.example.lib1.Lib1Class
            |import com.squareup.anvil.annotations.MergeComponent
            |
            |@MergeComponent(Lib1Class::class)
            |public interface AppComponent
            |
            """.trimMargin()
          )
        }
      )
    }
    ProjectSpec("project") {
      applyEach(projects) { project ->
        addSubproject(project)
      }
      addSubproject(appProject)
      addSubprojects(jvmSub1, jvmSub2)
      addSettingsSpec(projectSettings.build())
      addBuildSpec(projectBuild.build())
    }
      .writeIn(testProjectDir.toPath())

    shouldSucceed("moduleCheck")
  }

  @Test
  fun `module which contributes anvil scopes with named argument should not be unused in module which merges that scope`() {
    jvmSub2.edit {
      projectBuildSpec?.edit {
        addPlugin("id(\"com.squareup.anvil\")")
      }
      addSrcSpec(
        ProjectSrcSpec(Path.of("src/main/kotlin")) {
          addRawFile(
            "AppComponent.kt",
            """package com.example.lib2
            |
            |import com.example.lib1.Lib1Class
            |import com.squareup.anvil.annotations.ContributesBinding
            |
            |@ContributesBinding(scope = Lib1Class::class)
            |public class Lib2FooImpl : Foo
            |
            |interface Foo
            |
            """.trimMargin()
          )
        }
      )
    }

    val appProject = ProjectSpec("app") {
      addBuildSpec(
        ProjectBuildSpec {
          addPlugin("kotlin(\"jvm\")")
          addPlugin("id(\"com.squareup.anvil\")")
          addProjectDependency("api", jvmSub1)
          addProjectDependency("api", jvmSub2)
        }
      )
      addSrcSpec(
        ProjectSrcSpec(Path.of("src/main/kotlin")) {
          addRawFile(
            "AppComponent.kt",
            """package com.example.app

            |import com.example.lib1.Lib1Class
            |import com.squareup.anvil.annotations.MergeComponent
            |
            |@MergeComponent(Lib1Class::class)
            |public interface AppComponent
            |
            """.trimMargin()
          )
        }
      )
    }
    ProjectSpec("project") {
      applyEach(projects) { project ->
        addSubproject(project)
      }
      addSubproject(appProject)
      addSubprojects(jvmSub1, jvmSub2)
      addSettingsSpec(projectSettings.build())
      addBuildSpec(projectBuild.build())
    }
      .writeIn(testProjectDir.toPath())

    shouldSucceed("moduleCheck")
  }

  @Test
  fun `module which contributes anvil scopes with named argument and wrong order should not be unused in module which merges that scope`() {
    val appComponent = FileSpec.builder("com.example.app", "AppComponent")
      .addType(
        TypeSpec.classBuilder("AppComponent")
          .addAnnotation(
            AnnotationSpec
              .builder(ClassName.bestGuess("com.squareup.anvil.annotations.MergeComponent"))
              .addMember("%T::class", ClassName.bestGuess("com.example.lib1.Lib1Class"))
              .build()
          )
          .build()
      )
      .build()

    val lib2Component = ClassName("com.example.lib2", "Lib2Component")

    jvmSub2.edit {
      projectBuildSpec?.edit {
        addPlugin("id(\"com.squareup.anvil\")")
      }
      addSrcSpec(
        ProjectSrcSpec(Path.of("src/main/kotlin")) {
          addFileSpec(
            FileSpec.builder(lib2Component.packageName, lib2Component.simpleName)
              .addType(
                TypeSpec.classBuilder(lib2Component.simpleName)
                  .addAnnotation(
                    AnnotationSpec
                      .builder(ClassName.bestGuess("com.squareup.anvil.annotations.ContributesBinding"))
                      .addMember("replaces = []")
                      .addMember(
                        "scope = %T::class",
                        ClassName.bestGuess("com.example.lib1.Lib1Class")
                      )
                      .build()
                  )
                  .build()
              )
              .build()
          )
        }
      )
    }

    val appProject = ProjectSpec("app") {
      addBuildSpec(
        ProjectBuildSpec {
          addPlugin("kotlin(\"jvm\")")
          addPlugin("id(\"com.squareup.anvil\")")
          addProjectDependency("api", jvmSub1)
          addProjectDependency("api", jvmSub2)
        }
      )
      addSrcSpec(
        ProjectSrcSpec(Path.of("src/main/kotlin")) {
          addFileSpec(appComponent)
        }
      )
    }
    ProjectSpec("project") {
      applyEach(projects) { project ->
        addSubproject(project)
      }
      addSubproject(appProject)
      addSubprojects(jvmSub1, jvmSub2)
      addSettingsSpec(projectSettings.build())
      addBuildSpec(projectBuild.build())
    }
      .writeIn(testProjectDir.toPath())

    shouldSucceed("moduleCheck")
  }

  @Test
  fun `module which contributes anvil scopes should not be unused in module which merges that scope with named argument`() {
    val appComponent = FileSpec.builder("com.example.app", "AppComponent")
      .addType(
        TypeSpec.classBuilder("AppComponent")
          .addAnnotation(
            AnnotationSpec
              .builder(ClassName.bestGuess("com.squareup.anvil.annotations.MergeComponent"))
              .addMember("scope = %T::class", ClassName.bestGuess("com.example.lib1.Lib1Class"))
              .build()
          )
          .build()
      )
      .build()

    val lib2Component = ClassName("com.example.lib2", "Lib2Component")

    jvmSub2.edit {
      projectBuildSpec?.edit {
        addPlugin("id(\"com.squareup.anvil\")")
      }
      addSrcSpec(
        ProjectSrcSpec(Path.of("src/main/kotlin")) {
          addFileSpec(
            FileSpec.builder(lib2Component.packageName, lib2Component.simpleName)
              .addType(
                TypeSpec.classBuilder(lib2Component.simpleName)
                  .addAnnotation(
                    AnnotationSpec
                      .builder(ClassName.bestGuess("com.squareup.anvil.annotations.ContributesBinding"))
                      .addMember(
                        "scope = %T::class",
                        ClassName.bestGuess("com.example.lib1.Lib1Class")
                      )
                      .build()
                  )
                  .build()
              )
              .build()
          )
        }
      )
    }

    val appProject = ProjectSpec("app") {
      addBuildSpec(
        ProjectBuildSpec {
          addPlugin("kotlin(\"jvm\")")
          addPlugin("id(\"com.squareup.anvil\")")
          addProjectDependency("api", jvmSub1)
          addProjectDependency("api", jvmSub2)
        }
      )
      addSrcSpec(
        ProjectSrcSpec(Path.of("src/main/kotlin")) {
          addFileSpec(appComponent)
        }
      )
    }
    ProjectSpec("project") {
      applyEach(projects) { project ->
        addSubproject(project)
      }
      addSubproject(appProject)
      addSubprojects(jvmSub1, jvmSub2)
      addSettingsSpec(projectSettings.build())
      addBuildSpec(projectBuild.build())
    }
      .writeIn(testProjectDir.toPath())

    shouldSucceed("moduleCheck")
  }

  @Test
  fun `module which contributes anvil scopes should be unused in module which does not merge that scope`() {
    val appComponent = FileSpec.builder("com.example.app", "AppComponent")
      .addType(
        TypeSpec.classBuilder("AppComponent")
          .addAnnotation(
            AnnotationSpec
              .builder(ClassName.bestGuess("com.squareup.anvil.annotations.ContributesTo"))
              .addMember("%T::class", ClassName.bestGuess("com.example.lib1.Lib1Class"))
              .build()
          )
          .build()
      )
      .build()

    val lib2Component = ClassName("com.example.lib2", "Lib2Component")

    jvmSub2.edit {
      projectBuildSpec?.edit {
        addPlugin("id(\"com.squareup.anvil\")")
      }
      addSrcSpec(
        ProjectSrcSpec(Path.of("src/main/kotlin")) {
          addFileSpec(
            FileSpec.builder(lib2Component.packageName, lib2Component.simpleName)
              .addType(
                TypeSpec.classBuilder(lib2Component.simpleName)
                  .addAnnotation(
                    AnnotationSpec
                      .builder(ClassName.bestGuess("com.squareup.anvil.annotations.ContributesTo"))
                      .addMember("%T::class", ClassName.bestGuess("com.example.lib1.Lib1Class"))
                      .build()
                  )
                  .build()
              )
              .build()
          )
        }
      )
    }

    val appProject = ProjectSpec("app") {
      addBuildSpec(
        ProjectBuildSpec {
          addPlugin("kotlin(\"jvm\")")
          addPlugin("id(\"com.squareup.anvil\")")
          addProjectDependency("api", jvmSub1)
          addProjectDependency("api", jvmSub2)
        }
      )
      addSrcSpec(
        ProjectSrcSpec(Path.of("src/main/kotlin")) {
          addFileSpec(appComponent)
        }
      )
    }
    ProjectSpec("project") {
      applyEach(projects) { project ->
        addSubproject(project)
      }
      addSubproject(appProject)
      addSubprojects(jvmSub1, jvmSub2)
      addSettingsSpec(projectSettings.build())
      addBuildSpec(projectBuild.build())
    }
      .writeIn(testProjectDir.toPath())

    shouldFail("moduleCheck") withTrimmedMessage """
    :app
               configuration    dependency    name                source    build file
            X  api              :lib-2        unusedDependency              /app/build.gradle.kts: (8, 3):

    ModuleCheck found 1 issue
    """
  }
}
