package com.rickbusarow.modulecheck

inline fun <T : Any, E> T.applyEach(elements: Iterable<E>, block: T.(E) -> Unit): T {
  elements.forEach { element -> this.block(element) }
  return this
}
