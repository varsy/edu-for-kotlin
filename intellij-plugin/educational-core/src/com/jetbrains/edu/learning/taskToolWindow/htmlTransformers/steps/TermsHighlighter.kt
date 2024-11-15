package com.jetbrains.edu.learning.taskToolWindow.htmlTransformers.steps

import com.jetbrains.edu.learning.taskToolWindow.*
import com.jetbrains.edu.learning.taskToolWindow.htmlTransformers.HtmlTransformer
import com.jetbrains.edu.learning.taskToolWindow.htmlTransformers.HtmlTransformerContext
import com.jetbrains.edu.learning.theoryLookup.TheoryLookupTermsManager
import org.jsoup.nodes.Document
import org.jsoup.nodes.Element

/**
 * Highlights terms in an HTML document by adding dashed underline style to the occurrences of the terms.
 * Takes terms from the [TheoryLookupTermsManager] object.
 *
 * @see HtmlTransformer
 * @see TheoryLookupTermsManager
 */
object TermsHighlighter : HtmlTransformer {
  // TODO: filter code blocks and other tags
  private val INVALID_TAGS = listOf(CODE_TAG, A_TAG, IMG_TAG)

  private fun Element.isValidTag(): Boolean = tagName() !in INVALID_TAGS

  override fun transform(html: Document, context: HtmlTransformerContext): Document {
    TheoryLookupTermsManager.getInstance(context.project).getTaskTerms(context.task)?.map { it.value }?.forEach { termTitle ->
      html.getElementsContainingOwnText(termTitle)
        .flatMap { element ->
          if (!element.isValidTag()) return@flatMap emptyList()
          element.textNodes().filter { textNode -> textNode.text().contains(termTitle) }
        }
        .forEach { node ->
          val termElement = getDashedUnderlineElement(html, termTitle)
          var currentNode = node
          var text = currentNode.text()
          while (text.contains(termTitle)) {
            val startIdx = text.indexOf(termTitle)
            val endIdx = startIdx + termTitle.length
            val tail = if (endIdx < text.length) currentNode.splitText(endIdx) else null
            val middle = currentNode.splitText(startIdx)
            middle.after(termElement.clone())
            middle.remove()
            currentNode = tail ?: break
            text = currentNode.text()
          }
        }
    }

    return html
  }
}
