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

package modulecheck.gradle.task

import com.squareup.anvil.annotations.ContributesBinding
import modulecheck.dagger.AppScope
import modulecheck.gradle.platforms.ConfigurationFileResolver
import org.gradle.api.artifacts.Configuration
import org.gradle.api.artifacts.ExternalModuleDependency
import org.gradle.api.file.FileCollection
import org.gradle.api.provider.Property
import org.gradle.workers.WorkAction
import org.gradle.workers.WorkParameters
import org.gradle.workers.WorkerExecutor
import java.io.File
import javax.inject.Inject

@ContributesBinding(AppScope::class)
class RealConfigurationFileResolver @Inject constructor(
  private val workerExecutor: WorkerExecutor
) : ConfigurationFileResolver {

  override fun resolve(configurations: List<Configuration>): List<File> {

    val queue = workerExecutor.noIsolation()

    configurations.forEach { config ->
      queue.submit(DispatchAction::class.java) { params ->
        params.configuration.set(
          config.fileCollection { dep ->
            dep is ExternalModuleDependency
          }
        )
      }
    }
    queue.await()

    return configurations
      .flatMap { config ->
        config
          .files { dep -> dep is ExternalModuleDependency }
          .filter { it.exists() }
      }
  }
}

abstract class DispatchAction : WorkAction<DispatchParams> {
  override fun execute() {
    val config = parameters.configuration.get()
    // if (config.isCanBeResolved) {
    //   config.resolve()
    // }

    config
      .filter { it.exists() }
      .files
  }
}

interface DispatchParams : WorkParameters {
  val configuration: Property<FileCollection>
  // val action: Property<Runnable>
}
