package com.rickbusarow.modulecheck

import groovy.util.Node
import groovy.util.XmlParser
import java.io.File

object AndroidLayoutParser {
  private val parser = XmlParser()

  fun parseViews(file: File): Set<String> {

    return parser.parse(file)
      .breadthFirst()
      .filterIsInstance<Node>()
      .mapNotNull { it.name() as? String }
      .toSet()
  }

  fun parseResources(file: File): Set<String> {

    return parser.parse(file)
      .breadthFirst()
      .filterIsInstance<Node>()
      .mapNotNull { it.attributes() }
      .flatMap { it.values.mapNotNull { value -> value } }
      .filterIsInstance<String>()
      .toSet()
  }

}

