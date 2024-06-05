package com.jetbrains.edu.kotlin.jarvis.completion

import com.intellij.codeInsight.completion.*
import com.intellij.codeInsight.lookup.LookupElementBuilder
import com.intellij.codeInsight.template.TemplateManager
import com.intellij.codeInsight.template.impl.TextExpression
import com.intellij.patterns.PlatformPatterns
import com.intellij.util.ProcessingContext
import org.jetbrains.kotlin.lexer.KtTokens.REGULAR_STRING_PART

/**
 * Provides completion suggestions for the Jarvis application.
 * It adds completions for basic completion types defined by the `PlatformPatterns.psiElement(REGULAR_STRING_PART)` pattern.
 *
 * @see CompletionContributor
 */
class JarvisCompletionContributor : CompletionContributor() {
  init {
    extend(
      CompletionType.BASIC,
      PlatformPatterns.psiElement(REGULAR_STRING_PART),
      object : CompletionProvider<CompletionParameters>() {
        override fun addCompletions(parameters: CompletionParameters, context: ProcessingContext, resultSet: CompletionResultSet) {
          //TODO: add all completions
          addCompletion(resultSet, "Create `variable` equal to `value`", listOf("Create ", " equal to "), listOf("variable", "value"))
          addCompletion(resultSet, "Create constant `variable` equal to `value`", listOf("Create constant ", " equal to "), listOf("variable", "value"))
        }
      }
    )
  }

  private fun addCompletion(resultSet: CompletionResultSet, lookupString: String, segments: List<String>, variables: List<String>) {
    resultSet.addElement(LookupElementBuilder.create(lookupString).withInsertHandler { context, _ ->
      val templateManager = TemplateManager.getInstance(context.project)
      val template = templateManager.createTemplate("", "")
      for (i in segments.indices) {
        template.addTextSegment(segments[i])
        if (i < variables.size) {
          template.addVariable(TextExpression(variables[i]), true)
        }
      }

      context.editor.document.deleteString(context.startOffset, context.tailOffset)
      context.editor.caretModel.moveToOffset(context.startOffset)
      templateManager.startTemplate(context.editor, template)
    })
  }
}
