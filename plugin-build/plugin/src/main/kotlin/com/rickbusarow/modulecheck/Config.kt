package com.rickbusarow.modulecheck

sealed class Config(val name: String) {

  object AndroidTest : Config("androidTest")
  object Api : Config("api")
  object CompileOnly : Config("compileOnly")
  object Implementation : Config("implementation")
  object RuntimeOnly : Config("runtimeOnly")
  object TestApi : Config("testApi")
  object TestImplementation : Config("testImplementation")

  class Custom(name: String) : Config(name)

  companion object {
    fun from(name: String): Config = when (name) {
      AndroidTest.name -> AndroidTest
      Api.name -> Api
      CompileOnly.name -> CompileOnly
      Implementation.name -> Implementation
      RuntimeOnly.name -> RuntimeOnly
      TestApi.name -> TestApi
      TestImplementation.name -> TestImplementation
      else -> Custom(name)
    }
  }

  override fun equals(other: Any?): Boolean {
    if (this === other) return true
    if (other !is Config) return false

    if (name != other.name) return false

    return true
  }

  override fun hashCode(): Int {
    return name.hashCode()
  }


}
