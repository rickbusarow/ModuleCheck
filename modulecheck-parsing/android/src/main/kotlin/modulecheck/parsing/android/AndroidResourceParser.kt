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

package modulecheck.parsing.android

import groovy.util.Node
import modulecheck.parsing.source.AndroidResource
import modulecheck.parsing.source.DeclarationName
import modulecheck.parsing.source.asDeclarationName
import modulecheck.utils.flatMapToSet
import java.io.File

class AndroidResourceParser {

  fun parseFile(resDir: File): Set<DeclarationName> {

    val xmlParser = SafeXmlParser()

    return resDir
      .walkTopDown()
      .filter { it.isFile }
      .filter { it.extension == "xml" }
      .flatMap { file ->

        val fromFile = listOfNotNull(AndroidResource.fromFile(file))

        val fromContents = xmlParser.parse(file)
          ?.takeIf { it.name() == "resources" }
          ?.children()
          ?.filterIsInstance<Node>()
          ?.mapNotNull { node ->

            val name = node.attributes()["name"]?.toString() ?: return@mapNotNull null

            AndroidResource.fromValuePair(
              type = node.name().toString(),
              name = name
            )
          }
          .orEmpty()

        fromFile + fromContents
      }
      .flatMapToSet {
        listOf(
          "R.${it.prefix}".asDeclarationName(),
          "R.${it.prefix}.${it.name}".asDeclarationName()
        )
      }
  }
}
