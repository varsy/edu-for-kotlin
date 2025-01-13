package com.jetbrains.edu.aiHints.core

import com.intellij.codeInspection.LocalInspectionTool
import com.intellij.lang.LanguageExtension
import com.intellij.openapi.project.Project
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiFile
import com.jetbrains.edu.aiHints.core.context.FunctionSignature
import com.jetbrains.edu.aiHints.core.context.FunctionsToStrings
import com.jetbrains.edu.aiHints.core.context.SignatureSource
import com.jetbrains.edu.learning.courseFormat.Course
import com.jetbrains.edu.learning.courseFormat.ext.languageById

/**
 * The main interface provides AI Hints support for some language.
 *
 * To get provider instance for the specific course use [EduAIHintsProvider.forCourse].
 *
 * @see com.jetbrains.edu.aiHints.kotlin.KtEduAiHintsProvider
 */
interface EduAIHintsProvider {
  /**
   * Identifies function names that have different implementations between two [PsiFile]s.
   *
   * Example use case: identifying the list of function names that are changed in the [com.jetbrains.educational.ml.hints.hint.CodeHint].
   *
   * @param considerParameters Whether to consider changes in the parameter list.
   *
   * @see com.jetbrains.edu.aiHints.kotlin.KtEduAiHintsProvider.findChangedMethods
   */
  fun findChangedMethods(before: PsiFile, after: PsiFile, considerParameters: Boolean = false): List<String>

  /**
   * Reduces the changes in two functions.
   * This function helps in providing next step hint rather than showing the complete solution at once.
   *
   * @see [TaskProcessorImpl.reduceChangesInCodeHint]
   */
  fun reduceDiffFunctions(function: PsiElement?, modifiedFunction: PsiElement, project: Project): PsiElement?

  /**
   * Returns a list of inspections that will be applied to the [com.jetbrains.educational.ml.hints.hint.CodeHint].
   */
  fun getInspections(): List<LocalInspectionTool>

  /**
   * Retrieves a specific function's [PsiElement] by its signature.
   *
   * Used for [com.jetbrains.educational.ml.hints.hint.CodeHint] modifications.
   */
  fun getFunctionBySignature(psiFile: PsiFile, functionName: String): PsiElement?

  /**
   * A utility function that extracts function signatures from a [PsiFile] based on the specified [SignatureSource].
   *
   * Used for [com.jetbrains.educational.ml.hints.hint.CodeHint] modifications as well as building context information for LLM requests.
   */
  fun getFunctionSignatures(psiFile: PsiFile, signatureSource: SignatureSource): List<FunctionSignature>

  /**
   * A utility function that creates a mapping of functions to their string representations for the given [PsiFile].
   * Used to build context information for LLM requests.
   *
   * @see [com.jetbrains.educational.ml.hints.context.StepByStepNextCodeStepContext]
   * @see [com.jetbrains.educational.ml.hints.context.TaskCompletionGuidanceContext]
   * @see [com.jetbrains.edu.aiHints.core.context.AuthorSolutionContext]
   */
  fun getFunctionsToStringsMap(psiFile: PsiFile): FunctionsToStrings

  companion object {
    private val EP_NAME = LanguageExtension<EduAIHintsProvider>("aiHints.provider")

    fun forCourse(course: Course): EduAIHintsProvider? {
      return course.languageById?.let { EP_NAME.forLanguage(it) }
    }
  }
}