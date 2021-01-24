@file:Suppress("MatchingDeclarationName")

/*
 * Copyright (C) 2020 Rick Busarow
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

package com.rickbusarow.modulecheck.parser

data class KaptProcessor(val coordinates: String)

class KaptMatcher(
  val name: String,
  val processor: String,
  annotationImports: List<String>
) {

  val annotationImports = annotationImports.map { it.toRegex() }

  override fun equals(other: Any?): Boolean {
    if (this === other) return true
    if (other !is KaptMatcher) return false

    if (processor != other.processor) return false

    return true
  }

  override fun hashCode(): Int = processor.hashCode()

  override fun toString(): String {
    return """KaptMatcher(
      |  name='$name',
      |  processor='$processor',
      |  annotationImports=${annotationImports.joinToString("\n    ", "\n    ")}
      |)""".trimMargin()
  }
}

fun List<KaptMatcher>.asMap(): Map<String, KaptMatcher> = associateBy { it.processor }

internal val kaptMatchers: List<KaptMatcher> = listOf(
  KaptMatcher(
    name = "Dagger Hilt",
    processor = "com.google.dagger:hilt-compiler",
    annotationImports = listOf(
      "dagger\\.hilt\\.\\*",
      "dagger\\.hilt\\.DefineComponent",
      "dagger\\.hilt\\.EntryPoint",
      "dagger\\.hilt\\.InstallIn"
    )
  ),
  KaptMatcher(
    name = "Dagger Hilt Android",
    processor = "com.google.dagger:hilt-android-compiler",
    annotationImports = listOf(
      "dagger\\.hilt\\.\\*",
      "dagger\\.hilt\\.DefineComponent",
      "dagger\\.hilt\\.EntryPoint",
      "dagger\\.hilt\\.InstallIn",
      "dagger\\.hilt\\.android.AndroidEntryPoint",
      "dagger\\.hilt\\.android.HiltAndroidApp",
      "dagger\\.hilt\\.android.WithFragmentBindings"
    )
  ),
  KaptMatcher(
    name = "Moshi Kotlin codegen",
    processor = "com.squareup.moshi:moshi-kotlin-codegen",
    annotationImports = listOf(
      "com\\.squareup\\.moshi\\.\\*",
      "com\\.squareup\\.moshi\\.Json",
      "com\\.squareup\\.moshi\\.JsonClass"
    )
  ),
  KaptMatcher(
    name = "Dagger",
    processor = "com.google.dagger:dagger-compiler",
    annotationImports = listOf(
      "javax\\.inject\\.\\*",
      "javax\\.inject\\.Inject",
      "dagger\\.Binds",
      "dagger\\.Module",
      "dagger\\.multibindings\\.IntoMap",
      "dagger\\.multibindings\\.IntoSet",
      "dagger\\.BindsInstance",
      "dagger\\.Component",
      "dagger\\.assisted\\.\\*",
      "dagger\\.assisted\\.Assisted",
      "dagger\\.assisted\\.AssistedInject",
      "dagger\\.assisted\\.AssistedFactory"
    )
  ),
  KaptMatcher(
    name = "Dagger Android",
    processor = "com.google.dagger:dagger-android-processor",
    annotationImports = listOf(
      "dagger\\.android\\.\\*",
      "dagger\\.android\\.ContributesAndroidInjector"
    )
  ),
  KaptMatcher(
    name = "Inflation Inject", processor = "com.squareup.inject:inflation-inject-processor",
    annotationImports = listOf(
      "com\\.squareup\\.inject\\.inflation\\.\\*",
      "com\\.squareup\\.inject\\.inflation\\.InflationInject",
      "com\\.squareup\\.inject\\.inflation\\.InflationInjectModule"
    )
  ),
  KaptMatcher(
    name = "Assisted Inject Dagger",
    processor = "com.squareup.inject:assisted-inject-processor-dagger2",
    annotationImports = listOf(
      "com\\.squareup\\.inject\\.assisted\\.dagger2\\.\\*",
      "com\\.squareup\\.inject\\.assisted\\.dagger2\\.AssistedModule"
    )
  ),
  KaptMatcher(
    name = "Assisted Inject", processor = "com.squareup.inject:assisted-inject-processor",
    annotationImports = listOf(
      "com\\.squareup\\.inject\\.assisted\\.\\*",
      "com\\.squareup\\.inject\\.assisted\\.AssistedInject",
      "com\\.squareup\\.inject\\.assisted\\.AssistedInject.Factory",
      "com\\.squareup\\.inject\\.assisted\\.Assisted"
    )
  ),
  KaptMatcher(
    name = "Roomigrant",
    processor = "com.github.MatrixDev.Roomigrant:RoomigrantCompiler",
    annotationImports = listOf(
      "dev\\.matrix\\.roomigrant\\.\\*",
      "dev\\.matrix\\.roomigrant\\.GenerateRoomMigrations",
      "dev\\.matrix\\.roomigrant\\.rules\\.\\*",
      "dev\\.matrix\\.roomigrant\\.rules\\.FieldMigrationRule",
      "dev\\.matrix\\.roomigrant\\.rules\\.OnMigrationEndRule",
      "dev\\.matrix\\.roomigrant\\.rules\\.OnMigrationStartRule"
    )
  ),
  KaptMatcher(
    name = "Room",
    processor = "androidx.room:room-compiler",
    annotationImports = listOf(
      "androidx\\.room\\.\\*",
      "androidx\\.room\\.Database"
    )
  ),
  KaptMatcher(
    name = "AutoService",
    processor = "com.google.auto.service:auto-service",
    annotationImports = listOf(
      "com\\.google\\.auto\\.service\\.\\*",
      "com\\.google\\.auto\\.service\\.AutoService"
    )
  ),
  KaptMatcher(
    name = "Gradle Incap Helper",
    processor = "net.ltgt.gradle.incap:incap-processor",
    annotationImports = listOf(
      "net\\.ltgt\\.gradle\\.incap\\.\\*",
      "net\\.ltgt\\.gradle\\.incap\\.IncrementalAnnotationProcessor"
    )
  )
)
