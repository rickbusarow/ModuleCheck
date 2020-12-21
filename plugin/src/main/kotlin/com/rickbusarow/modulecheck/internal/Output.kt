package com.rickbusarow.modulecheck.internal

@Suppress("DefaultLocale")
object Output {


  fun printBlueBackground(message: String) {
    print(AnsiColor.WHITE.boldHighIntensity)
    print(AnsiColor.BLUE.background)
    print(message)
    println(AnsiColor.RESET)
  }

  fun printBlue(message: String) {
    print(AnsiColor.BLUE.bold)
    print(message)
    println(AnsiColor.RESET)
  }

  fun printGreenBackground(message: String) {
    print(AnsiColor.WHITE.boldHighIntensity)
    print(AnsiColor.GREEN.background)
    print(message)
    println(AnsiColor.RESET)
  }

  fun printMagenta(message: String) {
    print(AnsiColor.MAGENTA.bold)
    print(message)
    println(AnsiColor.RESET)
  }

  fun printYellow(message: String) {
    print(AnsiColor.YELLOW.bold)
    print(message)
    println(AnsiColor.RESET)
  }

  fun printGreen(message: String) {
    print(AnsiColor.GREEN.bold)
    print(message)
    println(AnsiColor.RESET)
  }

  fun printRed(message: String) {
    print(AnsiColor.RED.bold)
    print(message)
    println(AnsiColor.RESET)
  }

  fun red(message: String) = AnsiColor.RED.bold + message + AnsiColor.RESET

  fun magentaBackground(message: String): String {
    return AnsiColor.MAGENTA.background + AnsiColor.WHITE.boldHighIntensity + message + AnsiColor.RESET
  }
}
