package com.jetbrains.edu.aiHints.core

import com.intellij.codeInspection.LocalInspectionTool
import com.intellij.lang.LanguageExtension
import com.intellij.openapi.project.Project
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiFile
import com.jetbrains.edu.aiHints.core.context.FunctionSignature
import com.jetbrains.edu.aiHints.core.context.SignatureSource
import com.jetbrains.edu.learning.courseFormat.Course
import com.jetbrains.edu.learning.courseFormat.ext.languageById

interface EduAIHintsConfigurator {
  fun findChangedMethods(before: PsiFile, after: PsiFile, considerParameters: Boolean): List<String>

  fun reduceDiffFunctions(function: PsiElement?, modifiedFunction: PsiElement, project: Project): PsiElement?

  fun getFunctionBySignature(psiFile: PsiFile, functionName: String): PsiElement?

  fun getFunctionSignatures(psiFile: PsiFile, signatureSource: SignatureSource): List<FunctionSignature>

  fun getInspections(): List<LocalInspectionTool>

  companion object {
    private val EP_NAME = LanguageExtension<EduAIHintsConfigurator>("aiHints.eduAiHints")

    fun findEduAiHintsConfigurator(course: Course?): EduAIHintsConfigurator? {
      return course?.languageById?.let { EP_NAME.forLanguage(it) }
    }
  }
}