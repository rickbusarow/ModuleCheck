/*
 * Copyright (C) 2021 Rick Busarow
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

package modulecheck.api

import modulecheck.api.Output.Color.*
import java.util.*

@Suppress("DefaultLocale")
object Output {

  private const val prefix = "\u001B"

  fun printBlueBackground(message: String) {
    print(WHITE.boldHighIntensity)
    print(BLUE.background)
    print(message)
    reset()
  }

  fun printBlue(message: String) {
    print(BLUE.bold)
    print(message)
    reset()
  }

  fun printGreenBackground(message: String) {
    print(WHITE.boldHighIntensity)
    print(GREEN.background)
    print(message)
    reset()
  }

  fun printMagenta(message: String) {
    print(MAGENTA.boldHighIntensity)
    print(message)
    reset()
  }

  fun printCyan(message: String) {
    print(CYAN.boldHighIntensity)
    print(message)
    reset()
  }

  fun printYellow(message: String) {
    print(YELLOW.bold)
    print(message)
    reset()
  }

  fun printGreen(message: String) {
    print(GREEN.bold)
    print(message)
    reset()
  }

  fun printRed(message: String) {
    print(RED.boldHighIntensity)
    print(message)
    reset()
  }

  fun printRedBackground(message: String) {
    print(WHITE.boldHighIntensity)
    print(RED.backgroundHighIntensity)
    print(message)
    reset()
  }

  private fun reset() {
    println("$prefix[0m")
  }

  @Suppress("MagicNumber")
  enum class Color(private val colorNumber: Byte) {
    BLACK(0),
    RED(1),
    GREEN(2),
    YELLOW(3),
    BLUE(4),
    MAGENTA(5),
    CYAN(6),
    WHITE(7);

    val background get() = if (!windows) "$prefix[4${colorNumber}m" else ""
    val backgroundHighIntensity get() = if (!windows) "$prefix[0;10${colorNumber}m" else ""
    val bold get() = if (!windows) "$prefix[1;3${colorNumber}m" else ""
    val boldHighIntensity get() = if (!windows) "$prefix[1;9${colorNumber}m" else ""
    val highIntensity get() = if (!windows) "$prefix[0;9${colorNumber}m" else ""
    val regular get() = if (!windows) "$prefix[0;3${colorNumber}m" else ""
    val underline get() = if (!windows) "$prefix[4;3${colorNumber}m" else ""

    companion object {
      private val windows = "win" in System.getProperty("os.name").toLowerCase(Locale.ROOT)
    }
  }
}
