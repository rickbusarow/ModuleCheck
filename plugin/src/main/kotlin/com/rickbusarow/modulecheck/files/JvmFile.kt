package com.rickbusarow.modulecheck.files


abstract class JvmFile {
  abstract val packageFqName: String
  abstract val importDirectives: Set<String>
  abstract val declarations: Set<String>

  override fun toString(): String {
    return """${this::class.simpleName}(
      |packageFqName='$packageFqName',
      |
      |importDirectives=$importDirectives,
      |
      |declarations=$declarations
      |
      |)""".trimMargin()
  }
}
