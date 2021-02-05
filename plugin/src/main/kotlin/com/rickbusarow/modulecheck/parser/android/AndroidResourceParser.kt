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

package com.rickbusarow.modulecheck.parser.android

import com.rickbusarow.modulecheck.internal.mainResRootOrNull
import groovy.util.Node
import groovy.util.XmlParser
import org.gradle.api.Project
import org.jetbrains.kotlin.utils.addToStdlib.cast
import java.io.File

object AndroidResourceParser {
  private val parser = XmlParser()

  fun parse(project: Project) {
    fun log(msg: () -> String) {
      if (project.path == ":base:resources") println(msg())
    }

    val resRoot = project.mainResRootOrNull() ?: return

    val anims = resRoot
      .walkTopDown()
      .filter { it.isFile }

      .onEach { file ->
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
      }

      .map { file -> AndroidResource.fromFile(file) }
      .toList()

    // val anims = File(resRoot.path + "/anim")
    //   .orNull()
    //   ?.listFiles()
    //   ?.map { file -> Anim(file.nameWithoutExtension) }
    //   .orEmpty()

    // log {
    //   """------------------------------------------------ base resources anims ---
    //   |
    //   |${anims.lines()}
    // """.trimMargin()
    // }
  }

  fun parse(file: File) = parser.parse(file)
    .breadthFirst()
    .filterIsInstance<Node>()
    .mapNotNull { it.attributes() }
    .flatMap { it.entries }
    .filterNotNull()
    // .flatMap { it.values.mapNotNull { value -> value } }
    .filterIsInstance<MutableMap.MutableEntry<String, String>>()
    .map { it.key to it.value }
    .toMap()
}

fun File.orNull(): File? = if (exists()) this else null

/*

resources[
  attributes={};
  value=[dimen[attributes={name=gauge_view_height};
  value=[125dp]], dimen[attributes={name=mt_font_size_large};
  value=[13sp]], dimen[attributes={name=mt_font_size_medium};
  value=[10sp]], dimen[attributes={name=mt_font_size_small};
  value=[8sp]], dimen[attributes={name=mt_font_size_standard};
  value=[11sp]], dimen[attributes={name=mt_font_size_xl};
  value=[15sp]], dimen[attributes={name=mt_margin_large};
  value=[25dp]], dimen[attributes={name=mt_margin_medium};
  value=[8dp]], dimen[attributes={name=mt_margin_small};
  value=[5dp]], dimen[attributes={name=mt_margin_standard};
  value=[10dp]], dimen[attributes={name=mt_margin_tiny};
  value=[2dp]], dimen[attributes={name=mt_margin_xl};
  value=[50dp]], dimen[attributes={name=mt_padding_large};
  value=[25dp]], dimen[attributes={name=mt_padding_medium};
  value=[8dp]], dimen[attributes={name=mt_padding_small};
  value=[5dp]], dimen[attributes={name=mt_padding_standard};
  value=[10dp]], dimen[attributes={name=mt_padding_tiny};
  value=[2dp]], dimen[attributes={name=mt_padding_xl};
  value=[50dp]], dimen[attributes={name=showcase_radius};
  value=[55dp]], dimen[attributes={name=showcase_radius_inner};
  value=[65dp]], dimen[attributes={name=showcase_radius_outer};
  value=[75dp]], dimen[attributes={name=text_size_body}; value=[12sp]]]]











 */
