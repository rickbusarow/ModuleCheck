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

@file:Suppress("ForbiddenImport")

package modulecheck.parsing.android

import groovy.util.Node
import java.io.File
import groovy.xml.XmlParser as GroovyXmlParser

class SafeXmlParser {

  private val delegate = GroovyXmlParser()

  fun parse(file: File): Node? {
    return kotlin.runCatching { parse(file.readText()) }
      .getOrNull()
  }

  fun parse(text: String): Node? {
    return delegate.parseText(text.sanitizeXml())
  }
}

// https://www.w3.org/TR/xml/#charsets
// https://github.com/RBusarow/ModuleCheck/issues/375
private fun String.sanitizeXml(): String {
  val xml10Pattern = "[^\u0009\r\n\u0020-\uD7FF\uE000-\uFFFD\ud800\udc00-\udbff\udfff]"
  return replace(xml10Pattern.toRegex(), " ") // .remove("\ud83d").remove("\ud83e")
}
