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

@file:Suppress("MatchingDeclarationName")

package modulecheck.core.kapt

import modulecheck.api.KaptMatcher

val defaultKaptMatchers: List<KaptMatcher> = listOf(
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
      "dagger\\.assisted\\.AssistedFactory",
      "com\\.squareup\\.anvil\\.annotations\\.\\*",
      "com\\.squareup\\.anvil\\.annotations\\.ContributesTo",
      "com\\.squareup\\.anvil\\.annotations\\.MergeComponent",
      "com\\.squareup\\.anvil\\.annotations\\.MergeSubomponent"
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
    name = "Inflation Inject (Square)",
    processor = "com.squareup.inject:inflation-inject-processor",
    annotationImports = listOf(
      "com\\.squareup\\.inject\\.inflation\\.\\*",
      "com\\.squareup\\.inject\\.inflation\\.InflationInject",
      "com\\.squareup\\.inject\\.inflation\\.InflationInjectModule"
    )
  ),
  KaptMatcher(
    name = "Inflation Inject (Cash App)",
    processor = "app.cash.inject:inflation-inject-processor",
    annotationImports = listOf(
      "app\\.cash\\.inject\\.inflation\\.\\*",
      "app\\.cash\\.inject\\.inflation\\.InflationInject",
      "app\\.cash\\.inject\\.inflation\\.InflationModule",
      "app\\.cash\\.inject\\.inflation\\.ViewFactory"
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
    name = "Assisted Inject",
    processor = "com.squareup.inject:assisted-inject-processor",
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
    name = "AutoFactory",
    processor = "com.google.auto.factory:auto-factory",
    annotationImports = listOf(
      "com\\.google\\.auto\\.factory\\.\\*",
      "com\\.google\\.auto\\.factory\\.AutoFactory"
    )
  ),
  KaptMatcher(
    name = "Gradle Incap Helper",
    processor = "net.ltgt.gradle.incap:incap-processor",
    annotationImports = listOf(
      "net\\.ltgt\\.gradle\\.incap\\.\\*",
      "net\\.ltgt\\.gradle\\.incap\\.IncrementalAnnotationProcessor"
    )
  ),
  KaptMatcher(
    name = "Epoxy",
    processor = "com.airbnb.android:epoxy-processor",
    annotationImports = listOf(
      "com\\.airbnb\\.epoxy\\.\\*",
      "com\\.airbnb\\.epoxy\\.AfterPropsSet",
      "com\\.airbnb\\.epoxy\\.AutoModel",
      "com\\.airbnb\\.epoxy\\.CallbackProp",
      "com\\.airbnb\\.epoxy\\.EpoxyAttribute",
      "com\\.airbnb\\.epoxy\\.EpoxyDataBindingLayouts",
      "com\\.airbnb\\.epoxy\\.EpoxyDataBindingPattern",
      "com\\.airbnb\\.epoxy\\.EpoxyModelClass",
      "com\\.airbnb\\.epoxy\\.ModelProp",
      "com\\.airbnb\\.epoxy\\.ModelView",
      "com\\.airbnb\\.epoxy\\.OnViewRecycled",
      "com\\.airbnb\\.epoxy\\.OnVisibilityChanged",
      "com\\.airbnb\\.epoxy\\.OnVisibilityStateChanged",
      "com\\.airbnb\\.epoxy\\.PackageEpoxyConfig",
      "com\\.airbnb\\.epoxy\\.PackageModelViewConfig",
      "com\\.airbnb\\.epoxy\\.TextProp",
    )
  )
)
