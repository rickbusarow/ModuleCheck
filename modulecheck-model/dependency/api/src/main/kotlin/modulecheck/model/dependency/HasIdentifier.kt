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

package modulecheck.model.dependency

/** Something associated with a specific [ProjectPath][modulecheck.model.dependency.ProjectPath]. */
interface HasProjectPath : HasIdentifier {
  val projectPath: ProjectPath
  override val identifier: Identifier get() = projectPath
}

interface HasIdentifier {
  val identifier: Identifier
}

interface HasMavenCoordinates : HasIdentifier {
  val mavenCoordinates: MavenCoordinates
  override val identifier: Identifier get() = mavenCoordinates
}

/**
 * common trait interface for maven coordinates
 *
 * ```
 * org.junit.jupiter:junit-jupiter-engine:5.0.0
 * └───────┬───────┘ └────────┬─────────┘ └─┬─┘
 *         │                  │             └ version
 *         │                  └ moduleName
 *         └ group
 * ```
 */
interface HasMavenCoordinatesElements : HasGroup, HasModuleName, HasVersion

/** Convenience trait interface for [group] */
interface HasGroup {
  /** In `org.junit.jupiter:junit-jupiter-engine:5.0.0`, this is `org.junit.jupiter:__:__`. */
  val group: String?
}

/** Convenience trait interface for [moduleName] */
interface HasModuleName {
  /** In `org.junit.jupiter:junit-jupiter-engine:5.0.0`, this is `__:junit-jupiter-engine:__`. */
  val moduleName: String
}

/** Convenience trait interface for [version] */
interface HasVersion {
  /** In `org.junit.jupiter:junit-jupiter-engine:5.0.0`, this is `__:__:5.0.0`. */
  val version: String?
}
