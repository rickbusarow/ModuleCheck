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

package modulecheck.parsing.gradle.model

/**
 * [org.gradle.api.tasks.SourceSet]
 *
 * @since 0.12.0
 */
typealias GradleSourceSet = org.gradle.api.tasks.SourceSet

/**
 * [org.gradle.api.artifacts.Configuration]
 *
 * @since 0.12.0
 */
typealias GradleConfiguration = org.gradle.api.artifacts.Configuration

/**
 * [org.gradle.api.Project]
 *
 * @since 0.12.0
 */
typealias GradleProject = org.gradle.api.Project

/**
 * [org.gradle.api.artifacts.ProjectDependency]
 *
 * @since 0.12.0
 */
typealias GradleProjectDependency = org.gradle.api.artifacts.ProjectDependency

/** [org.gradle.api.logging.Logger] */
typealias GradleLogger = org.gradle.api.logging.Logger

/** [org.gradle.api.logging.Logging] */
typealias GradleLogging = org.gradle.api.logging.Logging

/** [org.gradle.api.provider.Property] */
typealias GradleProperty<T> = org.gradle.api.provider.Property<T>

/** [org.gradle.api.provider.Provider] */
typealias GradleProvider<T> = org.gradle.api.provider.Provider<T>
