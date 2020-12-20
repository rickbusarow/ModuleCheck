plugins {
  javaLibrary
}

dependencies {

  compileOnly(gradleApi())

  implementation(Libs.Kotlin.gradlePlugin)
  implementation(Libs.Kotlin.reflect)
  implementation(Libs.javaParser)
  implementation(Libs.Kotlin.compiler)
  implementation(Libs.Square.KotlinPoet.core)
  testImplementation(Libs.Kotest.assertions)
  testImplementation(Libs.Kotest.properties)
  testImplementation(Libs.Kotest.runner)
  testImplementation(Libs.JUnit.api)
  testImplementation(Libs.JUnit.engine)
  testImplementation(Libs.JUnit.params)
}
