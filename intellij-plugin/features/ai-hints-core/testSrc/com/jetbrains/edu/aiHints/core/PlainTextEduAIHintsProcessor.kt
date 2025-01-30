package com.jetbrains.edu.aiHints.core

import com.intellij.psi.PsiElement
import com.intellij.psi.PsiFile
import com.jetbrains.edu.aiHints.core.api.*
import com.jetbrains.edu.aiHints.core.context.FunctionSignature
import com.jetbrains.edu.aiHints.core.context.FunctionsToStrings
import com.jetbrains.edu.aiHints.core.context.SignatureSource

class PlainTextEduAIHintsProcessor : EduAIHintsProcessor {
  override fun getFilesDiffer(): FilesDiffer = object : FilesDiffer {
    override fun findChangedMethods(before: PsiFile, after: PsiFile, considerParameters: Boolean): List<String> = listOf()
  }

  override fun getFunctionDiffReducer(): FunctionDiffReducer = object : FunctionDiffReducer {
    override fun reduceDiffFunctions(function: PsiElement?, modifiedFunction: PsiElement): PsiElement? = null
  }

  override fun getInspectionsProvider(): InspectionsProvider = object : InspectionsProvider {
    override val inspectionIds: Set<String> = emptySet()
  }

  override fun getFunctionSignatureManager(): FunctionSignaturesManager = object : FunctionSignaturesManager {
    override fun getFunctionSignatures(psiFile: PsiFile, signatureSource: SignatureSource): List<FunctionSignature> = emptyList()
    override fun getFunctionBySignature(psiFile: PsiFile, functionName: String): PsiElement? = null
  }

  override fun getStringsExtractor(): StringExtractor = object : StringExtractor {
    override fun getFunctionsToStringsMap(psiFile: PsiFile): FunctionsToStrings = FunctionsToStrings(emptyMap())
  }
}

