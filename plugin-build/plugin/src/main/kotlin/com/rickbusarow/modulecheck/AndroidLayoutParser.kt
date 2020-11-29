package com.rickbusarow.modulecheck

import groovy.util.Node
import groovy.util.XmlParser
import java.io.File

object AndroidLayoutParser {
  private val parser = XmlParser()

  fun parse(file: File): Set<String> {
    return parser.parse(file)
      .breadthFirst()
      .filterIsInstance<Node>()
      .mapNotNull { it.name() as? String }
      .toSet()
  }

}

