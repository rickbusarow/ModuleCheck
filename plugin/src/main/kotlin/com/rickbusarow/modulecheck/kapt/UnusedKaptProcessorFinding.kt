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

package com.rickbusarow.modulecheck.kapt

import com.rickbusarow.modulecheck.Config
import com.rickbusarow.modulecheck.Position
import com.rickbusarow.modulecheck.positionOf
import org.gradle.api.Project

data class UnusedKaptProcessorFinding(
  override val dependentProject: Project,
  val dependencyPath: String,
  val config: Config
) : UnusedKaptFinding {

  override val dependencyIdentifier = dependencyPath

  override val problemName = "unused ${config.name} dependency"

  override fun position(): Position? {
    val fixedPath = dependencyPath.split(".")
      .drop(1)
      .joinToString(":", ":")

    return dependentProject
      .buildFile
      .readText()
      .lines()
      .positionOf(dependencyPath, config)
      ?: dependentProject
        .buildFile
        .readText()
        .lines()
        .positionOf(fixedPath, config)
  }
}
