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

plugins {
  javaLibrary
}

dependencies {

  compileOnly("org.codehaus.groovy:groovy-xml:3.0.7")

  compileOnly(gradleApi())

  compileOnly(project(path = ":modulecheck-specs"))

  api(BuildPlugins.androidGradlePlugin)
  api(Libs.JUnit.api)
  api(Libs.JUnit.engine)
  api(Libs.JUnit.params)
  api(Libs.Kotest.assertions)
  api(Libs.Kotest.properties)
  api(Libs.Kotest.runner)
  api(Libs.Kotlin.compiler)
  api(Libs.Kotlin.gradlePlugin)
  api(Libs.Kotlin.reflect)
  api(Libs.RickBusarow.Hermit.core)
  api(Libs.RickBusarow.Hermit.junit5)
  api(Libs.Square.KotlinPoet.core)
  api(Libs.Swiftzer.semVer)
  api(Libs.javaParser)
}
