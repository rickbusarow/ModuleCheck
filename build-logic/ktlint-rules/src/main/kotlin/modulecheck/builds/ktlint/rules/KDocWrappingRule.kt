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

package modulecheck.builds.ktlint.rules

import com.pinterest.ktlint.core.Rule
import com.pinterest.ktlint.core.api.EditorConfigProperties
import com.pinterest.ktlint.core.api.UsesEditorConfigProperties
import com.pinterest.ktlint.core.api.editorconfig.EditorConfigProperty
import com.pinterest.ktlint.core.api.editorconfig.MAX_LINE_LENGTH_PROPERTY
import com.pinterest.ktlint.core.ast.ElementType
import com.pinterest.ktlint.core.ast.ElementType.KDOC
import com.pinterest.ktlint.core.ast.ElementType.KDOC_LEADING_ASTERISK
import com.pinterest.ktlint.core.ast.ElementType.KDOC_TEXT
import com.pinterest.ktlint.core.ast.children
import modulecheck.builds.mapLines
import modulecheck.builds.remove
import org.intellij.markdown.MarkdownElementTypes.CODE_BLOCK
import org.intellij.markdown.MarkdownElementTypes.PARAGRAPH
import org.intellij.markdown.MarkdownTokenTypes.Companion.EOL
import org.intellij.markdown.MarkdownTokenTypes.Companion.WHITE_SPACE
import org.intellij.markdown.ast.getTextInNode
import org.intellij.markdown.flavours.commonmark.CommonMarkFlavourDescriptor
import org.intellij.markdown.parser.MarkdownParser
import org.jetbrains.kotlin.com.intellij.lang.ASTNode
import org.jetbrains.kotlin.com.intellij.psi.impl.source.tree.CompositeElement
import org.jetbrains.kotlin.com.intellij.psi.impl.source.tree.LeafPsiElement
import org.jetbrains.kotlin.com.intellij.psi.impl.source.tree.PsiWhiteSpaceImpl
import org.jetbrains.kotlin.kdoc.psi.api.KDoc
import org.jetbrains.kotlin.kdoc.psi.impl.KDocTag
import org.jetbrains.kotlin.psi.psiUtil.startOffset
import kotlin.LazyThreadSafetyMode.NONE

/** Fixes wrapping inside KDoc comments.*/
class KDocWrappingRule : Rule(id = "kdoc-wrapping"), UsesEditorConfigProperties {

  val maxLineLengthProperty = MAX_LINE_LENGTH_PROPERTY.copy(defaultValue = 103)
  private var maxLineLength: Int = maxLineLengthProperty.defaultValue

  private val markdownParser by lazy(NONE) {
    MarkdownParser(CommonMarkFlavourDescriptor())
  }

  override val editorConfigProperties: List<EditorConfigProperty<*>>
    get() = listOf(maxLineLengthProperty)

  override fun beforeFirstNode(editorConfigProperties: EditorConfigProperties) {

    maxLineLength = editorConfigProperties.getEditorConfigValue(maxLineLengthProperty)
    super.beforeFirstNode(editorConfigProperties)
  }

  override fun beforeVisitChildNodes(
    node: ASTNode,
    autoCorrect: Boolean,
    emit: (offset: Int, errorMessage: String, canBeAutoCorrected: Boolean) -> Unit
  ) {

    if (node.elementType == ElementType.KDOC_START) {
      visitKDoc(node, autoCorrect = autoCorrect, emit = emit)
    }
  }

  private fun visitKDoc(
    kdocNode: ASTNode,
    autoCorrect: Boolean,
    emit: (offset: Int, errorMessage: String, canBeAutoCorrected: Boolean) -> Unit
  ) {

    val kdoc = kdocNode.psi.parent as KDoc

    val starIndent = kdoc.findIndent()

    // KDocSection is a subtype of KDocTag.  The default section is a tag.
    val tags = kdoc.getAllTags()

    tags.forEachIndexed { tagIndex, tag ->

      val lastTag = tagIndex == tags.lastIndex

      val textNodesEtc = tag.node.children()
        .dropWhile { !it.isKDocText() }
        .takeWhile { !it.isKDocTag() }
        .toList()

      val sectionText = textNodesEtc
        .joinToString("") { it.text }
        .mapLines { it.remove("""^ *\*? ?""".toRegex()) }
        .trimEnd()

      val wrapped = tag.sectionContent(starIndent.length + 2)

      if (sectionText != wrapped) {

        emit(tag.startOffset, "kdoc line wrapping", true)

        if (autoCorrect) {

          val tagNode = tag.node as CompositeElement

          val anchor = textNodesEtc.firstOrNull()

          val newlineIndent = "\n$starIndent"

          val wrappedLines = wrapped.lines()

          wrappedLines
            .forEachIndexed { i, line ->
              if (i != 0) {
                tagNode.addChild(PsiWhiteSpaceImpl(newlineIndent), anchor)
                tagNode.addChild(LeafPsiElement(KDOC_LEADING_ASTERISK, "*"), anchor)

                if (line.isNotBlank()) {
                  tagNode.addChild(PsiWhiteSpaceImpl(" "), anchor)
                }
              }

              if (i == 0 && tag.parent == kdoc) {
                tagNode.addChild(LeafPsiElement(KDOC_TEXT, " $line"), anchor)
              } else {
                tagNode.addChild(LeafPsiElement(KDOC_TEXT, line), anchor)
              }
            }

          if (!lastTag) {
            if (tag.parent == kdoc) {
              tagNode.addChild(PsiWhiteSpaceImpl(newlineIndent), anchor)
              tagNode.addChild(LeafPsiElement(KDOC_LEADING_ASTERISK, "*"), anchor)
              tagNode.addChild(PsiWhiteSpaceImpl(newlineIndent), anchor)
              tagNode.addChild(LeafPsiElement(KDOC_LEADING_ASTERISK, "*"), anchor)
              tagNode.addChild(PsiWhiteSpaceImpl(" "), anchor)
            }
          }

          textNodesEtc.toList().forEach { tagNode.removeChild(it) }
        }
      }
    }
  }

  private fun KDocTag.sectionContent(indentLength: Int): String {

    val sectionText = node.children()
      // strip away the LEADING asterisk (from the first line), as well as any `@___` tags and any
      // links like `myParameter` or `IllegalStateException`
      .dropWhile { !it.isKDocText() }
      .takeWhile { !it.isKDocTag() }
      .joinToString("") { it.text }
      .mapLines { it.remove("""^ *\*? ?""".toRegex()) }

    val skip = setOf(WHITE_SPACE, EOL)

    fun CharSequence.cleanWhitespaces() = replace("(\\S)\\s+".toRegex(), "$1 ").trim()

    val maxLength = maxLineLength - indentLength

    fun wrappingRegex(extraLeadingSpaces: Int = 0): Regex {

      val max = maxLength - extraLeadingSpaces

      @Suppress("RegExpSimplifiable")
      return """([^\n]{1,$max})(?:\s|$)""".toRegex()
    }

    return markdownParser.buildMarkdownTreeFromString(sectionText)
      .children
      .filterNot { it.type in skip }
      .mapIndexed { paragraphNumber: Int, markdownNode ->

        val prefix = if (paragraphNumber == 0 && parent.node.elementType != KDOC) {
          node.children()
            .dropWhile { it.isKDocLeadingAsterisk() }
            .takeWhile { !it.isKDocText() }
            .joinToString("") { " ".repeat(it.text.length) }
        } else {
          ""
        }

        val extraLeading = if (parent.node.elementType == KDOC) 0 else 2

        when (markdownNode.type) {
          PARAGRAPH -> {

            wrappingRegex(extraLeading)
              .findAll(prefix + markdownNode.getTextInNode(sectionText).cleanWhitespaces())
              .mapIndexed { i, mr ->
                val perLine = if (i == 0 && paragraphNumber == 0) {
                  ""
                } else {
                  " ".repeat(extraLeading)
                }

                "$perLine${mr.groupValues.first().trim()}"
              }
              .joinToString("\n")
          }

          // If a CODE_BLOCK is top-level within a section/tag, that means it's not wrapped in three
          // backticks.  Assume this means it's a multi-line description of a tag, and it's just
          // indented.  If it's multi-line
          CODE_BLOCK -> {
            if (parent.node.elementType == KDOC) {
              markdownNode.getTextInNode(sectionText).toString().replaceIndent("    ")
            } else {
              wrappingRegex(2)
                .findAll(prefix + markdownNode.getTextInNode(sectionText).cleanWhitespaces())
                .joinToString("\n") { "  ${it.groupValues.first().trim()}" }
            }
          }

          // code fences, headers, tables, etc. don't get wrapped
          else -> {
            markdownNode.getTextInNode(sectionText).toString().trimIndent()
          }
        }
      }
      .joinToString("\n\n")
  }
}
