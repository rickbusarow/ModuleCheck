/*
 * Copyright (C) 2020 Rick Busarow
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

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
    return AnsiColor.MAGENTA.background +
        AnsiColor.WHITE.boldHighIntensity +
        message +
        AnsiColor.RESET
  }
}
