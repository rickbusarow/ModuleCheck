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

package modulecheck.builds.artifacts

import com.squareup.moshi.JsonClass
import java.io.Serializable

/**
 * Models the module-specific properties of published maven artifacts.
 *
 * see (Niklas Baudy's
 * [gradle-maven-publish-plugin](https://github.com/vanniktech/gradle-maven-publish-plugin))
 *
 * @param gradlePath the path of the Gradle project, such as `:workflow-core`
 * @param group The maven "group", which should always be `com.rickbusarow.modulecheck`.
 * @param artifactId The maven "module", such as `workflow-core-jvm`.
 * @param description The description of this specific artifact, such as "Workflow Core".
 * @param packaging `aar` or `jar`.
 * @param javaVersion the java version of the artifact (typically 8 or 11). If not set
 *   explicitly, this defaults to the JDK version used to build the artifact.
 * @since 0.13.0
 */
@JsonClass(generateAdapter = true)
data class ArtifactConfig(
  val gradlePath: String,
  val group: String,
  val artifactId: String,
  val description: String,
  val packaging: String,
  val javaVersion: String
) : Serializable
