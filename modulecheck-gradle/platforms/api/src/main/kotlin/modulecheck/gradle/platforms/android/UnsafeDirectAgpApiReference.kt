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
typealias AgpAppExtension = com.android.build.gradle.AppExtension

/** [com.android.build.gradle.BaseExtension] */
@UnsafeDirectAgpApiReference
typealias AgpBaseExtension = com.android.build.gradle.BaseExtension

/** [com.android.build.gradle.BasePlugin] */
@UnsafeDirectAgpApiReference
typealias AgpBasePlugin = com.android.build.gradle.BasePlugin

/** [com.android.build.gradle.api.AndroidBasePlugin] */
@UnsafeDirectAgpApiReference
typealias AgpApiBasePlugin = com.android.build.gradle.api.AndroidBasePlugin

/** [com.android.build.api.dsl.CommonExtension<*,] */
@UnsafeDirectAgpApiReference
typealias AgpCommonExtension = com.android.build.api.dsl.CommonExtension<*, *, *, *>

/** [com.android.build.gradle.LibraryExtension] */
@UnsafeDirectAgpApiReference
typealias AgpLibraryExtension = com.android.build.gradle.LibraryExtension

/** [com.android.build.api.variant.AndroidComponentsExtension] */
@UnsafeDirectAgpApiReference
typealias AgpComponentsExtension = com.android.build.api.variant.AndroidComponentsExtension<*, *, *>

/** [com.android.build.gradle.TestExtension] */
@UnsafeDirectAgpApiReference
typealias AgpTestExtension = com.android.build.gradle.TestExtension

/** [com.android.build.gradle.TestedExtension] */
@UnsafeDirectAgpApiReference
typealias AgpTestedExtension = com.android.build.gradle.TestedExtension

/** [com.android.build.api.dsl.AndroidSourceSet] */
@UnsafeDirectAgpApiReference
typealias AgpSourceSet = com.android.build.api.dsl.AndroidSourceSet

/** [com.android.build.gradle.api.BaseVariant] */
@Suppress("DEPRECATION")
@UnsafeDirectAgpApiReference
typealias AgpBaseVariant = com.android.build.gradle.api.BaseVariant

/** [com.android.build.gradle.internal.api.TestedVariant] */
@Suppress("DEPRECATION")
@UnsafeDirectAgpApiReference
typealias AgpTestedVariant = com.android.build.gradle.internal.api.TestedVariant

/** [com.android.build.gradle.api.TestVariant] */
@Suppress("DEPRECATION")
@UnsafeDirectAgpApiReference
typealias AgpTestVariant = com.android.build.gradle.api.TestVariant

/** [com.android.build.gradle.api.UnitTestVariant] */
@Suppress("DEPRECATION")
@UnsafeDirectAgpApiReference
typealias AgpUnitTestVariant = com.android.build.gradle.api.UnitTestVariant

/** [com.android.build.gradle.internal.api.DefaultAndroidSourceDirectorySet] */
@UnsafeDirectAgpApiReference
typealias AgpDefaultAndroidSourceDirectorySet =
  com.android.build.gradle.internal.api.DefaultAndroidSourceDirectorySet

/** [com.android.build.gradle.internal.res.GenerateLibraryRFileTask] */
@UnsafeDirectAgpApiReference
typealias AgpGenerateLibraryRFileTask = com.android.build.gradle.internal.res.GenerateLibraryRFileTask

/** [com.android.build.gradle.internal.res.LinkApplicationAndroidResourcesTask] */
@UnsafeDirectAgpApiReference
typealias AgpLinkApplicationAndroidResourcesTask =
  com.android.build.gradle.internal.res.LinkApplicationAndroidResourcesTask

/** [com.android.build.gradle.internal.tasks.VariantAwareTask] */
@UnsafeDirectAgpApiReference
typealias AgpVariantAwareTask = com.android.build.gradle.internal.tasks.VariantAwareTask

/** [com.android.build.gradle.tasks.GenerateBuildConfig] */
@UnsafeDirectAgpApiReference
typealias AgpGenerateBuildConfig = com.android.build.gradle.tasks.GenerateBuildConfig

/** [com.android.build.gradle.tasks.ManifestProcessorTask] */
@UnsafeDirectAgpApiReference
typealias AgpManifestProcessorTask = com.android.build.gradle.tasks.ManifestProcessorTask
