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

package com.rickbusarow.modulecheck.kapt

import com.rickbusarow.modulecheck.Config.Kapt
import com.rickbusarow.modulecheck.Config.KaptAndroidTest
import com.rickbusarow.modulecheck.Config.KaptTest
import com.rickbusarow.modulecheck.MCP

object UnusedKaptParser {

  fun parseLazy(mcp: MCP): Lazy<MCP.ParsedKapt<UnusedKaptProcessor>> = lazy {
    parse(mcp)
  }

  fun parse(mcp: MCP): MCP.ParsedKapt<UnusedKaptProcessor> {
    val matchers = kaptMatchers.asMap()

    val unusedAndroidTest = mcp.kaptDependencies.androidTest.filter { coords ->
      matchers[coords.coordinates]?.let { matcher ->
        matcher.annotationImports.none { annotationRegex ->
          mcp.androidTestImports.any { imp ->
            annotationRegex.matches(imp)
          }
        }
      } == true
    }
      .map { UnusedKaptProcessor(mcp.project, it.coordinates, KaptAndroidTest) }
      .toSet()

    val unusedMain = mcp.kaptDependencies.main.filter { coords ->
      matchers[coords.coordinates]?.let { matcher ->
        mcp.mainImports.none { imp ->
          matcher.annotationImports.any { annotationRegex ->
            annotationRegex.matches(imp)
          }
        }
      } == true
    }
      .map { UnusedKaptProcessor(mcp.project, it.coordinates, Kapt) }
      .toSet()

    val unusedTest = mcp.kaptDependencies.test.filter { coords ->
      matchers[coords.coordinates]?.let { matcher ->
        matcher.annotationImports.none { annotationRegex ->
          mcp.testImports.any { imp ->
            annotationRegex.matches(imp)
          }
        }
      } == true
    }
      .map { UnusedKaptProcessor(mcp.project, it.coordinates, KaptTest) }
      .toSet()

    return MCP.ParsedKapt(unusedAndroidTest, unusedMain, unusedTest)
  }
}
