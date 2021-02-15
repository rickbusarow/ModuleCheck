/*
 * Copyright (C) 2021 Rick Busarow
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

object Plugins {

  const val androidApplication = "com.android.application"
  const val androidLibrary = "com.android.library"
  const val anvil = "com.squareup.anvil"
  const val atomicFu = "kotlinx-atomicfu"
  const val benManes = "com.github.ben-manes.versions"
  const val changeTracker = "com.ismaeldivita.changetracker"
  const val crashlytics = "com.google.firebase.crashlytics"
  const val dependencyAnalysis = "com.autonomousapps.dependency-analysis"
  const val detekt = "io.gitlab.arturbosch.detekt"
  const val dokka = "org.jetbrains.dokka"
  const val gradleDoctor = "com.osacky.doctor"
  const val javaLibrary = "java-library"
  const val knit = "kotlinx-knit"
  const val kotlinter = "org.jmailen.kotlinter"
  const val mavenPublish = "com.vanniktech.maven.publish"
  const val spotless = "com.diffplug.spotless"
  const val taskTree = "com.dorongold.task-tree"
}

object Versions {

  const val androidTools = "4.1.2"
  const val anvil = "2.0.6"
  const val benManes = "0.36.0"
  const val changeTracker = "0.7.3"
  const val compileSdk = 30
  const val dependencyAnalysis = "0.63.0"
  const val dokka = "1.4.20"
  const val gradleDoctor = "0.7.0"
  const val knit = "0.2.3"
  const val kotlin = "1.4.30"
  const val kotlinter = "3.3.0"
  const val mavenPublish = "0.13.0"
  const val minSdk = "23"
  const val sonarPlugin = "2.6.1"
  const val spoon = "1.5.0"
  const val spotless = "5.10.1"
  const val targetSdk = 30
  const val taskTree = "1.5"
  const val versionName = "0.10.0"
}

object BuildPlugins {

  const val androidGradlePlugin = "com.android.tools.build:gradle:${Versions.androidTools}"
  const val anvil = "com.squareup.anvil:gradle-plugin:${Versions.anvil}"
  const val atomicFu = "org.jetbrains.kotlinx:atomicfu-gradle-plugin:0.14.1"
  const val crashlytics = "com.google.firebase:firebase-crashlytics-gradle:2.3.0"
  const val dokka = "org.jetbrains.dokka:dokka-gradle-plugin:${Versions.dokka}"
  const val gradleMavenPublish =
    "com.vanniktech:gradle-maven-publish-plugin:${Versions.mavenPublish}"
  const val kotlinGradlePlugin = "org.jetbrains.kotlin:kotlin-gradle-plugin:${Versions.kotlin}"
  const val kotlinter = "org.jmailen.gradle:kotlinter-gradle:${Versions.kotlinter}"
  const val spotless = "com.diffplug.spotless:spotless-plugin-gradle:${Versions.spotless}"
}

object Libs {

  object Detekt {

    const val version = "1.15.0"
    const val api = "io.gitlab.arturbosch.detekt:detekt-api:$version"
    const val cli = "io.gitlab.arturbosch.detekt:detekt-cli:$version"
    const val formatting = "io.gitlab.arturbosch.detekt:detekt-formatting:$version"
    const val test = "io.gitlab.arturbosch.detekt:detekt-test:$version"
  }

  object JUnit {
    private const val version = "5.7.1"

    const val api = "org.junit.jupiter:junit-jupiter-api:$version"
    const val params = "org.junit.jupiter:junit-jupiter-params:$version"
    const val engine = "org.junit.jupiter:junit-jupiter-engine:$version"
    const val vintage = "org.junit.vintage:junit-vintage-engine:$version"
  }

  const val javaParser = "com.github.javaparser:javaparser-symbol-solver-core:3.18.0"

  object Kotlin {
    const val compiler = "org.jetbrains.kotlin:kotlin-compiler-embeddable:${Versions.kotlin}"
    const val stdlib = "org.jetbrains.kotlin:kotlin-stdlib-jdk8:${Versions.kotlin}"
    const val reflect = "org.jetbrains.kotlin:kotlin-reflect:${Versions.kotlin}"
    const val gradlePlugin = "org.jetbrains.kotlin:kotlin-gradle-plugin:${Versions.kotlin}"
    const val extensions = "org.jetbrains.kotlin:kotlin-android-extensions:${Versions.kotlin}"
  }

  object Kotest {
    private const val version = "4.4.1"
    const val assertions = "io.kotest:kotest-assertions-core-jvm:$version"
    const val properties = "io.kotest:kotest-property-jvm:$version"
    const val runner = "io.kotest:kotest-runner-junit5-jvm:$version"
  }

  object Kotlinx {
    object Coroutines {
      private const val version = "1.4.2"
      const val core = "org.jetbrains.kotlinx:kotlinx-coroutines-core:$version"
      const val coreJvm = "org.jetbrains.kotlinx:kotlinx-coroutines-core-jvm:$version"
      const val android = "org.jetbrains.kotlinx:kotlinx-coroutines-android:$version"
      const val test = "org.jetbrains.kotlinx:kotlinx-coroutines-test:$version"
    }

    object Knit {
      const val test = "org.jetbrains.kotlinx:kotlinx-knit-test:${Versions.knit}"
    }
  }

  object RickBusarow {

    object Hermit {
      private const val version = "0.9.2"
      const val core = "com.rickbusarow.hermit:hermit-core:$version"
      const val junit4 = "com.rickbusarow.hermit:hermit-junit4:$version"
      const val junit5 = "com.rickbusarow.hermit:hermit-junit5:$version"
      const val mockk = "com.rickbusarow.hermit:hermit-mockk:$version"
      const val coroutines = "com.rickbusarow.hermit:hermit-coroutines:$version"
    }

    object Dispatch {

      private const val version = "1.0.0-beta04"

      const val core = "com.rickbusarow.dispatch:dispatch-core:$version"
      const val detekt = "com.rickbusarow.dispatch:dispatch-detekt:$version"
      const val espresso = "com.rickbusarow.dispatch:dispatch-android-espresso:$version"
      const val extensions = "com.rickbusarow.dispatch:dispatch-extensions:$version"
      const val lifecycle = "com.rickbusarow.dispatch:dispatch-android-lifecycle:$version"
      const val lifecycleExtensions =
        "com.rickbusarow.dispatch:dispatch-android-lifecycle-extensions:$version"
      const val viewModel = "com.rickbusarow.dispatch:dispatch-android-viewmodel:$version"

      object Test {
        const val core = "com.rickbusarow.dispatch:dispatch-test:$version"
        const val jUnit4 = "com.rickbusarow.dispatch:dispatch-test-junit4:$version"
        const val jUnit5 = "com.rickbusarow.dispatch:dispatch-test-junit5:$version"
      }
    }
  }

  object Square {

    const val javaPoet = "com.squareup:javapoet:1.12.1"

    const val okio = "com.squareup.okio:okio:2.4.1"
    const val picasso = "com.squareup.picasso:picasso:2.5.2"
    const val spoon = "com.squareup.spoon:spoon-client:1.7.1"

    object KotlinPoet {
      private const val version = "1.7.2"

      const val core =
        "com.squareup:kotlinpoet:$version" // update the buildSrc gradle dependency too!
      const val classInspectorElements = "com.squareup:kotlinpoet-classinspector-elements:$version"
      const val metadata = "com.squareup:kotlinpoet-metadata:$version"
      const val metadataSpecs = "com.squareup:kotlinpoet-metadata-specs:$version"
    }

    object Moshi {
      private const val version = "1.11.0"

      const val core = "com.squareup.moshi:moshi:$version"
      const val adapters = "com.squareup.moshi:moshi-adapters:$version"
      const val kotlin = "com.squareup.moshi:moshi-kotlin:$version"
      const val kotlinCodegen = "com.squareup.moshi:moshi-kotlin-codegen:$version"
    }

    object OkHttp {
      private const val version = "4.3.1"

      const val mockWebServer = "com.squareup.okhttp3:mockwebserver:$version"
      const val core = "com.squareup.okhttp3:okhttp:$version"
      const val logging = "com.squareup.okhttp3:logging-interceptor:$version"
    }

    object Retrofit {
      private const val version = "2.7.1"

      const val core = "com.squareup.retrofit2:retrofit:$version"
      const val moshi = "com.squareup.retrofit2:converter-moshi:$version"
      const val mock = "com.squareup.retrofit2:retrofit-mock:$version"
    }
  }

  object Swiftzer {
    const val semVer = "net.swiftzer.semver:semver:1.1.1"
  }
}
