package com.rickbusarow.modulecheck.internal

import java.util.*

@Suppress("DefaultLocale")
class Cli {

  fun askForString(
    question: String,
    default: String? = null,
    errorMessageGen: (String) -> String?
  ): String {
    while (true) {
      println()
      printBlue(question)

      val choice = scanner.nextLine()

      val result = if (choice.isEmpty() && default != null) default else choice

      val errorMessage = errorMessageGen(choice) ?: return result.also { printGreen("choice: $it") }

      printRed(errorMessage)
    }
  }

  fun askBinaryQuestion(
    question: String,
    trueChoice: String,
    falseChoice: String
  ): Boolean {
    while (true) {
      println()
      printBlue(question)

      printMagenta("$trueChoice/$falseChoice")

      when (val choice = scanner.nextLine().toLowerCase()) {
        trueChoice.toLowerCase() -> return true.also { printGreen("choice: $choice") }
        falseChoice.toLowerCase() -> return false.also { printGreen("choice: $choice") }
        else                      -> {

          printRed("Unexpected input ($choice), please, try again")
        }
      }
    }
  }

  fun showMenuAndGetIndexOfChoice(
    header: String,
    footer: String,
    numberedEntries: List<String>
  ): MenuEntryIndex {
    println()
    printBlue(header)
    println()
    numberedEntries.forEachIndexed { index, entryText ->
      val number = index + 1
      printBlue("$number.  $entryText")
    }
    println()
    printBlue(footer)

    return MenuEntryIndex(scanner.nextLine().toInt() - 1).also { printGreen("choice: $it") }
  }

  private val scanner = Scanner(System.`in`)

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
