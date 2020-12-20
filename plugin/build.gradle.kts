plugins {
  kotlin("jvm")
  id("java-gradle-plugin")
  `kotlin-dsl`
  `maven-publish`
  id("com.gradle.plugin-publish") version "0.12.0"
}

repositories {
  mavenCentral()
  google()
  jcenter()
  maven("https://oss.sonatype.org/content/repositories/snapshots")
}
tasks.withType<Test> {
  useJUnitPlatform()
}
dependencies {
  compileOnly(gradleApi())

  val kotlinVersion = "1.4.21"

  testImplementation(project(path = ":testing"))

  implementation(kotlin("gradle-plugin", version = kotlinVersion))
  implementation(kotlin("stdlib", version = kotlinVersion))
  implementation(kotlin("reflect", version = kotlinVersion))

  implementation("com.android.tools.build:gradle:4.1.0")
  implementation("com.squareup:kotlinpoet:1.7.2")
  implementation("com.github.javaparser:javaparser-symbol-solver-core:3.17.0")
  implementation("org.jetbrains.kotlin:kotlin-compiler-embeddable:$kotlinVersion")
  implementation("org.jetbrains.kotlin:kotlin-gradle-plugin:$kotlinVersion")
  implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.4.2")

  testImplementation("io.kotest:kotest-runner-junit5:4.3.2")
  testImplementation("io.kotest:kotest-assertions-core:4.3.2")
  testImplementation("io.kotest:kotest-property:4.3.2")
  testImplementation("org.junit.jupiter:junit-jupiter-api:5.7.0")
  testImplementation("org.junit.jupiter:junit-jupiter-params:5.7.0")
  testImplementation("org.junit.jupiter:junit-jupiter-engine:5.7.0")
}

java {
  sourceCompatibility = JavaVersion.VERSION_1_8
  targetCompatibility = JavaVersion.VERSION_1_8
}

gradlePlugin {
  plugins {
    create("moduleCheck") {
      id = PluginCoordinates.ID
      group = PluginCoordinates.GROUP
      implementationClass = PluginCoordinates.IMPLEMENTATION_CLASS
      version = PluginCoordinates.VERSION
    }
  }
}
object PluginCoordinates {
  const val ID = "com.rickbusarow.module-check"
  const val GROUP = "com.rickbusarow.modulecheck"
  const val VERSION = "0.10.0"
  const val IMPLEMENTATION_CLASS = "com.rickbusarow.modulecheck.ModuleCheckPlugin"
}

object PluginBundle {
  const val VCS = "https://github.com/RBusarow/ModuleCheck"
  const val WEBSITE = "https://github.com/RBusarow/ModuleCheck"
  const val DESCRIPTION = "Fast dependency graph validation for gradle"
  const val DISPLAY_NAME = "Fast dependency graph validation for gradle"
  val TAGS = listOf("plugin", "gradle")
}

// Configuration Block for the Plugin Marker artifact on Plugin Central
pluginBundle {
  website = PluginBundle.WEBSITE
  vcsUrl = PluginBundle.VCS
  description = PluginBundle.DESCRIPTION
  tags = PluginBundle.TAGS

  plugins {
    getByName("moduleCheck") {
      displayName = PluginBundle.DISPLAY_NAME
    }
  }
}

tasks.create("setupPluginUploadFromEnvironment") {
  doLast {
    val key = System.getenv("GRADLE_PUBLISH_KEY")
    val secret = System.getenv("GRADLE_PUBLISH_SECRET")

    if (key == null || secret == null) {
      throw GradleException("gradlePublishKey and/or gradlePublishSecret are not defined environment variables")
    }

    System.setProperty("gradle.publish.key", key)
    System.setProperty("gradle.publish.secret", secret)
  }
}
