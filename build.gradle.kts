buildscript {
  repositories {
    mavenCentral()
    google()
    jcenter()
    maven("https://plugins.gradle.org/m2/")
    maven("https://oss.sonatype.org/content/repositories/snapshots")
  }
  dependencies {
    classpath("com.android.tools.build:gradle:4.1.0")
    classpath("com.google.firebase:firebase-crashlytics-gradle:2.3.0")
    classpath("com.google.gms:google-services:4.3.4")
    classpath("org.jetbrains.kotlin:kotlin-gradle-plugin:1.4.20")
    classpath("com.vanniktech:gradle-maven-publish-plugin:0.13.0")
    classpath("com.jaredsburrows:gradle-spoon-plugin:1.5.0")
    classpath("org.jetbrains.kotlinx:kotlinx-knit:0.2.2")
    classpath("com.squareup.anvil:gradle-plugin:2.0.6")
  }
}

plugins {
  kotlin("jvm")
  id(Plugins.spotless) version Versions.spotless
  id("com.rickbusarow.module-check")
}

// apply<ModuleCheck>()

allprojects {

  repositories {
    google()
    mavenCentral()
    maven("https://oss.sonatype.org/content/repositories/snapshots")
    maven("https://jitpack.io")
    jcenter()
    maven("https://s3-us-west-2.amazonaws.com/si-mobile-sdks/android/")
  }

//  dependencies {
//
//  }
//
//  tasks.withType<KotlinCompile>()
//      .configureEach {
//
//        kotlinOptions {
//
//          freeCompilerArgs = listOf(
//              "-Xinline-classes",
//              "-Xopt-in=kotlin.ExperimentalStdlibApi",
//              "-Xuse-experimental=kotlin.contracts.ExperimentalContracts"
//          )
//        }
//      }
}

configure<com.diffplug.gradle.spotless.SpotlessExtension> {
  java {
    target("src/*/java/**/*.java")
    googleJavaFormat("1.7")
    removeUnusedImports()
    trimTrailingWhitespace()
    endWithNewline()
  }
  kotlin {
    target("**/src/**/*.kt", "**/src/**/*.kt")
    ktlint("0.40.0")
      .userData(
        mapOf(
          "indent_size" to "2",
          "continuation_indent_size" to "2",
          "max_line_length" to "off",
          "disabled_rules" to "no-wildcard-imports",
          "ij_kotlin_imports_layout" to "*,java.**,javax.**,kotlin.**,^"
        )
      )
    trimTrailingWhitespace()
    endWithNewline()
  }
  kotlinGradle {
    target("*.gradle.kts")
    ktlint("0.40.0")
      .userData(
        mapOf(
          "indent_size" to "2",
          "continuation_indent_size" to "2",
          "max_line_length" to "off",
          "disabled_rules" to "no-wildcard-imports",
          "ij_kotlin_imports_layout" to "*,java.**,javax.**,kotlin.**,^"
        )
      )
  }
}
