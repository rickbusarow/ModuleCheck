package com.rickbusarow.modulecheck.files

import com.github.javaparser.StaticJavaParser
import com.github.javaparser.ast.Node
import com.github.javaparser.ast.body.TypeDeclaration
import java.io.File

class JavaFile(val file: File) : JvmFile() {

  val parsed by lazy { StaticJavaParser.parse(file) }

  override val packageFqName by lazy { parsed.packageDeclaration.get().nameAsString }
  override val importDirectives by lazy {
    parsed.imports.map {
      it.nameAsString
//          .split(".")
//          .dropLast(1)
//          .joinToString(".")
    }.toSet()
  }
  override val declarations by lazy {

//      val fields = mutableListOf<String>()
//      val methods = mutableListOf<String>()
    val types = mutableSetOf<String>()

    val iterator = NodeIterator { node ->
      when (node) {
//          is FieldDeclaration -> if (node.isStatic) fields.add(node.toString())
//          is MethodDeclaration -> if (node.isStatic) methods.add(node.toString())
        is TypeDeclaration<*> -> types.add(node.fullyQualifiedName.get())
      }
      true
    }

    iterator.explore(parsed)

    types
  }
}

class NodeIterator(
  private val predicate: (node: Node) -> Boolean
) {

  fun explore(node: Node) {
    if (predicate(node)) {
      node.childNodes.forEach { child ->
        explore(child)
      }
    }
  }
}
