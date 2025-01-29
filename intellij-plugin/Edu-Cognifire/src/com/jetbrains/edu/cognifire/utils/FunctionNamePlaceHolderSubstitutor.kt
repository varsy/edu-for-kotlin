package com.jetbrains.edu.cognifire.utils

import com.jetbrains.edu.cognifire.models.FunctionSignature
import com.jetbrains.educational.ml.cognifire.responses.PromptToCodeContent

object FunctionNamePlaceHolderSubstitutor {
  fun substitutePlaceHolders(promptToCodeContent: PromptToCodeContent, functionName: String): PromptToCodeContent =
    promptToCodeContent.map { it.copy(generatedCodeLine = replaceFunctionCalls(it.generatedCodeLine, functionName)) }

  private fun replaceFunctionCalls(code: String, functionName: String): String {
    val regex = Regex("\\b${FunctionSignature.PLACEHOLDER}")
    return code.replace(regex, functionName)
  }
}