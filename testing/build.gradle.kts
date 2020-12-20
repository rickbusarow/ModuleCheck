plugins {
  kotlin("jvm")
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

java {
  sourceCompatibility = JavaVersion.VERSION_1_8
  targetCompatibility = JavaVersion.VERSION_1_8
}

dependencies {
  compileOnly(gradleApi())

  val kotlinVersion = "1.4.21"

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
