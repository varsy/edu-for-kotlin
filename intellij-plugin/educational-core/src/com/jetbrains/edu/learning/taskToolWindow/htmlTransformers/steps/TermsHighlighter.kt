package com.jetbrains.edu.learning.taskToolWindow.htmlTransformers.steps

import com.jetbrains.edu.learning.taskToolWindow.*
import com.jetbrains.edu.learning.taskToolWindow.htmlTransformers.HtmlTransformer
import com.jetbrains.edu.learning.taskToolWindow.htmlTransformers.HtmlTransformerContext
import com.jetbrains.edu.learning.theoryLookup.TermsManager
import org.jsoup.nodes.Document

/**
 * Highlights terms in an HTML document by adding dashed underline style to the occurrences of the terms.
 * Takes terms from the [TermsManager] object.
 *
 * @see HtmlTransformer
 * @see TermsManager
 */
object TermsHighlighter : HtmlTransformer {
  override fun transform(html: Document, context: HtmlTransformerContext): Document {
    val task = context.task
    val project = context.project
    val termsManager = TermsManager.getInstance(project)

    termsManager.getTerms(task).forEach {
      val term = it.original
      html.getElementsContainingOwnText(term).forEach { element ->
        // TODO: filter code blocks and other tags
        if (element.tagName() != CODE_TAG && element.tagName() != A_TAG && element.tagName() != IMG_TAG) {
          val termElement = getDashedUnderlineElement(html, term)
          element.textNodes().filter { textNode -> textNode.text().contains(term) }.forEach { node ->
            var currentNode = node
            var text = currentNode.text()
            while (text.contains(term)) {
              val startIdx = text.indexOf(term)
              val endIdx = startIdx + term.length
              val tail = if (endIdx < text.length) currentNode.splitText(endIdx) else null
              val middle = currentNode.splitText(startIdx)
              middle.after(termElement.clone())
              middle.remove()
              currentNode = tail ?: break
              text = currentNode.text()
            }
          }
        }
      }
    }

    return html
  }
}
