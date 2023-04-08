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

package modulecheck.parsing.android

import groovy.util.Node
import modulecheck.parsing.source.UnqualifiedAndroidResource
import modulecheck.utils.flatMapToSet
import java.io.File

class AndroidResourceParser {

  /**
   * @return returns all **unqualified** resources declared
   *   within this directory, such as `R.string.app_name`
   * @since 0.12.0
   */
  fun parseFile(resDir: File): Set<UnqualifiedAndroidResource> {
    val xmlParser = SafeXmlParser()

    return resDir
      .walkTopDown()
      .filter { it.isFile }
      .filter { it.extension == "xml" }
      .flatMapToSet { file ->

        val fromFile = listOfNotNull(UnqualifiedAndroidResource.fromFile(file))

        val fromContents = xmlParser.parse(file)
          ?.takeIf { it.name() == "resources" }
          ?.children()
          ?.filterIsInstance<Node>()
          ?.mapNotNull { node ->

            val name = node.attributes()["name"]?.toString() ?: return@mapNotNull null

            UnqualifiedAndroidResource.fromValuePair(
              type = node.name().toString(),
              name = name
            )
          }
          .orEmpty()

        fromFile + fromContents
      }
  }
}
