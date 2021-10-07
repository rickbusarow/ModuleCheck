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

package modulecheck.parsing.xml

import groovy.util.Node
import groovy.xml.XmlParser
import java.io.File

object AndroidManifestParser {
  private val parser = XmlParser()

  fun parse(file: File) = parser.parse(file)
    .breadthFirst()
    .filterIsInstance<Node>()
    .mapNotNull { it.attributes() }
    .flatMap { it.entries }
    .filterNotNull()
    // .flatMap { it.values.mapNotNull { value -> value } }
    .filterIsInstance<MutableMap.MutableEntry<String, String>>()
    .map { it.key to it.value }
    .toMap()
}
