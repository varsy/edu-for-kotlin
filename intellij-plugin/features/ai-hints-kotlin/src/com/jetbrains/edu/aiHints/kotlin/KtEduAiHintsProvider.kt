package com.jetbrains.edu.aiHints.kotlin

import com.intellij.codeInspection.LocalInspectionTool
import com.intellij.openapi.project.Project
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiFile
import com.jetbrains.edu.aiHints.core.EduAIHintsProvider
import com.jetbrains.edu.aiHints.core.context.FunctionSignature
import com.jetbrains.edu.aiHints.core.context.SignatureSource

class KtEduAiHintsProvider : EduAIHintsProvider {
  override fun findChangedMethods(before: PsiFile, after: PsiFile, considerParameters: Boolean): List<String> {
    TODO("Not yet implemented")
  }

  override fun reduceDiffFunctions(function: PsiElement?, modifiedFunction: PsiElement, project: Project): PsiElement? {
    TODO("Not yet implemented")
  }

  override fun getFunctionBySignature(psiFile: PsiFile, functionName: String): PsiElement? {
    TODO("Not yet implemented")
  }

  override fun getFunctionSignatures(psiFile: PsiFile, signatureSource: SignatureSource): List<FunctionSignature> {
    TODO("Not yet implemented")
  }

  override fun getInspections(): List<LocalInspectionTool> {
    TODO("Not yet implemented")
  }
}