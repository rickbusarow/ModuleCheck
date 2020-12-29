package com.rickbusarow.modulecheck.parser

import com.rickbusarow.modulecheck.Config
import com.rickbusarow.modulecheck.Fixable
import com.rickbusarow.modulecheck.MCP
import org.gradle.api.Project
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

              val comb = dep.group + ":" + dep.name

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

object UnusedKaptParser {

  fun parseLazy(mcp: MCP): Lazy<MCP.ParsedKapt<UnusedKapt>> = lazy {
    parse(mcp)
  }

  fun parse(mcp: MCP): MCP.ParsedKapt<UnusedKapt> {
    val unusedAndroidTest = mcp.kaptDependencies.androidTest.filter { matcher ->
      matcher.annotationImports.none { annotationRegex ->
        mcp.androidTestImports.any { imp ->
          annotationRegex.matches(imp)
        }
      }
    }
      .map { UnusedKapt(mcp.project, it.processor, Config.KaptAndroidTest) }
      .toSet()

    val unusedMain = mcp.kaptDependencies.main.filter { matcher ->
      mcp.mainImports.none { imp ->
        matcher.annotationImports.any { annotationRegex ->
          annotationRegex.matches(imp)
        }
      }
    }
      .map { UnusedKapt(mcp.project, it.processor, Config.Kapt) }
      .toSet()

    val unusedTest = mcp.kaptDependencies.test.filter { matcher ->
      matcher.annotationImports.none { annotationRegex ->
        mcp.testImports.any { imp ->
          annotationRegex.matches(imp)
        }
      }
    }
      .map { UnusedKapt(mcp.project, it.processor, Config.KaptTest) }
      .toSet()

    return MCP.ParsedKapt(unusedAndroidTest, unusedMain, unusedTest)
  }
}

data class UnusedKapt(
  val dependentProject: Project,
  val dependencyPath: String,
  val config: Config
) : Fixable {

  fun position(): MCP.Position {
    return MCP.Position(0, 0)
  }

  fun logString(): String {
    val pos = if (position().row == 0 || position().column == 0) {
      ""
    } else {
      "(${position().row}, ${position().column}): "
    }

    return "${dependentProject.buildFile.path}: $pos$dependencyPath"
  }

  override fun fix() {
    val text = dependentProject.buildFile.readText()

    val row = position().row - 1

    val lines = text.lines().toMutableList()

    if (row > 0) {
      lines[row] = "//" + lines[row]

      val newText = lines.joinToString("\n")

      dependentProject.buildFile.writeText(newText)
    }
  }
}

data class KaptMatcher(
  val name: String,
  val processor: String,
  val annotationImports: List<Regex>
)

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
    name = "Dagger Android",
    processor = "com.google.dagger:dagger-android-processor",
    annotationImports = listOf(
      "javax\\.android\\.\\*",
      "javax\\.android\\.ContributesAndroidInjector"
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
    name = "Assisted Inject", processor = "com.squareup.inject:assisted-inject-processor",
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
  ),
  KaptMatcher(
    name = "AutoService",
    processor = "com.google.auto.service:auto-service",
    annotationImports = listOf(
      "com\\.google\\.auto\\.\\*",
      "com\\.google\\.auto\\.service"
    ).map { it.toRegex() }
  ),
  KaptMatcher(
    name = "Gradle Incap Helper",
    processor = "net.ltgt.gradle.incap:incap-processor",
    annotationImports = listOf(
      "net\\.ltgt\\.gradle\\.incap\\.\\*",
      "net\\.ltgt\\.gradle\\.incap\\.IncrementalAnnotationProcessor"
    ).map { it.toRegex() }
  )
).associateBy { it.processor }
