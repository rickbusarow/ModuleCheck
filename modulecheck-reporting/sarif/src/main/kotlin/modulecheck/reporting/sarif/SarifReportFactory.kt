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

package modulecheck.reporting.sarif

import com.squareup.moshi.Moshi
import com.squareup.moshi.adapter
import modulecheck.dagger.ModuleCheckVersionProvider
import modulecheck.dagger.SourceWebsiteUrlProvider
import modulecheck.finding.Finding
import modulecheck.project.ProjectRoot
import modulecheck.reporting.sarif.Level.Warning
import modulecheck.rule.ModuleCheckRule
import modulecheck.utils.suffixIfNot
import java.io.File
import javax.inject.Inject
import kotlin.math.max
import kotlin.math.min

class SarifReportFactory @Inject constructor(
  private val websiteUrl: SourceWebsiteUrlProvider,
  private val moduleCheckVersion: ModuleCheckVersionProvider,
  private val projectRoot: ProjectRoot
) {

  fun create(
    findingResults: List<Finding.FindingResult>,
    rules: List<ModuleCheckRule<*>>
  ): String {

    val sarifRules = rules.map { rule ->
      rule.toSarifRule()
    }
      .sortedBy { it.name }

    val driver = SarifDriver(
      name = "ModuleCheck",
      fullName = "ModuleCheck",
      version = moduleCheckVersion.get(),
      semanticVersion = moduleCheckVersion.get(),
      informationURI = websiteUrl.get().suffixIfNot("/"),
      rules = sarifRules
    )

    val results = findingResults.map { findingResult ->
      SarifResult(
        ruleID = "modulecheck.${findingResult.findingName.id}",
        level = Level.Warning,
        message = Message(text = findingResult.message),
        locations = listOf(
          Location(
            physicalLocation = PhysicalLocation(
              artifactLocation = ArtifactLocation(
                uri = findingResult.buildFile.relativeTo(projectRoot.get()).path,
                uriBaseID = projectRoot.get().absolutePath.suffixIfNot(File.separator)
              ),
              // Extra area to be included in a code snippet, to show context.
              contextRegion = findingResult.positionOrNull
                ?.let { position ->
                  @Suppress("MagicNumber")
                  Region(
                    // these values are 1-indexed, instead of zero-indexed
                    // Add up to 3 lines of context on either side where possible
                    startLine = max(position.row - 3, 1),
                    endLine = min(
                      position.row + 3,
                      findingResult.buildFile.readText().lines().size
                    )
                  )
                },
              region = Region(
                startLine = findingResult.positionOrNull?.row,
                startColumn = findingResult.positionOrNull?.column
              )
            )
          )
        )
      )
    }

    val tool = Tool(driver = driver)

    val run = Run(
      tool = tool,
      results = results
    )

    val sarifReport = SarifReport(
      schema = "https://schemastore.azurewebsites.net/schemas/json/sarif-2.1.0.json",
      version = Version.The210,
      runs = listOf(run)
    )

    val moshi = Moshi.Builder().build()

    return moshi.adapter<SarifReport>().indent("  ").toJson(sarifReport)
  }

  private fun ModuleCheckRule<*>.toSarifRule() = SarifRule(
    id = "modulecheck.${name.id}",
    name = name.titleCase,
    shortDescription = MultiformatMessageString(text = description),
    fullDescription = MultiformatMessageString(text = description),
    defaultConfiguration = ReportingConfiguration(level = Warning),
    helpURI = documentationUrl
  )
}
