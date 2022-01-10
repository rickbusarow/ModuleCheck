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

package modulecheck.parsing.wiring

import com.squareup.anvil.annotations.ContributesBinding
import com.squareup.anvil.annotations.ContributesTo
import modulecheck.dagger.AppScope
import modulecheck.parsing.gradle.PluginsBlock
import modulecheck.parsing.gradle.PluginsBlockProvider
import modulecheck.parsing.groovy.antlr.GroovyPluginsBlockParser
import modulecheck.parsing.psi.KotlinPluginsBlockParser
import modulecheck.parsing.psi.internal.asKtFile
import org.jetbrains.kotlin.incremental.isKotlinFile
import java.io.File
import javax.inject.Inject
import javax.inject.Provider

class RealPluginsBlockProvider(
  private val groovyParser: GroovyPluginsBlockParser,
  private val kotlinParser: KotlinPluginsBlockParser,
  private val buildFile: File
) : PluginsBlockProvider {

  override fun get(): PluginsBlock? {
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
    private val groovyParserProvider: Provider<GroovyPluginsBlockParser>,
    private val kotlinParserProvider: Provider<KotlinPluginsBlockParser>
  ) : PluginsBlockProvider.Factory {
    override fun create(buildFile: File): PluginsBlockProvider {
      return RealPluginsBlockProvider(
        groovyParser = groovyParserProvider.get(),
        kotlinParser = kotlinParserProvider.get(),
        buildFile = buildFile
      )
    }
  }
}
