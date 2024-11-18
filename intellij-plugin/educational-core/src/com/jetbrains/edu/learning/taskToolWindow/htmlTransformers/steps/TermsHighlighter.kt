package com.jetbrains.edu.learning.taskToolWindow.htmlTransformers.steps

import com.jetbrains.edu.learning.taskToolWindow.*
import com.jetbrains.edu.learning.taskToolWindow.htmlTransformers.HtmlTransformer
import com.jetbrains.edu.learning.taskToolWindow.htmlTransformers.HtmlTransformerContext
import com.jetbrains.edu.learning.theoryLookup.TheoryLookupTermsManager
import org.jsoup.nodes.Document
import org.jsoup.nodes.Element
import org.jsoup.nodes.TextNode

/**
 * Highlights terms in an HTML document by adding dashed underline style to the occurrences of the terms.
 * Takes terms from the [TheoryLookupTermsManager] object.
 *
 * @see HtmlTransformer
 * @see TheoryLookupTermsManager
 */
object TermsHighlighter : HtmlTransformer {
  // TODO: filter code blocks and other tags
  private val INVALID_TAGS = setOf(CODE_TAG, A_TAG, IMG_TAG)

  private fun Element.isValidTag(): Boolean = tagName() !in INVALID_TAGS

  override fun transform(html: Document, context: HtmlTransformerContext): Document {
    val terms = TheoryLookupTermsManager.getInstance(context.project).getTaskTerms(context.task)?.map { it.value }
    if (terms.isNullOrEmpty()) return html
    for (termTitle in terms) {
      formatTermOccurrences(html, termTitle)
    }
    return html
  }

  private fun formatTermOccurrences(html: Document, termTitle: String) {
    val nodes = html.getElementsContainingOwnText(termTitle)
      .asSequence()
      .filter { it.isValidTag() }
      .flatMap { it.textNodes() }
      .filter { it.text().contains(termTitle) }
      .toList()
    for (node in nodes) {
      val termElement = getDashedUnderlineElement(html, termTitle)
      var currentNode = node
      do {
        currentNode = formatNextOccurrence(currentNode, termTitle, termElement)
      }
      while (currentNode != null)
    }
  }

  private fun formatNextOccurrence(textNode: TextNode, termTitle: String, dashedTermElement: Element): TextNode? {
    val text = textNode.text()
    if (!text.contains(termTitle)) return null
    val startIdx = text.indexOf(termTitle)
    val endIdx = startIdx + termTitle.length
    val tail = if (endIdx < text.length) textNode.splitText(endIdx) else null
    val middle = textNode.splitText(startIdx)
    middle.after(dashedTermElement.clone())
    middle.remove()
    return tail
  }
}
