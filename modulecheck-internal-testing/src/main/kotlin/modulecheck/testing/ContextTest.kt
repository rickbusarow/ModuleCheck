package modulecheck.testing

import hermit.test.junit.HermitJUnit5
import hermit.test.resets
import modulecheck.api.*
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.TestInfo
import java.io.File
import java.util.concurrent.ConcurrentHashMap

abstract class ContextTest : HermitJUnit5(), DynamicTests {

  val projectDir by tempDir()

  fun File.relativePath() = path.removePrefix(projectDir.path)

  private var testInfo: TestInfo? = null

  private val projectCache by resets { ConcurrentHashMap<String, Project2>() }

  fun project(
    gradlePath: String = ":",
    projectDir: File = this@ContextTest.projectDir,
    buildFile: File = File(projectDir, "build.gradle.kts"),
    configurations: Map<String, Config> = emptyMap(),
    projectDependencies: Lazy<Map<ConfigurationName, List<ConfiguredProjectDependency>>> = lazy { emptyMap() },
    hasKapt: Boolean = false,
    sourceSets: Map<SourceSetName, SourceSet> = emptyMap(),
    projectCache: ConcurrentHashMap<String, Project2> = this@ContextTest.projectCache,
    anvilGradlePlugin: AnvilGradlePlugin? = null
  ): Project2 {

    return projectCache.getOrPut(gradlePath) {
      Project2Impl(
        path = gradlePath,
        projectDir = projectDir,
        buildFile = buildFile,
        configurations = configurations,
        projectDependencies = projectDependencies,
        hasKapt = hasKapt,
        sourceSets = sourceSets,
        projectCache = projectCache,
        anvilGradlePlugin = anvilGradlePlugin
      )
    }
  }

  @BeforeEach
  fun beforeEach(testInfo: TestInfo) {
    this.testInfo = testInfo
  }

  @AfterEach
  fun afterEach() {
    testInfo = null
  }
}
