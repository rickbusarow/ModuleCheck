plugins {
  kotlin("jvm")
  id("java-gradle-plugin")
  `kotlin-dsl`
//  id("com.gradle.plugin-publish")
}

repositories {
  mavenCentral()
  google()
  jcenter()
  maven("https://oss.sonatype.org/content/repositories/snapshots")
}

kotlinDslPluginOptions {
  experimentalWarning.set(false)
}

dependencies {
  compileOnly(gradleApi())

  val kotlinVersion = "1.4.10"

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
    create("kaapt") {
      id = "onekey.plugins.kapt"
      implementationClass = "onekey.plugins.OneKeyKaptPlugin"
//      version = PluginCoordinates.VERSION
    }
  }
}

// Configuration Block for the Plugin Marker artifact on Plugin Central
//pluginBundle {
//  website = PluginBundle.WEBSITE
//  vcsUrl = PluginBundle.VCS
//  description = PluginBundle.DESCRIPTION
//  tags = PluginBundle.TAGS
//
//  plugins {
//    getByName(PluginCoordinates.ID) {
//      displayName = PluginBundle.DISPLAY_NAME
//    }
//  }
//}

//tasks.create("setupPluginUploadFromEnvironment") {
//  doLast {
//    val key = System.getenv("GRADLE_PUBLISH_KEY")
//    val secret = System.getenv("GRADLE_PUBLISH_SECRET")
//
//    if (key == null || secret == null) {
//      throw GradleException("gradlePublishKey and/or gradlePublishSecret are not defined environment variables")
//    }
//
//    System.setProperty("gradle.publish.key", key)
//    System.setProperty("gradle.publish.secret", secret)
//  }
//}
