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

package modulecheck.project.test

import com.rickbusarow.kase.ParamTestEnvironmentFactory
import com.rickbusarow.kase.files.TestLocation
import kotlinx.coroutines.flow.filterIsInstance
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import modulecheck.api.context.jvmFiles
import modulecheck.config.CodeGeneratorBinding
import modulecheck.config.internal.defaultCodeGeneratorBindings
import modulecheck.model.dependency.ConfigurationName
import modulecheck.model.dependency.ProjectDependency
import modulecheck.model.dependency.TypeSafeProjectPathResolver
import modulecheck.model.dependency.impl.RealConfiguredProjectDependencyFactory
import modulecheck.model.sourceset.SourceSetName
import modulecheck.parsing.kotlin.compiler.impl.DependencyModuleDescriptorAccess
import modulecheck.parsing.source.JavaFile
import modulecheck.parsing.source.KotlinFile
import modulecheck.project.McProject
import modulecheck.project.ProjectCache
import modulecheck.project.generation.ProjectCollector
import modulecheck.testing.TestEnvironment
import modulecheck.testing.TestEnvironmentParams
import modulecheck.utils.trace.Trace
import org.intellij.lang.annotations.Language
import org.jetbrains.kotlin.config.JvmTarget
import java.io.File

data class ProjectTestEnvironmentParams(
  val projectCache: ProjectCache
) : TestEnvironmentParams

class ProjectTestEnvironmentFactory : ParamTestEnvironmentFactory<ProjectTestEnvironmentParams, ProjectTestEnvironment> {
  override fun create(
    params: ProjectTestEnvironmentParams,
    names: List<String>,
    location: TestLocation
  ): ProjectTestEnvironment = ProjectTestEnvironment(params.projectCache, names, location)
}

/**
 * A specialized [TestEnvironment] for project-related tests.
 * Provides utility functions for creating files and dependencies.
 */
open class ProjectTestEnvironment(
  override val projectCache: ProjectCache,
  names: List<String>,
  testLocation: TestLocation = TestLocation.get()
) : TestEnvironment(names = names, testLocation = testLocation),
  ProjectCollector {
  override val codeGeneratorBindings: List<CodeGeneratorBinding> by lazy {
    defaultCodeGeneratorBindings()
  }

  override val dependencyModuleDescriptorAccess: DependencyModuleDescriptorAccess by lazy {
    DependencyModuleDescriptorAccess(projectCache)
  }

  override val root: File get() = workingDir

  val projectDependencyFactory: ProjectDependency.Factory by lazy {
    RealConfiguredProjectDependencyFactory(
      pathResolver = TypeSafeProjectPathResolver(projectProvider),
      generatorBindings = codeGeneratorBindings
    )
  }

  constructor(params: ProjectTestEnvironmentParams) : this(
    projectCache = params.projectCache,
    testStackFrame = params.testStackFrame,
    testVariantNames = params.testVariantNames
  )

  /**
   * Adds a project dependency to the receiver [McProject].
   *
   * @param configurationName the configuration name of the dependency
   * @param project the project to be added as a dependency
   * @param asTestFixture whether the dependency should be added as a test fixture or not
   */
  fun McProject.addDependency(
    configurationName: ConfigurationName,
    project: McProject,
    asTestFixture: Boolean = false
  ) {
    val old = projectDependencies[configurationName].orEmpty()

    val cpd = projectDependencyFactory.create(
      configurationName = configurationName,
      path = project.projectPath,
      isTestFixture = asTestFixture
    )

    projectDependencies[configurationName] = old + cpd
  }

  /**
   * Creates a [JavaFile] and adds it to the receiver [McProject].
   *
   * @param content the content of the Java file
   * @param sourceSetName the source set name of the Java file
   * @return the created Java file
   */
  fun McProject.createJavaFile(
    @Language("java")
    content: String,
    sourceSetName: SourceSetName = SourceSetName.MAIN
  ): JavaFile = createJavaFile(
    content = content,
    project = this@createJavaFile,
    sourceSetName = sourceSetName
  )

  /**
   * Creates a [JavaFile] and adds it to the specified [McProject].
   *
   * @param content the content of the Java file
   * @param project the project to which the Java file will be added
   * @param sourceSetName the source set name of the Java file
   * @param jvmTarget the JVM target version for the Java file
   * @return the created Java file
   */
  fun ProjectTestEnvironment.createJavaFile(
    @Language("java")
    content: String,
    project: McProject = simpleProject(),
    sourceSetName: SourceSetName = SourceSetName.MAIN,
    jvmTarget: JvmTarget = JvmTarget.JVM_11
  ): JavaFile = runBlocking(Trace.start(listOf(ProjectTest::class))) {
    project.editSimple {
      addJavaSource(content, sourceSetName)
      this.jvmTarget = jvmTarget
    }.jvmFiles()
      .get(sourceSetName)
      .filterIsInstance<JavaFile>()
      .first { it.file.readText() == content.trimIndent() }
  }

  /**
   * Creates a [KotlinFile] and adds it to the receiver [McProject].
   *
   * @param content the content of the Kotlin file
   * @param sourceSetName the source set name of the Kotlin file
   * @return the created Kotlin file
   */
  fun McProject.createKotlinFile(
    @Language("kotlin")
    content: String,
    sourceSetName: SourceSetName = SourceSetName.MAIN
  ): KotlinFile = createKotlinFile(
    content = content,
    project = this@createKotlinFile,
    sourceSetName = sourceSetName
  )

  /**
   * Creates a [KotlinFile] and adds it to the specified [McProject].
   *
   * @param content the content of the Kotlin file
   * @param project the project to which the Kotlin file will be added
   * @param sourceSetName the source set name of the Kotlin file
   * @param jvmTarget the JVM target version for the Kotlin file
   * @return the created Kotlin file
   */
  fun ProjectTestEnvironment.createKotlinFile(
    @Language("kotlin")
    content: String,
    project: McProject = simpleProject(),
    sourceSetName: SourceSetName = SourceSetName.MAIN,
    jvmTarget: JvmTarget = JvmTarget.JVM_11
  ): KotlinFile = runBlocking(Trace.start(listOf(ProjectTest::class))) {
    project.editSimple {
      addKotlinSource(content, sourceSetName)
      this.jvmTarget = jvmTarget
    }.jvmFiles()
      .get(sourceSetName)
      .filterIsInstance<KotlinFile>()
      .first { it.psi.text == content.trimIndent() }
  }
}
