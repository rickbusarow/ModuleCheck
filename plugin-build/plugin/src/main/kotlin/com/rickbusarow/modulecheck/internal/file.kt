package com.rickbusarow.modulecheck.internal

import java.io.File

fun Sequence<File>.ktFiles() = filter { it.isFile }
  .mapNotNull { it.asKtFile() }
