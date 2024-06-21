package com.jetbrains.edu.jarvis

import com.intellij.lang.annotation.AnnotationHolder
import com.intellij.lang.annotation.Annotator
import com.intellij.lang.annotation.HighlightSeverity
import com.intellij.openapi.util.TextRange
import com.intellij.psi.PsiElement
import com.intellij.psi.util.PsiTreeUtil
import com.intellij.refactoring.suggested.startOffset
import com.jetbrains.edu.jarvis.enums.AnnotatorError
import com.jetbrains.edu.jarvis.enums.AnnotatorRule
import com.jetbrains.edu.jarvis.errors.AnnotatorParametrizedError
import com.jetbrains.edu.jarvis.messages.EduJarvisBundle
import com.jetbrains.edu.jarvis.models.NamedFunction
import com.jetbrains.edu.jarvis.models.NamedVariable
import kotlin.reflect.KClass

/**
 * Highlights parts containing errors inside the `description` DSL element.
 */

interface DescriptionErrorAnnotator : Annotator {

  override fun annotate(element: PsiElement, holder: AnnotationHolder) {
    if (!element.isRelevant()) return
    val descriptionContent = getDescriptionContentOrNull(element) ?: return
    applyAnnotation(descriptionContent, holder)
  }

  /**
   * Highlights incorrect parts found in the `descriptionContent`.
   */
  fun applyAnnotation(
    descriptionContent: PsiElement,
    holder: AnnotationHolder
  ) =
    getIncorrectParts(descriptionContent).forEach {
      val errorRange = TextRange(
        descriptionContent.startOffset + it.range.first,
        descriptionContent.startOffset + it.range.last + 1
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
   * Returns a sequence of [DescriptionAnnotatorResult] which contains parts of
   * `context` to be highlighted and the type of error that the corresponding part contains.
   */
  fun getIncorrectParts(context: PsiElement): Collection<DescriptionAnnotatorResult> {
    val visibleFunctions = getVisibleEntities(context, *getNamedFunctionClasses()) { it.toNamedFunctionOrNull() }
    val visibleVariables = getVisibleEntities(context, *getNamedVariableClasses()) { it.toNamedVariableOrNull() }
    val processor = getProcessor(visibleFunctions, visibleVariables)
    return AnnotatorRule.values().asSequence()
      .flatMap { rule ->
        getRelevantPartsByRegex(context.text, rule.regex).map {
          it to rule
        }
      }.sortedBy { (match, _) ->
        match.range.first
      }.distinctBy { (match, _) ->
        match.range.first
      }.map { (match, rule) ->
        DescriptionAnnotatorResult(
          match.range, getError(rule, processor, match.value)
        )
      }.filter { it.parametrizedError.errorType != AnnotatorError.NONE }
      .toList()
  }

  /**
   * Returns a sequence of [MatchGroup] representing the parts of `target`
   * string that are relevant based on the given [Regex].
   */
  private fun getRelevantPartsByRegex(target: String, regex: Regex): Sequence<MatchGroup> {
    return regex.findAll(target).mapNotNull { it.groups[1] }
  }

  fun String.isNamedFunction() = namedFunctionRegex.matches(this)

  fun String.isNamedVariable() = namedVariableRegex.matches(this)

  fun <T> getVisibleEntities(
    context: PsiElement,
    vararg targetClasses: KClass<out PsiElement>,
    toEntityOrNull: (PsiElement) -> T?
  ): MutableSet<T> =
    targetClasses.map { PsiTreeUtil.collectElementsOfType(context.containingFile, it.java) }.flatten().mapNotNull { toEntityOrNull(it) }
      .toMutableSet()

  /**
   * Returns the type of error found in the `target` string.
   * The errors are processed using the provided [ErrorProcessor].
   */
  fun getError(
    rule: AnnotatorRule,
    processor: ErrorProcessor,
    target: String
  ): AnnotatorParametrizedError {
    return when (rule) {
      AnnotatorRule.VARIABLE_DECLARATION -> {
        processor.visibleVariables.add(NamedVariable(target))
        AnnotatorParametrizedError.NO_ERROR
      }

      AnnotatorRule.NO_PARENTHESES_FUNCTION -> {
        processor.processNamedFunction(target)
      }

      AnnotatorRule.ISOLATED_CODE -> {
        when {
          target.isNamedFunction() -> processor.processNamedFunction(target)
          target.isNamedVariable() -> processor.processNamedVariable(target)
          else -> AnnotatorParametrizedError.NO_ERROR
        }
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
  fun getNamedVariableClasses(): Array<KClass<out PsiElement>>

  /**
   * Returns the classes of PSI elements that represent named functions.
   */
  fun getNamedFunctionClasses(): Array<KClass<out PsiElement>>
  fun getProcessor(visibleFunctions: MutableSet<NamedFunction>, visibleVariables: MutableSet<NamedVariable>): ErrorProcessor
  fun getDescriptionContentOrNull(element: PsiElement): PsiElement?


  companion object {
    fun callSynonyms() = listOf("call", "calls", "invoke", "invokes", "execute", "executes", "run", "runs")
    fun functionSynonyms() = listOf("function", "fun")
    fun declareSynonyms() =
      listOf(
        "create", "creates", "declare", "declares", "set up", "sets up", "store", "stores",
        "stored", "set", "sets", "assign", "assigns", "give", "gives", "initialize", "initializes"
      )

    fun variableSynonyms() = listOf("var", "variable")

    val namedFunctionRegex = "[a-zA-Z_][a-zA-Z0-9_]*\\((?:\\s*[^(),\\s]+\\s*(?:,\\s*[^(),\\s]+\\s*)*)?\\s*\\)".toRegex()
    val namedVariableRegex = "[a-zA-Z_][a-zA-Z0-9_]*".toRegex()
  }

}
