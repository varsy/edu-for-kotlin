package com.jetbrains.edu.jarvis

import com.intellij.lang.annotation.AnnotationHolder
import com.intellij.lang.annotation.Annotator
import com.intellij.lang.annotation.HighlightSeverity
import com.intellij.openapi.util.TextRange
import com.intellij.psi.PsiElement
import com.intellij.refactoring.suggested.startOffset
import com.jetbrains.edu.jarvis.highlighting.undefinedidentifier.*
import com.jetbrains.edu.jarvis.messages.EduJarvisBundle
import com.jetbrains.edu.jarvis.models.NamedFunction
import com.jetbrains.edu.jarvis.models.NamedVariable

/**
 * Highlights parts containing errors inside the `prompt` DSL element.
 */

interface PromptErrorAnnotator<T> : Annotator {

  override fun annotate(element: PsiElement, holder: AnnotationHolder) {
    if (!element.isRelevant()) return
    val promptContent = getPromptContentOrNull(element) ?: return
    applyAnnotation(promptContent, holder)
  }

  /**
   * Highlights incorrect parts found in the `promptContent`.
   */
  fun applyAnnotation(
    promptContent: PsiElement,
    holder: AnnotationHolder
  ) =
    getIncorrectParts(promptContent).forEach {
      val errorRange = TextRange(
        promptContent.startOffset + it.range.first,
        promptContent.startOffset + it.range.last + 1
      )
      holder
        .newAnnotation(
          HighlightSeverity.ERROR,
          EduJarvisBundle.message(it.parametrizedError.errorType.message, *it.parametrizedError.params)
        )
        .range(errorRange)
        .create()
    }

  /**
   * Returns a sequence of [IncorrectPart] which contains parts of
   * `context` to be highlighted and the type of error that the corresponding part contains.
   */
  fun getIncorrectParts(context: PsiElement): Collection<IncorrectPart> {
    val visibleFunctions = getVisibleEntities(context, *getNamedFunctionClasses()) { it.toNamedFunctionOrNull() }
    val visibleVariables = getVisibleEntities(context, *getNamedVariableClasses()) { it.toNamedVariableOrNull() }
    val processor = ErrorProcessor(visibleFunctions, visibleVariables)
    return AnnotatorRule.values().asSequence()
      .flatMap { rule ->
        getAnnotatorRuleMatches(context.text, rule)
      }.distinctBy { match ->
        match.identifier.range.first
      }.sortedBy { match ->
        match.identifier.range.first
      }.map { match ->
        IncorrectPart(
          match.identifier.range, getError(match.rule, processor, match)
        )
      }.filter { it.parametrizedError.errorType != AnnotatorError.NONE }
      .toList()
  }

  /**
   * Returns a sequence of [MatchGroup] representing the parts of `target`
   * string that are relevant based on the given [Regex].
   */
  private fun getAnnotatorRuleMatches(target: String, rule: AnnotatorRule): Sequence<AnnotatorRuleMatch> {
    return rule.regex.findAll(target).map { it.toAnnotatorRuleMatch(rule) }
  }

  /**
   * Converts [MatchResult] to [AnnotatorRuleMatch].
   * The [MatchResult] must contain at least one non-null capturing group.
   */
  private fun MatchResult.toAnnotatorRuleMatch(rule: AnnotatorRule): AnnotatorRuleMatch {
    val identifier = groups[1] ?: error("Invalid regular expression. There should be at least one non-null capturing group.")
    val arguments = if (groups.size > 2) groups[2]?.value else null
    return AnnotatorRuleMatch(rule, identifier, arguments)
  }

  /**
   * Returns visible entities, that is, entities that can be accessed from the `context` scope.
   */
  fun <E> getVisibleEntities(
    context: PsiElement,
    vararg targetClasses: T,
    toEntityOrNull: (PsiElement) -> E?
  ): MutableSet<E>

  /**
   * Returns the type of error found in the `target` string.
   * The errors are processed using the provided [ErrorProcessor].
   */
  fun getError(
    rule: AnnotatorRule,
    processor: ErrorProcessor,
    target: AnnotatorRuleMatch
  ): AnnotatorParametrizedError {
    return when (rule) {
      AnnotatorRule.STORE_VARIABLE, AnnotatorRule.CREATE_VARIABLE,
      AnnotatorRule.SET_VARIABLE, AnnotatorRule.SAVE_VARIABLE, AnnotatorRule.FOR_LOOP -> {
        processor.visibleVariables.add(NamedVariable(target.identifier.value))
        AnnotatorParametrizedError.NO_ERROR
      }

      AnnotatorRule.CALL_FUNCTION -> {
        processor.processNamedFunction(NamedFunction(target))
      }

      AnnotatorRule.ISOLATED_CODE -> {
        processor.processNamedVariable(NamedVariable(target))
      }
    }
  }

  /**
   * Returns whether the [PsiElement] is relevant, that is, whether it may contain an error.
   */
  fun PsiElement.isRelevant(): Boolean
  fun PsiElement.toNamedFunctionOrNull(): NamedFunction?
  fun PsiElement.toNamedVariableOrNull(): NamedVariable?

  /**
   * Returns the classes of PSI elements that represent named variables.
   */
  fun getNamedVariableClasses(): Array<T>

  /**
   * Returns the classes of PSI elements that represent named functions.
   */
  fun getNamedFunctionClasses(): Array<T>

  /**
   * Returns the [PsiElement] representing the prompt block.
   * May return `null` if there is no prompt.
   */
  fun getPromptContentOrNull(element: PsiElement): PsiElement?

}
