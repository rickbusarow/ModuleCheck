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

//kotlinDslPluginOptions {
//  experimentalWarning.set(false)
//}

dependencies {
  compileOnly(gradleApi())

  val kotlinVersion = "1.4.20"

  implementation(kotlin("gradle-plugin", version = kotlinVersion))
  implementation(kotlin("stdlib", version = kotlinVersion))
  implementation(kotlin("stdlib-common", version = kotlinVersion))
  implementation(kotlin("stdlib-jdk7", version = kotlinVersion))
  implementation(kotlin("stdlib-jdk8", version = kotlinVersion))
  implementation(kotlin("reflect", version = kotlinVersion))

//  implementation("com.android.tools.build:gradle:4.1.0")

  implementation("com.google.firebase:firebase-crashlytics-gradle:2.3.0") // update Dependencies.kt as well
  implementation("com.android.tools.build:gradle:4.1.0") // update Dependencies.kt as well
  implementation("com.jaredsburrows:gradle-spoon-plugin:1.5.0") // update Dependencies.kt as well
  implementation("com.squareup:kotlinpoet:1.6.0") // update Dependencies.kt as well
  implementation("org.jetbrains.kotlin:kotlin-compiler-embeddable:$kotlinVersion") // update Dependencies.kt as well
  implementation("org.jetbrains.kotlin:kotlin-gradle-plugin:$kotlinVersion") // update Dependencies.kt as well
  implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.4.2") // update Dependencies.kt as well

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

//publishing {
//
//  repositories {
//    maven {
//
//      credentials {
//
//        username = project.properties["SONATYPE_NEXUS_PASSWORD"] as? String ?: "--"
//        password = project.properties["SONATYPE_NEXUS_USERNAME"] as? String ?: "--"
//      }
//
//      url = uri("")
//    }
//  }
//
//  publications {
//
//    create<MavenPublication>("maven") {
//
////      artifact(tasks["shadowJar"])
//
//      pom {
//        name.set("OpenLink Engine Sequences")
//        description.set("Command and response engine for the OpenLink protocol")
//      }
//
//      groupId = "com.milwaukeetool.onekey"
//      artifactId = "openlink-engine-sequences"
//
//      val versionName: String by project
//
//      val timestamp =  SimpleDateFormat("yyyyMMddHHmm").format( Date())
//
//      version = "0.10.0"
//
//      pom {
//        url.set("https://github.com/mkeonekey/android-onekey")
//
//        developers {
//          developer {
//            id.set("onekey")
//            name.set("One-Key")
//          }
//        }
//        scm {
//          connection.set("scm:git:https://github.com/mkeonekey/android-onekey.git")
//          developerConnection.set("scm:git:ssh://github.com/mkeonekey/android-onekey.git")
//          url.set("https://github.com/mkeonekey/android-onekey")
//        }
//      }
//    }
//  }
//}
