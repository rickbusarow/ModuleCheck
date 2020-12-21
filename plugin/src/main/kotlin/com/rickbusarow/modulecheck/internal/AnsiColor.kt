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

import java.util.*

@Suppress("MagicNumber")
enum class AnsiColor(private val colorNumber: Byte) {
  BLACK(0),
  RED(1),
  GREEN(2),
  YELLOW(3),
  BLUE(4),
  MAGENTA(5),
  CYAN(6),
  WHITE(7);

  val regular get() = if (isCompatible) "$prefix[0;3${colorNumber}m" else ""
  val bold get() = if (isCompatible) "$prefix[1;3${colorNumber}m" else ""
  val underline get() = if (isCompatible) "$prefix[4;3${colorNumber}m" else ""
  val background get() = if (isCompatible) "$prefix[4${colorNumber}m" else ""
  val highIntensity get() = if (isCompatible) "$prefix[0;9${colorNumber}m" else ""
  val boldHighIntensity get() = if (isCompatible) "$prefix[1;9${colorNumber}m" else ""
  val backgroundHighIntensity get() = if (isCompatible) "$prefix[0;10${colorNumber}m" else ""

  companion object {
    private const val prefix = "\u001B"
    const val RESET = "$prefix[0m"
    private val isCompatible = "win" !in System.getProperty("os.name").toLowerCase(Locale.ROOT)
  }
}

inline class MenuEntryIndex(val value: Int)
