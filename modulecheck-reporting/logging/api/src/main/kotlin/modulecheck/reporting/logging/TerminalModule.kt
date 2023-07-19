/*
 * Copyright (C) 2021-2023 Rick Busarow
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

package modulecheck.reporting.logging

import com.github.ajalt.mordant.rendering.AnsiLevel.TRUECOLOR
import com.github.ajalt.mordant.rendering.TextColors
import com.github.ajalt.mordant.rendering.TextStyle
import com.github.ajalt.mordant.rendering.Theme
import com.github.ajalt.mordant.terminal.Terminal
import com.squareup.anvil.annotations.ContributesTo
import dagger.Module
import dagger.Provides
import modulecheck.dagger.TaskScope

@Module
@ContributesTo(TaskScope::class)
object TerminalModule {
  @Provides
  fun provideTerminal(): Terminal = Terminal(
    ansiLevel = TRUECOLOR,
    theme = Theme(from = Theme.Default) {
      styles.putAll(
        mapOf(
          "success" to TextColors.green,
          "danger" to TextColors.red,
          "warning" to TextColors.yellow,
          "info" to TextColors.brightWhite,
          "muted" to TextStyle(dim = true)
        )
      )
    },
    interactive = true
  ).also {
    it.info.updateTerminalSize()
  }
}
