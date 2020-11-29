package com.rickbusarow.modulecheck

import com.github.javaparser.StaticJavaParser
import groovy.util.Node
import groovy.util.XmlParser
import java.io.File

class AndroidLayoutParser {
  private val parser = XmlParser()

  fun parse(file: File): Set<String> {
    return parser.parse(file)
      .breadthFirst()
      .filterIsInstance<Node>()
      .mapNotNull { it.name() as? String }
      .toSet()
  }

}

class JavaParser {

  fun parse(file:File) {

    val parsed = StaticJavaParser.parse(file)

    val imports = parsed.imports.toSet()

  }
}
