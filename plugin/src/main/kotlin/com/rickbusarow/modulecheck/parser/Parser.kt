package com.rickbusarow.modulecheck.parser

import com.rickbusarow.modulecheck.MCP

abstract class Parser<T> {

  fun parseLazy(mcp: MCP): Lazy<MCP.Parsed<T>> = lazy {
    parse(mcp)
  }

  abstract fun parse(mcp: MCP): MCP.Parsed<T>
}
