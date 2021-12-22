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

package modulecheck.parsing.wiring

import com.squareup.anvil.annotations.ContributesBinding
import modulecheck.dagger.AppScope
import modulecheck.parsing.gradle.DependenciesBlock
import modulecheck.parsing.gradle.DependenciesBlocksProvider
import modulecheck.parsing.groovy.antlr.GroovyDependencyBlockParser
import modulecheck.parsing.psi.KotlinDependencyBlockParser
import modulecheck.parsing.psi.internal.asKtFile
import org.jetbrains.kotlin.incremental.isKotlinFile
import java.io.File
import javax.inject.Inject
import javax.inject.Provider

class RealDependenciesBlocksProvider(
  private val groovyParser: GroovyDependencyBlockParser,
  private val kotlinParser: KotlinDependencyBlockParser,
  private val buildFile: File
) : DependenciesBlocksProvider {

  override fun get(): List<DependenciesBlock> {
    return when {
      buildFile.isKotlinFile(listOf("kts")) -> kotlinParser.parse(buildFile.asKtFile())
      buildFile.extension == "gradle" -> groovyParser.parse(buildFile.readText())
      else -> throw IllegalArgumentException(
        "The file argument must be either a `*.gradle.kts` file or `*.gradle`.  " +
          "The supplied argument was `${buildFile.name}`"
      )
    }
  }

  @ContributesBinding(AppScope::class)
  class Factory @Inject constructor(
    private val groovyParserProvider: Provider<GroovyDependencyBlockParser>,
    private val kotlinParserProvider: Provider<KotlinDependencyBlockParser>,
  ) : DependenciesBlocksProvider.Factory {
    override fun create(buildFile: File): RealDependenciesBlocksProvider {
      return RealDependenciesBlocksProvider(
        groovyParser = groovyParserProvider.get(),
        kotlinParser = kotlinParserProvider.get(),
        buildFile = buildFile
      )
    }
  }
}
