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
import groovy.util.NodeList
import modulecheck.parsing.source.McName.CompatibleLanguage.XML
import modulecheck.parsing.source.UnqualifiedAndroidResource
import modulecheck.parsing.source.UnqualifiedAndroidResourceReferenceName
import modulecheck.utils.flatMapToSet
import java.io.File

class AndroidStylesParser {

  fun parseFile(file: File): Set<UnqualifiedAndroidResourceReferenceName> {

    val xmlParser = SafeXmlParser()

    return xmlParser.parse(file)
      ?.takeIf { it.name() == "resources" }
      ?.children()
      ?.filterIsInstance<Node>()
      ?.filter { it.name() == "style" }
      ?.map { parseNode(it) }
      .orEmpty()
      .flatMapToSet { it }
  }

  private fun parseNode(styleNode: Node): Set<UnqualifiedAndroidResourceReferenceName> {

    // We don't actually need this attribute, but if it isn't there, this isn't a valid style.
    styleNode.attribute("name")?.toString() ?: return emptySet()

    val parentOrNull = styleNode.attribute("parent")
      ?.toString()
      ?.let { parentName ->

        val declaration = when {
          // ex: `parent="@style/Theme.AppCompat.Light.DarkActionBar"`
          // ex: `parent="@android:style/Theme.AppCompat.Light.DarkActionBar"`
          parentName.startsWith("@") -> {
            UnqualifiedAndroidResource.fromXmlString(parentName)
          }

          // ex: `parent="Theme.AppCompat.Light.DarkActionBar"`
          // ex: `parent="android:Theme.AppCompat.Light.DarkActionBar"`
          else -> {
            UnqualifiedAndroidResource.fromValuePair(
              "style",
              parentName.removePrefix("android:")
            )
          }
        }

        declaration?.let { UnqualifiedAndroidResourceReferenceName(it.name, XML) }
      }

    return styleNode.children()
      .asSequence()
      .filterIsInstance<Node>()
      .map { it.value() }
      .filterIsInstance<NodeList>()
      .mapNotNull { valueNodeList ->
        UnqualifiedAndroidResource.fromXmlString(valueNodeList.text())
          ?.let { UnqualifiedAndroidResourceReferenceName(it.name, XML) }
      }
      .plus(listOfNotNull(parentOrNull))
      .toSet()
  }
}
