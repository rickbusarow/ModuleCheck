/*
 * Copyright (C) 2021-2023 Rick Busarow
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

package modulecheck.gradle.platforms.android

/**
 * Any reference to AGP code is annotated with this opt-in
 * requirement. AGP code can only be referenced if AGP is in the target
 * project's *build* classpath, so it needs to be handled carefully.
 *
 * Using [AgpApiAccess.ifSafeOrNull] is the easiest and
 * safest way to ensure there are no runtime exceptions.
 *
 * @since 0.12.0
 */
@Target(
  AnnotationTarget.TYPEALIAS,
  AnnotationTarget.VALUE_PARAMETER,
  AnnotationTarget.FUNCTION,
  AnnotationTarget.CLASS
)
@RequiresOptIn(
  message = "This reference will probably cause a runtime exception " +
    "if the Android Gradle Plugin is not in the classpath.  " +
    "Wrap this reference in `AgpApiAccess.ifSafeOrNull { ... } to be safe.",
  level = RequiresOptIn.Level.ERROR
)
annotation class UnsafeDirectAgpApiReference

/** [com.android.build.gradle.AppExtension] */
@UnsafeDirectAgpApiReference
typealias AndroidAppExtension = com.android.build.gradle.AppExtension

/** [com.android.build.gradle.BaseExtension] */
@UnsafeDirectAgpApiReference
typealias AndroidBaseExtension = com.android.build.gradle.BaseExtension

/** [com.android.build.gradle.BasePlugin] */
@UnsafeDirectAgpApiReference
typealias AndroidBasePlugin = com.android.build.gradle.BasePlugin

/** [com.android.build.gradle.api.AndroidBasePlugin] */
@UnsafeDirectAgpApiReference
typealias AndroidApiBasePlugin = com.android.build.gradle.api.AndroidBasePlugin

/** [com.android.build.api.dsl.CommonExtension<*,] */
@UnsafeDirectAgpApiReference
typealias AndroidCommonExtension = com.android.build.api.dsl.CommonExtension<*, *, *, *>

/** [com.android.build.gradle.LibraryExtension] */
@UnsafeDirectAgpApiReference
typealias AndroidLibraryExtension = com.android.build.gradle.LibraryExtension

/** [com.android.build.api.variant.AndroidComponentsExtension] */
@UnsafeDirectAgpApiReference
typealias AndroidComponentsExtension = com.android.build.api.variant.AndroidComponentsExtension<*, *, *>

/** [com.android.build.gradle.TestExtension] */
@UnsafeDirectAgpApiReference
typealias AndroidTestExtension = com.android.build.gradle.TestExtension

/** [com.android.build.gradle.TestedExtension] */
@UnsafeDirectAgpApiReference
typealias AndroidTestedExtension = com.android.build.gradle.TestedExtension

/** [com.android.build.api.dsl.AndroidSourceSet] */
@UnsafeDirectAgpApiReference
typealias AndroidSourceSet = com.android.build.api.dsl.AndroidSourceSet

/** [com.android.build.gradle.api.BaseVariant] */
@Suppress("DEPRECATION")
@UnsafeDirectAgpApiReference
typealias AndroidBaseVariant = com.android.build.gradle.api.BaseVariant

/** [com.android.build.gradle.internal.api.TestedVariant] */
@Suppress("DEPRECATION")
@UnsafeDirectAgpApiReference
typealias AndroidTestedVariant = com.android.build.gradle.internal.api.TestedVariant

/** [com.android.build.gradle.api.TestVariant] */
@Suppress("DEPRECATION")
@UnsafeDirectAgpApiReference
typealias AndroidTestVariant = com.android.build.gradle.api.TestVariant

/** [com.android.build.gradle.api.UnitTestVariant] */
@Suppress("DEPRECATION")
@UnsafeDirectAgpApiReference
typealias AndroidUnitTestVariant = com.android.build.gradle.api.UnitTestVariant

/** [com.android.build.gradle.internal.api.DefaultAndroidSourceDirectorySet] */
@UnsafeDirectAgpApiReference
typealias AndroidDefaultAndroidSourceDirectorySet =
  com.android.build.gradle.internal.api.DefaultAndroidSourceDirectorySet

/** [com.android.build.gradle.internal.res.GenerateLibraryRFileTask] */
@UnsafeDirectAgpApiReference
typealias AndroidGenerateLibraryRFileTask = com.android.build.gradle.internal.res.GenerateLibraryRFileTask

/** [com.android.build.gradle.internal.res.LinkApplicationAndroidResourcesTask] */
@UnsafeDirectAgpApiReference
typealias AndroidLinkApplicationAndroidResourcesTask =
  com.android.build.gradle.internal.res.LinkApplicationAndroidResourcesTask

/** [com.android.build.gradle.internal.tasks.VariantAwareTask] */
@UnsafeDirectAgpApiReference
typealias AndroidVariantAwareTask = com.android.build.gradle.internal.tasks.VariantAwareTask

/** [com.android.build.gradle.tasks.GenerateBuildConfig] */
@UnsafeDirectAgpApiReference
typealias AndroidGenerateBuildConfig = com.android.build.gradle.tasks.GenerateBuildConfig

/** [com.android.build.gradle.tasks.ManifestProcessorTask] */
@UnsafeDirectAgpApiReference
typealias AndroidManifestProcessorTask = com.android.build.gradle.tasks.ManifestProcessorTask
