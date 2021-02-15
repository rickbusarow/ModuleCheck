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

package modulecheck.core.parser.android

import groovy.xml.XmlParser
import java.io.File

object AndroidResourceParser {
  private val parser = XmlParser()

  fun parse(resDir: File): Set<String> {
    fun log(msg: () -> String) {
      /*if (project.path == ":base:resources") */println(msg())
    }

    val resources = resDir
      .walkTopDown()
      .filter { it.isFile }

      /*.onEach { file ->
        if (file.path.endsWith("values-da/dimens.xml")) {
          val parsed = parser.parse(file)

          val t = parsed.children().cast<List<Node>>()

          println(
            """ ______________________________________________ node file --> $file
            |
            |${
            t.joinToString("\n") {
              it.name()
                .toString() + "       " + it.value() + "       " + it.attributes()
                .map { it.cast<MutableMap.MutableEntry<String, String>>().value }
            }
            }
            |
            |
            |____________________________________________________
          """.trimMargin()
          )
        }
      }*/

      .map { file -> AndroidResource.fromFile(file) }
      .toSet()

/*    val grouped = resources.groupBy { it::class }

    grouped
      .forEach { type, lst ->

        log {
          """ ------------------------------------------- ${type.simpleName}
        |
        |${lst.lines()}
        |
      """.trimMargin()
        }
      }*/

    return resources
      .map { "R.${it.prefix}.${it.name}" }
      .toSet()
      .also {
        //   println(
        //     """ ----------------------------------------------- resource things ----------------------------
        //   |
        //   |${it.lines()}
        // """.trimMargin()
        //   )
      }
  }
}

fun File.existsOrNull(): File? = if (exists()) this else null
