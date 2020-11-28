import com.android.SdkConstants
import com.android.build.gradle.internal.cxx.logging.infoln
import com.google.common.base.Charsets
import org.gradle.api.DefaultTask
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.tasks.TaskAction
import java.io.File
import java.io.FileInputStream
import java.io.InputStreamReader
import java.time.Instant
import java.util.*

abstract class MyPlugin : Plugin<Project> {

  override fun apply(target: Project) {

    target.afterEvaluate {

      val foobieValue = gradleLocalProperties(target.rootDir).getProperty("foobie2", "123").toInt()


      val now = Instant.now()

      val nowString = now.toString()

      val read = Instant.parse(nowString)

      target.tasks.register("myTask", MyPropertyTask::class.java)

    }
  }
}

open class MyPropertyTask : DefaultTask() {

  @TaskAction
  fun writeThings() {

    val file = project.file("${project.rootProject.rootDir}/local.properties")

    val txt = file.readText()

    val new = txt + "\nnow=${Instant.now()}"

    file.writeText(new)
  }
}

/**
 * Retrieve the project local properties if they are available.
 * If there is no local properties file then an empty set of properties is returned.
 */
fun gradleLocalProperties(projectRootDir: File): Properties {
  val properties = Properties()
  val localProperties = File(projectRootDir, SdkConstants.FN_LOCAL_PROPERTIES)

  if (localProperties.isFile) {
    InputStreamReader(FileInputStream(localProperties), Charsets.UTF_8).use { reader ->
      properties.load(reader)
    }
  } else {
    infoln("Gradle local properties file not found at $localProperties")
  }
  return properties
}

