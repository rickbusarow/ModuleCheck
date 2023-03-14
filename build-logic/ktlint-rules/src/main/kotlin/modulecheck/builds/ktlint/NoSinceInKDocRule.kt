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

package modulecheck.builds.ktlint

import com.pinterest.ktlint.core.Rule
import com.pinterest.ktlint.core.ast.ElementType
import com.pinterest.ktlint.core.ast.ElementType.KDOC_TAG
import com.pinterest.ktlint.core.ast.ElementType.KDOC_TAG_NAME
import com.pinterest.ktlint.core.ast.ElementType.KDOC_TEXT
import com.pinterest.ktlint.core.ast.ElementType.WHITE_SPACE
import com.pinterest.ktlint.core.ast.children
import com.pinterest.ktlint.core.ast.nextSibling
import com.pinterest.ktlint.core.ast.prevLeaf
import modulecheck.builds.VERSION_NAME
import org.jetbrains.kotlin.com.intellij.lang.ASTNode
import org.jetbrains.kotlin.com.intellij.psi.impl.source.tree.CompositeElement
import org.jetbrains.kotlin.com.intellij.psi.impl.source.tree.LeafElement
import org.jetbrains.kotlin.com.intellij.psi.impl.source.tree.LeafPsiElement
import org.jetbrains.kotlin.com.intellij.psi.impl.source.tree.PsiWhiteSpaceImpl
import org.jetbrains.kotlin.kdoc.parser.KDocKnownTag.SINCE
import org.jetbrains.kotlin.kdoc.psi.api.KDoc
import org.jetbrains.kotlin.kdoc.psi.impl.KDocTag
import org.jetbrains.kotlin.psi.psiUtil.collectDescendantsOfType
import org.jetbrains.kotlin.psi.psiUtil.startOffset

/**
 * Finds Kdoc comments which don't have a `@since <version>` annotation
 *
 * @since 0.13.0
 */
class NoSinceInKDocRule : Rule(id = "no-since-in-kdoc") {
  private val currentVersion by lazy {
    VERSION_NAME
      .removeSuffix("-LOCAL")
      .removeSuffix("-SNAPSHOT")
  }

  override fun beforeVisitChildNodes(
    node: ASTNode,
    autoCorrect: Boolean,
    emit: (offset: Int, errorMessage: String, canBeAutoCorrected: Boolean) -> Unit
  ) {
    if (node.elementType == ElementType.KDOC_END) {
      visitKDoc(node, autoCorrect = autoCorrect, emit = emit)
    }
  }

  private fun visitKDoc(
    kdocNode: ASTNode,
    autoCorrect: Boolean,
    emit: (offset: Int, errorMessage: String, canBeAutoCorrected: Boolean) -> Unit
  ) {
    val kdoc = kdocNode.psi.parent as KDoc

    val tag = kdoc.findSinceTag()

    if (tag == null) {
      emit(kdocNode.startOffset, "added `@since $currentVersion` to kdoc", true)

      if (autoCorrect) {
        kdocNode.addSinceTag(currentVersion)
      }
      return
    }

    val sinceVersion = kdoc.findSinceTag()?.getContent()

    if (sinceVersion.isNullOrBlank()) {
      emit(
        kdocNode.startOffset,
        "added '$currentVersion' to `@since` tag",
        true
      )

      if (autoCorrect) {
        tag.addVersionToSinceTag(currentVersion)
      }
    }
  }

  private fun KDoc.getAllTags(): List<KDocTag> = collectDescendantsOfType()

  private fun KDoc.findSinceTag(): KDocTag? {
    // Pre-existing 'since' tags which are parsed before visiting will show up as KDocTag. They're
    // nested inside KDocSections, so they don't show up using AST's non-recursive traversals.
    // After we've added our own tag, it won't show up in PSI -- but it's flat inside the KDoc node,
    // so it shows up with simple AST traversal.  Note that the PSI version has a name of 'since'
    // but the AST version node is '@since'.  This is consistent whether the tag is added manually
    // here, or if it's parsed that way from source.
    return collectDescendantsOfType<KDocTag> { it.name == "since" }
      .singleOrNull()
      ?: node.children()
        .filter { it.elementType == KDOC_TAG }
        .filter {
          it.children()
            .filter { it.elementType == KDOC_TAG_NAME }
            .any { it.text == "@since" }
        }
        .singleOrNull()
        ?.let { KDocTag(it) }
  }

  private fun ASTNode.addSinceTag(version: String) {
    val kdoc = psi.parent as KDoc

    val indent = kdoc.findIndent()
    val newlineIndent = "\n$indent"

    val isSingleLine = kdoc.text.lines().size == 1
    val contentWasBlank = kdoc.getAllSections().none { it.text.isNotBlank() }

    val hasTags = kdoc.getAllTags().any { it.text.trim().startsWith("@") }

    val leadingNewlineCount = when {
      contentWasBlank -> 0
      !hasTags && isSingleLine -> 2
      hasTags && isSingleLine -> 1
      !hasTags -> 2
      else -> 1
    }

    val closingTag = this as LeafElement

    val kdocNode = kdoc.node as CompositeElement

    var firstNewNode: ASTNode? = null

    repeat(leadingNewlineCount) {
      val newline = PsiWhiteSpaceImpl(newlineIndent)
      if (firstNewNode == null) {
        firstNewNode = newline
      }

      kdocNode.addChild(newline, closingTag)
      kdocNode.addChild(LeafPsiElement(ElementType.KDOC_LEADING_ASTERISK, "*"), closingTag)
    }

    if (!contentWasBlank) {
      // space after `*` and before `@since`
      kdocNode.addChild(PsiWhiteSpaceImpl(" "), closingTag)
    }

    CompositeElement(KDOC_TAG).also { tagComposite ->
      kdocNode.addChild(tagComposite, closingTag)

      if (firstNewNode == null) {
        firstNewNode = tagComposite
      }

      tagComposite.addChild(LeafPsiElement(KDOC_TAG_NAME, "@since"))

      tagComposite.addChild(PsiWhiteSpaceImpl(" "))
      tagComposite.addChild(LeafPsiElement(KDOC_TEXT, version))
    }

    // The AST will automatically add a whitespace between the previous node and the first one we
    // add, even if we're adding a whitespace.  So after we've added our new nodes, go back and
    // remove the new whitespace **before** the ones we've added.
    val previousLeaf = firstNewNode?.prevLeaf(true) as? LeafElement
    when (previousLeaf?.elementType) {
      WHITE_SPACE -> kdocNode.removeChild(previousLeaf)
      KDOC_TEXT -> previousLeaf.treeParent.replaceChild(
        previousLeaf,
        LeafPsiElement(KDOC_TEXT, previousLeaf.text.trimEnd())
      )
    }

    val openingTag = kdocNode.findChildByType(ElementType.KDOC_START) as LeafElement
    val secondChild = openingTag.nextSibling { true } as ASTNode

    if (contentWasBlank) {
      if (isSingleLine) {
        // turn `/**@since 0.0.1 */` into `/** @since 0.0.1 */`
        kdocNode.addChild(PsiWhiteSpaceImpl(" "), secondChild)
      } else {
        // turn `/**\n @since 0.0.1 */` into `/** @since 0.0.1 */`
        kdocNode.removeChild(secondChild)
        kdocNode.addChild(PsiWhiteSpaceImpl(" "), firstNewNode)
      }

      kdocNode.addChild(PsiWhiteSpaceImpl(" "), closingTag)
    } else {
      if (isSingleLine) {
        // example: `/** comment */`
        // If the kdoc was a single line before, but it wasn't blank, then it's now going to be
        // multi-line.  We have to add a newline, indent, and asterisk after the opening tag.

        kdocNode.addChild(PsiWhiteSpaceImpl(newlineIndent), secondChild)
        kdocNode.addChild(LeafPsiElement(ElementType.KDOC_LEADING_ASTERISK, "*"), secondChild)
      }

      kdocNode.addChild(PsiWhiteSpaceImpl(newlineIndent), closingTag)
    }
  }

  private fun KDocTag.addVersionToSinceTag(version: String) {
    require(knownTag == SINCE) {
      "Expected to be adding a version to a `@since` tag, but instead it's `$text`."
    }

    val tag = collectDescendantsOfType<LeafPsiElement>().last()
    val old = tag.text

    tag.rawReplaceWithText("$old $version")
  }

  private fun KDoc.findIndent(): String {
    val fileLines = containingFile.text.lines()

    var acc = startOffset + 1

    val numSpaces = fileLines.asSequence()
      .mapNotNull {
        if (it.length + 1 < acc) {
          acc -= (it.length + 1)
          null
        } else {
          acc
        }
      }
      .first()
    return " ".repeat(numSpaces)
  }
}
