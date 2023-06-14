package com.jetbrains.edu.learning.taskDescription.ui.htmlTransformers.steps

import com.jetbrains.edu.learning.taskDescription.ui.htmlTransformers.HtmlTransformerContext
import com.jetbrains.edu.learning.taskDescription.ui.htmlTransformers.StringHtmlTransformer
import com.jetbrains.edu.learning.taskDescription.ui.htmlWithResources

object ResourceWrapper : StringHtmlTransformer {
  override fun transform(html: String, context: HtmlTransformerContext): String = htmlWithResources(context.project, html, context.task)
}