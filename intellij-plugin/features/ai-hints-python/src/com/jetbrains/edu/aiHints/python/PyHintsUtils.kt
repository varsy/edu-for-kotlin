package com.jetbrains.edu.aiHints.python

import com.intellij.psi.PsiFile
import com.intellij.psi.util.PsiTreeUtil
import com.intellij.util.concurrency.annotations.RequiresReadLock
import com.jetbrains.edu.aiHints.core.context.FunctionParameter
import com.jetbrains.edu.aiHints.core.context.FunctionSignature
import com.jetbrains.edu.aiHints.core.context.SignatureSource
import com.jetbrains.python.psi.PyFunction
import com.jetbrains.python.psi.PyParameter

object PyHintsUtils {
  @RequiresReadLock
  fun PsiFile.functions(): Collection<PyFunction> = PsiTreeUtil.findChildrenOfType(this, PyFunction::class.java)

  infix fun PyFunction.hasSameBodyAs(other: PyFunction): Boolean =
    other.statementList.text == statementList.text

  infix fun PyFunction.hasSameParametersAs(other: PyFunction): Boolean =
    other.parameterList.text == parameterList.text

  fun PyFunction.generateSignature(signatureSource: SignatureSource): FunctionSignature? {
    val functionName = name ?: return null
    val parameters = parameterList.parameters.mapNotNull { it.toFunctionParameter() }
    val bodyLineCount = statementList.text.lines().size
    return FunctionSignature(functionName, parameters, annotationValue, signatureSource, bodyLineCount)
  }

  private fun PyParameter.toFunctionParameter(): FunctionParameter? {
    val splitParameter = text.split(":").mapNotNull { it.split("=").firstOrNull()?.trim() }
    return when (splitParameter.size) {
      2 -> FunctionParameter(splitParameter[0].trim(), splitParameter[1].trim())
      1 -> FunctionParameter(splitParameter[0].trim(), null)
      else -> null
    }
  }
}