package com.rickbusarow.modulecheck.parser

import com.rickbusarow.modulecheck.MCP
import org.gradle.api.artifacts.Dependency

object KaptParser {

  fun parseLazy(mcp: MCP): Lazy<MCP.ParsedKapt<KaptMatcher>> = lazy {
    parse(mcp)
  }

  fun parse(mcp: MCP): MCP.ParsedKapt<KaptMatcher> {
    val grouped = mcp.project
      .configurations
      .groupBy { it.name }
      .mapValues { (_, configurations) ->
        configurations.flatMap { config ->
          config
            .dependencies
            .map { dep: Dependency ->

              val comb = dep.name + ":" + dep.version

              kaptMatchers.getOrElse(comb) { KaptMatcher("???", comb, listOf()) }
            }
        }.toSet()
      }

    return MCP.ParsedKapt(
      grouped.getOrDefault("kaptAndroidTest", setOf()),
      grouped.getOrDefault("kapt", setOf()),
      grouped.getOrDefault("kaptTest", setOf())
    )
  }
}

data class KaptMatcher(val name: String, val processor: String, val annotationImports: List<Regex>)

internal val kaptMatchers: Map<String, KaptMatcher> = listOf(
  KaptMatcher(
    name = "Dagger Hilt",
    processor = "com.google.dagger:hilt-compiler",
    annotationImports = listOf(
      "dagger\\.hilt\\.\\*",
      "dagger\\.hilt\\.DefineComponent",
      "dagger\\.hilt\\.EntryPoint",
      "dagger\\.hilt\\.InstallIn",
      "dagger\\.hilt\\.android.AndroidEntryPoint",
      "dagger\\.hilt\\.android.HiltAndroidApp",
      "dagger\\.hilt\\.android.WithFragmentBindings"
    ).map { it.toRegex() }
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
    ).map { it.toRegex() }
  ),
  KaptMatcher(
    name = "Moshi Kotlin codegen",
    processor = "com.squareup.moshi:moshi-kotlin-codegen",
    annotationImports = listOf(
      "com\\.squareup\\.moshi\\.\\*",
      "com\\.squareup\\.moshi\\.Json",
      "com\\.squareup\\.moshi\\.JsonClass"
    ).map { it.toRegex() }
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
      "dagger\\.Component"
    ).map { it.toRegex() }
  ),
  KaptMatcher(
    name = "Inflation Inject", processor = "com.squareup.inject:inflation-inject-processor",
    annotationImports = listOf(
      "com\\.squareup\\.inject\\.inflation\\.\\*",
      "com\\.squareup\\.inject\\.inflation\\.InflationInject",
      "com\\.squareup\\.inject\\.inflation\\.InflationInjectModule"
    ).map { it.toRegex() }
  ),
  KaptMatcher(
    name = "Assisted Inject Dagger",
    processor = "com.squareup.inject:assisted-inject-processor-dagger2",
    annotationImports = listOf(
      "com\\.squareup\\.inject\\.assisted\\.dagger2\\.\\*",
      "com\\.squareup\\.inject\\.assisted\\.dagger2\\.AssistedModule"
    ).map { it.toRegex() }
  ),
  KaptMatcher(
    name = "Assisted Inject", processor = "com.squareup.inject:assisted-inject-processor-dagger2",
    annotationImports = listOf(
      "com\\.squareup\\.inject\\.assisted\\.\\*",
      "com\\.squareup\\.inject\\.assisted\\.AssistedInject",
      "com\\.squareup\\.inject\\.assisted\\.AssistedInject.Factory",
      "com\\.squareup\\.inject\\.assisted\\.Assisted"
    ).map { it.toRegex() }
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
    ).map { it.toRegex() }
  ),
  KaptMatcher(
    name = "Room",
    processor = "androidx.room:room-compiler",
    annotationImports = listOf(
      "androidx\\.room\\.\\*",
      "androidx\\.room\\.Database"
    ).map { it.toRegex() }
  )
).associateBy { it.processor }
