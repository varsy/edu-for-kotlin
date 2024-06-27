package com.jetbrains.edu.learning.taskToolWindow.htmlTransformers

import com.jetbrains.edu.learning.taskToolWindow.htmlTransformers.steps.*

private val TaskDescriptionHtmlTransformer = HtmlTransformer.pipeline(
  CutOutHeaderTransformer,
  CssHtmlTransformer,
  MediaThemesTransformer,
  CodeHighlighter,
  HintsWrapper,
  TermsHighlighter
)

val TaskDescriptionTransformer = StringHtmlTransformer.pipeline(
  TaskDescriptionHtmlTransformer.toStringTransformer(),
  ResourceWrapper
)