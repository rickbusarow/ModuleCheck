package com.rickbusarow.modulecheck

import io.kotest.core.TestConfiguration
import io.kotest.core.config.AbstractProjectConfig
import io.kotest.core.spec.IsolationMode
import java.io.File
import java.nio.file.Files

object ProjectConfig : AbstractProjectConfig() {
  //  override val parallelism = 3
  override val isolationMode = IsolationMode.InstancePerLeaf
}

fun TestConfiguration.tempDir(): File {
  val file = Files.createTempDirectory("").toFile()
  afterSpec {
    file.delete()
  }
  return file
}
