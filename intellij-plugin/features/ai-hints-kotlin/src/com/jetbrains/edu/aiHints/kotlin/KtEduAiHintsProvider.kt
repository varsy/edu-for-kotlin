package com.jetbrains.edu.aiHints.kotlin

import com.intellij.codeInspection.LocalInspectionTool
import com.intellij.openapi.project.Project
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiFile
import com.jetbrains.edu.aiHints.core.EduAIHintsProvider
import com.jetbrains.edu.aiHints.core.context.FunctionSignature
import com.jetbrains.edu.aiHints.core.context.FunctionsToStrings
import com.jetbrains.edu.aiHints.core.context.SignatureSource
import com.jetbrains.edu.aiHints.kotlin.util.*

class KtEduAiHintsProvider : EduAIHintsProvider {
  override fun findChangedMethods(before: PsiFile, after: PsiFile, considerParameters: Boolean): List<String> =
    KtFilesDiffer.findChangedMethods(before, after, considerParameters)

  override fun reduceDiffFunctions(function: PsiElement?, modifiedFunction: PsiElement, project: Project): PsiElement =
    KtFunctionDiffReducer.reduceDiffFunctions(function, modifiedFunction, project)

  override fun getFunctionBySignature(psiFile: PsiFile, functionName: String): PsiElement? =
    KtFunctionSignaturesProvider.getFunctionBySignature(psiFile, functionName)

  override fun getFunctionSignatures(psiFile: PsiFile, signatureSource: SignatureSource): List<FunctionSignature> =
    KtFunctionSignaturesProvider.getFunctionSignatures(psiFile, signatureSource)

  override fun getInspections(): List<LocalInspectionTool> = KtInspectionsProvider.getInspections()

  override fun getFunctionsToStringsMap(psiFile: PsiFile): FunctionsToStrings = KtStringExtractor.getFunctionsToStringsMap(psiFile)
}