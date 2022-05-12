/*
 * Copyright (C) 2021-2022 Rick Busarow
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

package modulecheck.core.kapt

import modulecheck.config.CodeGeneratorBinding
import modulecheck.parsing.gradle.model.PluginDefinition

fun defaultCodeGeneratorBindings(): List<CodeGeneratorBinding> = listOf(
  CodeGeneratorBinding.KotlinCompilerPlugin(
    name = "Anvil",
    generatorMavenCoordinates = "com.squareup.anvil:compiler",
    annotationNames = listOf(
      "com.squareup.anvil.annotations.ContributesBinding",
      "com.squareup.anvil.annotations.ContributesMultibinding",
      "com.squareup.anvil.annotations.ContributesSubcomponent",
      "com.squareup.anvil.annotations.ContributesTo",
      "com.squareup.anvil.annotations.MergeComponent",
      "com.squareup.anvil.annotations.MergeSubcomponent",
      "com.squareup.anvil.annotations.compat.MergeInterfaces",
      "com.squareup.anvil.annotations.compat.MergeModules",
      "dagger.Binds",
      "dagger.BindsInstance",
      "dagger.Component",
      "dagger.Module",
      "dagger.assisted.Assisted",
      "dagger.assisted.AssistedFactory",
      "dagger.assisted.AssistedInject",
      "dagger.multibindings.IntoMap",
      "dagger.multibindings.IntoSet",
      "javax.inject.Inject"
    ),
    gradlePlugin = PluginDefinition(
      name = "Anvil",
      qualifiedId = "com.squareup.anvil",
      legacyIdOrNull = null,
      precompiledAccessorOrNull = null,
      kotlinFunctionArgumentOrNull = null
    )
  ),
  CodeGeneratorBinding.KotlinCompilerPlugin(
    name = "Kotlin Parcelize",
    generatorMavenCoordinates = "com.squareup.anvil:compiler",
    annotationNames = listOf(
      "kotlinx.parcelize.Parcelize"
    ),
    gradlePlugin = PluginDefinition(
      name = "Kotlin Parcelize",
      qualifiedId = "org.jetbrains.kotlin.plugin.parcelize",
      legacyIdOrNull = "kotlin-parcelize",
      precompiledAccessorOrNull = null,
      kotlinFunctionArgumentOrNull = "parcelize"
    )
  ),
  CodeGeneratorBinding.AnvilExtension(
    name = "Tangle Core",
    generatorMavenCoordinates = "com.rickbusarow.tangle:tangle-compiler",
    annotationNames = listOf(
      "tangle.inject.TangleParam",
      "tangle.inject.TangleScope"
    )
  ),
  CodeGeneratorBinding.AnvilExtension(
    name = "Tangle Fragments",
    generatorMavenCoordinates = "com.rickbusarow.tangle:tangle-fragment-compiler",
    annotationNames = listOf(
      "tangle.fragment.ContributesFragment",
      "tangle.fragment.FragmentInject",
      "tangle.fragment.FragmentInjectFactory",
      "tangle.fragment.FragmentKey",
      "tangle.fragment.TangleFragmentProviderMap"
    )
  ),
  CodeGeneratorBinding.AnvilExtension(
    name = "Tangle ViewModels",
    generatorMavenCoordinates = "com.rickbusarow.tangle:tangle-viewmodel-compiler",
    annotationNames = listOf(
      "tangle.viewmodel.TangleViewModelProviderMap",
      "tangle.viewmodel.VMInject"
    )
  ),
  CodeGeneratorBinding.AnvilExtension(
    name = "Tangle Work",
    generatorMavenCoordinates = "com.rickbusarow.tangle:tangle-work-compiler",
    annotationNames = listOf(
      "tangle.work.TangleAssistedWorkerFactoryMap",
      "tangle.work.TangleWorker"
    )
  ),
  CodeGeneratorBinding.AnnotationProcessor(
    name = "Dagger Hilt",
    generatorMavenCoordinates = "com.google.dagger:hilt-compiler",
    annotationNames = listOf(
      "dagger.hilt.DefineComponent",
      "dagger.hilt.EntryPoint",
      "dagger.hilt.InstallIn"
    )
  ),
  CodeGeneratorBinding.AnnotationProcessor(
    name = "Dagger Hilt Android",
    generatorMavenCoordinates = "com.google.dagger:hilt-android-compiler",
    annotationNames = listOf(
      "dagger.hilt.DefineComponent",
      "dagger.hilt.EntryPoint",
      "dagger.hilt.InstallIn",
      "dagger.hilt.android.AndroidEntryPoint",
      "dagger.hilt.android.HiltAndroidApp",
      "dagger.hilt.android.WithFragmentBindings"
    )
  ),
  CodeGeneratorBinding.AnnotationProcessor(
    name = "Moshi Kotlin codegen (kapt)",
    generatorMavenCoordinates = "com.squareup.moshi:moshi-kotlin-codegen",
    annotationNames = listOf(
      "com.squareup.moshi.Json",
      "com.squareup.moshi.JsonClass"
    )
  ),
  CodeGeneratorBinding.KspExtension(
    name = "Moshi Kotlin codegen (KSP)",
    generatorMavenCoordinates = "com.squareup.moshi:moshi-kotlin-codegen",
    annotationNames = listOf(
      "com.squareup.moshi.Json",
      "com.squareup.moshi.JsonClass"
    )
  ),
  CodeGeneratorBinding.AnnotationProcessor(
    name = "Dagger",
    generatorMavenCoordinates = "com.google.dagger:dagger-compiler",
    annotationNames = listOf(
      "javax.inject.Inject",
      "dagger.Binds",
      "dagger.Module",
      "dagger.multibindings.IntoMap",
      "dagger.multibindings.IntoSet",
      "dagger.BindsInstance",
      "dagger.Component",
      "dagger.assisted.Assisted",
      "dagger.assisted.AssistedInject",
      "dagger.assisted.AssistedFactory",
      "com.squareup.anvil.annotations.ContributesTo",
      "com.squareup.anvil.annotations.MergeComponent",
      "com.squareup.anvil.annotations.MergeSubomponent"
    )
  ),
  CodeGeneratorBinding.AnnotationProcessor(
    name = "Dagger Android",
    generatorMavenCoordinates = "com.google.dagger:dagger-android-processor",
    annotationNames = listOf(
      "dagger.android.ContributesAndroidInjector"
    )
  ),
  CodeGeneratorBinding.AnnotationProcessor(
    name = "Inflation Inject (Square)",
    generatorMavenCoordinates = "com.squareup.inject:inflation-inject-processor",
    annotationNames = listOf(
      "com.squareup.inject.inflation.InflationInject",
      "com.squareup.inject.inflation.InflationInjectModule"
    )
  ),
  CodeGeneratorBinding.AnnotationProcessor(
    name = "Inflation Inject (Cash App)",
    generatorMavenCoordinates = "app.cash.inject:inflation-inject-processor",
    annotationNames = listOf(
      "app.cash.inject.inflation.InflationInject",
      "app.cash.inject.inflation.InflationModule",
      "app.cash.inject.inflation.ViewFactory"
    )
  ),
  CodeGeneratorBinding.AnnotationProcessor(
    name = "Assisted Inject Dagger (Legacy Square library)",
    generatorMavenCoordinates = "com.squareup.inject:assisted-inject-processor-dagger2",
    annotationNames = listOf(
      "com.squareup.inject.assisted.dagger2.AssistedModule"
    )
  ),
  CodeGeneratorBinding.AnnotationProcessor(
    name = "Assisted Inject (Legacy Square library)",
    generatorMavenCoordinates = "com.squareup.inject:assisted-inject-processor",
    annotationNames = listOf(
      "com.squareup.inject.assisted.AssistedInject",
      "com.squareup.inject.assisted.AssistedInject.Factory",
      "com.squareup.inject.assisted.Assisted"
    )
  ),
  CodeGeneratorBinding.AnnotationProcessor(
    name = "Roomigrant",
    generatorMavenCoordinates = "com.github.MatrixDev.Roomigrant:RoomigrantCompiler",
    annotationNames = listOf(
      "dev.matrix.roomigrant.GenerateRoomMigrations",
      "dev.matrix.roomigrant.rules.FieldMigrationRule",
      "dev.matrix.roomigrant.rules.OnMigrationEndRule",
      "dev.matrix.roomigrant.rules.OnMigrationStartRule"
    )
  ),
  CodeGeneratorBinding.AnnotationProcessor(
    name = "Room (annotation processor)",
    generatorMavenCoordinates = "androidx.room:room-compiler",
    annotationNames = listOf(
      "androidx.room.Database"
    )
  ),
  CodeGeneratorBinding.KspExtension(
    name = "Room (KSP)",
    generatorMavenCoordinates = "androidx.room:room-compiler",
    annotationNames = listOf(
      "androidx.room.Database"
    )
  ),
  CodeGeneratorBinding.AnnotationProcessor(
    name = "AutoService (annotation processor)",
    generatorMavenCoordinates = "com.google.auto.service:auto-service",
    annotationNames = listOf(
      "com.google.auto.service.AutoService"
    )
  ),
  CodeGeneratorBinding.KspExtension(
    name = "AutoService (Zac Sweers KSP)",
    generatorMavenCoordinates = "dev.zacsweers.autoservice:compiler",
    annotationNames = listOf(
      "com.google.auto.service.AutoService"
    )
  ),
  CodeGeneratorBinding.AnnotationProcessor(
    name = "AutoFactory",
    generatorMavenCoordinates = "com.google.auto.factory:auto-factory",
    annotationNames = listOf(
      "com.google.auto.factory.AutoFactory"
    )
  ),
  CodeGeneratorBinding.AnnotationProcessor(
    name = "Gradle Incap Helper",
    generatorMavenCoordinates = "net.ltgt.gradle.incap:incap-processor",
    annotationNames = listOf(
      "net.ltgt.gradle.incap.IncrementalAnnotationProcessor"
    )
  ),
  CodeGeneratorBinding.AnnotationProcessor(
    name = "Epoxy",
    generatorMavenCoordinates = "com.airbnb.android:epoxy-processor",
    annotationNames = listOf(
      "com.airbnb.epoxy.AfterPropsSet",
      "com.airbnb.epoxy.AutoModel",
      "com.airbnb.epoxy.CallbackProp",
      "com.airbnb.epoxy.EpoxyAttribute",
      "com.airbnb.epoxy.EpoxyDataBindingLayouts",
      "com.airbnb.epoxy.EpoxyDataBindingPattern",
      "com.airbnb.epoxy.EpoxyModelClass",
      "com.airbnb.epoxy.ModelProp",
      "com.airbnb.epoxy.ModelView",
      "com.airbnb.epoxy.OnViewRecycled",
      "com.airbnb.epoxy.OnVisibilityChanged",
      "com.airbnb.epoxy.OnVisibilityStateChanged",
      "com.airbnb.epoxy.PackageEpoxyConfig",
      "com.airbnb.epoxy.PackageModelViewConfig",
      "com.airbnb.epoxy.TextProp"
    )
  )
)
