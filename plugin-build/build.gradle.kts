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

allprojects {

  repositories {
    google()
    mavenCentral()
    maven("https://oss.sonatype.org/content/repositories/snapshots")
    maven("https://jitpack.io")
    jcenter()
    maven("https://s3-us-west-2.amazonaws.com/si-mobile-sdks/android/")
  }
}

allprojects {


  tasks.withType<org.jetbrains.kotlin.gradle.tasks.KotlinCompile>()
    .configureEach {

      kotlinOptions {

        // allWarningsAsErrors = true

        jvmTarget = "1.8"

        val args = listOf(
          "-Xallow-result-return-type",
          "-Xopt-in=kotlin.contracts.ExperimentalContracts",
          "-Xopt-in=kotlin.Experimental",
          "-Xopt-in=kotlin.time.ExperimentalTime",
          "-Xopt-in=kotlin.RequiresOptIn",
          "-Xinline-classes",
          "-Xuse-experimental=kotlinx.coroutines.ExperimentalCoroutinesApi",
          "-Xuse-experimental=kotlinx.coroutines.FlowPreview"
        )

        // https://youtrack.jetbrains.com/issue/KT-24946
        freeCompilerArgs = args
      }
    }
}
